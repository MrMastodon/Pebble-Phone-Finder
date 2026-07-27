#include <pebble.h>
#include <string.h>

#define COMMAND_STOP 0
#define COMMAND_START 1

typedef enum {
  STATE_IDLE,           // not playing, nothing in flight
  STATE_SENDING_START,  // sent START, waiting for phone to ack
  STATE_PLAYING,        // phone acked START
  STATE_SENDING_STOP,   // sent STOP, waiting for phone to ack
  STATE_FAILED,         // last send failed (e.g. watch not connected to phone)
} AppState;

static Window *s_window;
static TextLayer *s_status_layer;
static TextLayer *s_bt_status_layer;
static Layer *s_select_arrow_layer;

static AppState s_state = STATE_IDLE;
static bool s_is_norwegian = false;

// Two-language string table, picked once at startup from the watch's system
// locale (see prv_init). Pebble doesn't have a string-resource localization
// system for this SDK generation, so this is a plain runtime switch.
static const char *prv_text_for_state(AppState state) {
  if (s_is_norwegian) {
    switch (state) {
      case STATE_IDLE:          return "Trykk for\nå finne mobilen";
      case STATE_SENDING_START: return "Starter...";
      case STATE_PLAYING:       return "Spiller\nTrykk for å stoppe";
      case STATE_SENDING_STOP:  return "Stopper...";
      case STATE_FAILED:        return "Ikke tilkoblet\nPrøv igjen";
    }
  } else {
    switch (state) {
      case STATE_IDLE:          return "Press to\nfind phone";
      case STATE_SENDING_START: return "Starting...";
      case STATE_PLAYING:       return "Playing\nPress to stop";
      case STATE_SENDING_STOP:  return "Stopping...";
      case STATE_FAILED:        return "Not connected\nPress to retry";
    }
  }
  return "";
}

static void prv_update_status_text(void) {
  text_layer_set_text(s_status_layer, prv_text_for_state(s_state));
}

static void prv_update_bt_status(bool connected) {
  const char *text;
  if (s_is_norwegian) {
    text = connected ? "Telefon: tilkoblet" : "Telefon: frakoblet";
  } else {
    text = connected ? "Phone: connected" : "Phone: disconnected";
  }
  text_layer_set_text(s_bt_status_layer, text);
  text_layer_set_text_color(s_bt_status_layer, connected ? GColorJaegerGreen : GColorRed);
}

static void prv_bt_connection_handler(bool connected) {
  prv_update_bt_status(connected);
}

static void prv_send_command(uint8_t command) {
  DictionaryIterator *iter;
  AppMessageResult result = app_message_outbox_begin(&iter);
  if (result != APP_MSG_OK) {
    s_state = STATE_FAILED;
    prv_update_status_text();
    return;
  }

  dict_write_uint8(iter, MESSAGE_KEY_COMMAND, command);
  app_message_outbox_send();
}

static void prv_select_click_handler(ClickRecognizerRef recognizer, void *context) {
  bool starting = (s_state != STATE_PLAYING && s_state != STATE_SENDING_START);
  s_state = starting ? STATE_SENDING_START : STATE_SENDING_STOP;
  prv_update_status_text();
  prv_send_command(starting ? COMMAND_START : COMMAND_STOP);
}

static void prv_click_config_provider(void *context) {
  window_single_click_subscribe(BUTTON_ID_SELECT, prv_select_click_handler);
}

// Draws a right-pointing triangle at the vertical height of the physical
// SELECT button, near the screen's right edge - a visual cue for which
// button to press, without needing a separate icon resource.
static void prv_select_arrow_update_proc(Layer *layer, GContext *ctx) {
  GRect bounds = layer_get_bounds(layer);
  int16_t mid_y = bounds.size.h / 2;
  int16_t right_x = bounds.size.w;

  GPoint points[3] = {
    { (int16_t)(right_x - 22), (int16_t)(mid_y - 12) },
    { (int16_t)(right_x - 22), (int16_t)(mid_y + 12) },
    { (int16_t)(right_x - 4), mid_y },
  };
  GPathInfo path_info = {
    .num_points = 3,
    .points = points,
  };
  GPath *path = gpath_create(&path_info);

  graphics_context_set_fill_color(ctx, GColorOrange);
  gpath_draw_filled(ctx, path);

  gpath_destroy(path);
}

static void prv_window_load(Window *window) {
  Layer *window_layer = window_get_root_layer(window);
  GRect bounds = layer_get_bounds(window_layer);

  s_bt_status_layer = text_layer_create(GRect(0, 4, bounds.size.w, 20));
  text_layer_set_text_alignment(s_bt_status_layer, GTextAlignmentCenter);
  text_layer_set_font(s_bt_status_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD));
  layer_add_child(window_layer, text_layer_get_layer(s_bt_status_layer));

  s_status_layer = text_layer_create(GRect(4, 28, bounds.size.w - 8, bounds.size.h - 52));
  text_layer_set_text_alignment(s_status_layer, GTextAlignmentCenter);
  text_layer_set_font(s_status_layer, fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
  layer_add_child(window_layer, text_layer_get_layer(s_status_layer));

  s_select_arrow_layer = layer_create(bounds);
  layer_set_update_proc(s_select_arrow_layer, prv_select_arrow_update_proc);
  layer_add_child(window_layer, s_select_arrow_layer);

  prv_update_status_text();
  prv_update_bt_status(connection_service_peek_pebble_app_connection());
}

static void prv_window_unload(Window *window) {
  text_layer_destroy(s_bt_status_layer);
  text_layer_destroy(s_status_layer);
  layer_destroy(s_select_arrow_layer);
}

static void prv_outbox_sent_handler(DictionaryIterator *iterator, void *context) {
  // The phone's Bluetooth stack confirmed receipt of the message. This does
  // NOT mean the companion app actually acted on it - only that it reached
  // the phone side of the link.
  if (s_state == STATE_SENDING_START) {
    s_state = STATE_PLAYING;
  } else if (s_state == STATE_SENDING_STOP) {
    s_state = STATE_IDLE;
  }
  prv_update_status_text();
}

static void prv_outbox_failed_handler(DictionaryIterator *iterator, AppMessageResult reason,
                                       void *context) {
  s_state = STATE_FAILED;
  prv_update_status_text();
}

static void prv_init(void) {
  // e.g. "nb", "nb_NO" for Norwegian Bokmal - matched by prefix since the
  // exact suffix/region can vary.
  const char *locale = i18n_get_system_locale();
  s_is_norwegian = (strncmp(locale, "nb", 2) == 0);

  app_message_register_outbox_sent(prv_outbox_sent_handler);
  app_message_register_outbox_failed(prv_outbox_failed_handler);
  app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum());

  connection_service_subscribe((ConnectionHandlers) {
    .pebble_app_connection_handler = prv_bt_connection_handler,
    .pebblekit_connection_handler = NULL,
  });

  s_window = window_create();
  window_set_click_config_provider(s_window, prv_click_config_provider);
  window_set_window_handlers(s_window, (WindowHandlers) {
    .load = prv_window_load,
    .unload = prv_window_unload,
  });
  window_stack_push(s_window, true);
}

static void prv_deinit(void) {
  connection_service_unsubscribe();
  window_destroy(s_window);
}

int main(void) {
  prv_init();
  app_event_loop();
  prv_deinit();
}
