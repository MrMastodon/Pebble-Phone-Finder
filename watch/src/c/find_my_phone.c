#include <pebble.h>

#define COMMAND_STOP 0
#define COMMAND_START 1

static Window *s_window;
static TextLayer *s_status_layer;

static bool s_is_playing = false;

static void prv_update_status_text(void) {
  if (s_is_playing) {
    text_layer_set_text(s_status_layer, "Playing...\nPress to stop");
  } else {
    text_layer_set_text(s_status_layer, "Press to\nfind phone");
  }
}

static void prv_send_command(uint8_t command) {
  DictionaryIterator *iter;
  AppMessageResult result = app_message_outbox_begin(&iter);
  if (result != APP_MSG_OK) {
    return;
  }

  dict_write_uint8(iter, MESSAGE_KEY_COMMAND, command);
  app_message_outbox_send();
}

static void prv_select_click_handler(ClickRecognizerRef recognizer, void *context) {
  s_is_playing = !s_is_playing;
  prv_update_status_text();
  prv_send_command(s_is_playing ? COMMAND_START : COMMAND_STOP);
}

static void prv_click_config_provider(void *context) {
  window_single_click_subscribe(BUTTON_ID_SELECT, prv_select_click_handler);
}

static void prv_window_load(Window *window) {
  Layer *window_layer = window_get_root_layer(window);
  GRect bounds = layer_get_bounds(window_layer);

  s_status_layer = text_layer_create(GRect(0, bounds.size.h / 2 - 24, bounds.size.w, 48));
  text_layer_set_text_alignment(s_status_layer, GTextAlignmentCenter);
  text_layer_set_font(s_status_layer, fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD));
  layer_add_child(window_layer, text_layer_get_layer(s_status_layer));

  prv_update_status_text();
}

static void prv_window_unload(Window *window) {
  text_layer_destroy(s_status_layer);
}

static void prv_outbox_failed_handler(DictionaryIterator *iterator, AppMessageResult reason,
                                       void *context) {
  text_layer_set_text(s_status_layer, "Not connected\nPress to retry");
}

static void prv_init(void) {
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
