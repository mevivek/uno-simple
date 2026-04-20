package com.vivek.unosimple.ui

/**
 * Stable identifiers used by Compose UI tests to locate nodes without coupling
 * to user-visible copy. Everything that a test asserts on should have a tag
 * here so renaming "Start game" to "Play" (or translating the UI) doesn't
 * break the test suite.
 */
object TestTags {
    // HomeScreen
    const val HOME_SCREEN = "home_screen"
    const val HOME_TITLE = "home_title"
    const val HOME_START_BUTTON = "home_start_button"
    const val HOME_OPPONENT_CHIP_PREFIX = "home_opponent_chip_"

    // GameScreen
    const val GAME_SCREEN = "game_screen"
    const val GAME_MENU_BUTTON = "game_menu_button"
    const val GAME_TURN_HEADER = "game_turn_header"
    const val GAME_DRAW_PILE = "game_draw_pile"
    const val GAME_DISCARD_PILE = "game_discard_pile"
    const val GAME_HAND_CARD_PREFIX = "game_hand_card_"
    const val GAME_OPPONENT_TILE_PREFIX = "game_opponent_tile_"

    // Wild color picker
    const val WILD_PICKER_DIALOG = "wild_picker_dialog"
    const val WILD_PICKER_COLOR_PREFIX = "wild_picker_color_"

    // UNO declaration button
    const val UNO_BUTTON = "uno_button"

    // Settings screen
    const val SETTINGS_SCREEN = "settings_screen"
    const val SETTINGS_SOUND_TOGGLE = "settings_sound_toggle"
    const val SETTINGS_HAPTICS_TOGGLE = "settings_haptics_toggle"
    const val SETTINGS_SPEED_SLIDER = "settings_speed_slider"
    const val SETTINGS_BACK_BUTTON = "settings_back_button"
    const val HOME_SETTINGS_BUTTON = "home_settings_button"
    const val HOME_HOTSEAT_BUTTON = "home_hotseat_button"

    // Lobby screen (local multiplayer setup)
    const val LOBBY_SCREEN = "lobby_screen"
    const val LOBBY_COUNT_CHIP_PREFIX = "lobby_count_chip_"
    const val LOBBY_NAME_FIELD_PREFIX = "lobby_name_field_"
    const val LOBBY_START_BUTTON = "lobby_start_button"

    // Online lobby (Firebase create/join)
    const val ONLINE_LOBBY_SCREEN = "online_lobby_screen"
    const val ONLINE_LOBBY_NAME_FIELD = "online_lobby_name_field"
    const val ONLINE_LOBBY_CREATE_BUTTON = "online_lobby_create_button"
    const val ONLINE_LOBBY_CODE_FIELD = "online_lobby_code_field"
    const val ONLINE_LOBBY_JOIN_BUTTON = "online_lobby_join_button"
    const val HOME_ONLINE_BUTTON = "home_online_button"

    // Hotseat multiplayer game screen
    const val HOTSEAT_SCREEN = "hotseat_screen"
    const val HOTSEAT_PASS_OVERLAY = "hotseat_pass_overlay"
    const val HOTSEAT_READY_BUTTON = "hotseat_ready_button"
}
