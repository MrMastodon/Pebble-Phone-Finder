#include <pebble.h>

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
static Layer *s_select_arrow_layer;

static AppState s_state = STATE_IDLE;

static void prv_update_status_text(void) {
  switch (s_state) {
    case STATE_IDLE:
      text_layer_set_text(s_status_layer, "Press to\nfind phone");
      break;
    case STATE_SENDING_START:
      text_layer_set_text(s_status_layer, "Starting...");
      break;
    case STATE_PLAYING:
      text_layer_set_text(s_status_layer, "Playing\nPress to stop");
      break;
    case STATE_SENDING_STOP:
      text_layer_set_text(s_status_layer, "Stopping...");
      break;
    case STATE_FAILED:
      text_layer_set_text(s_status_layer, "Not connected\nPress to retry");
      break;
  }
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

  s_status_layer = text_layer_create(GRect(4, 24, bounds.size.w - 8, bounds.size.h - 48));
  text_layer_set_text_alignment(s_status_layer, GTextAlignmentCenter);
  text_layer_set_font(s_status_layer, fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
  layer_add_child(window_layer, text_layer_get_layer(s_status_layer));

  s_select_arrow_layer = layer_create(bounds);
  layer_set_update_proc(s_select_arrow_layer, prv_select_arrow_update_proc);
  layer_add_child(window_layer, s_select_arrow_layer);

  prv_update_status_text();
}

static void prv_window_unload(Window *window) {
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
  app_message_register_outbox_sent(prv_outbox_sent_handler);
  app_message_register_outbox_failed(prv_outbox_failed_handler);
  app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum());

  s_window = window_create();
  window_set_click_config_provider(s_window, prv_click_config_provider);
  window_set_window_handlers(s_window, (WindowHandlers) {
    .load = prv_window_load,
    .unload = prv_window_unload,
  });
  window_stack_push(s_window, true);
}

static void prv_deinit(void) {
  window_destroy(s_window);
}

int main(void) {
  prv_init();
  app_event_loop();
  prv_deinit();
}
