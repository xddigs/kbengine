package com.isofarm.input;

/**
 * Logical controls whose physical bindings are defined in {@code config.json}.
 */
public enum ControlAction {
    MOVE_FORWARD("move_forward"),
    MOVE_BACKWARD("move_backward"),
    MOVE_LEFT("move_left"),
    MOVE_RIGHT("move_right"),
    JUMP("jump"),
    MOVE_X("move_x"),
    MOVE_Y("move_y"),
    SWIM_UP("swim_up"),
    SWIM_DOWN("swim_down"),
    SNEAK("sneak"),
    ZOOM("zoom"),
    CAMERA_ROTATE_LEFT("camera_rotate_left"),
    CAMERA_ROTATE_RIGHT("camera_rotate_right"),
    PRIMARY_ACTION("primary_action"),
    SECONDARY_ACTION("secondary_action"),
    THIRD_ACTION("third_action"),
    PATHFIND("pathfind"),
    OPEN_CHAT("open_chat"),
    TOGGLE_HUD("toggle_hud"),
    TOGGLE_DEBUG("toggle_debug"),
    DROP_ITEM("drop_item"),
    TOGGLE_SHIELD("toggle_shield"),
    MODIFIER("modifier"),
    SMART_SHIFT("smart_shift"),
    TOGGLE_INVENTORY("toggle_inventory"),
    TOGGLE_BOOK("toggle_book"),
    TOGGLE_MUSIC("toggle_music"),
    PREVIOUS_PAGE("previous_page"),
    NEXT_PAGE("next_page"),
    QUIT("quit"),
    CHANGE_LANGUAGE("change_language"),
    SHOW_LANGUAGE("show_language"),
    TOGGLE_FULLSCREEN("toggle_fullscreen"),
    UI_SELECT("ui_select"),
    UI_CONTEXT("ui_context");

    private final String id;

    ControlAction(String id) {
        this.id = id;
    }

    /**
     * Creates or returns id from the supplied arguments.
     * @return {@link String} identifier used by {@code config.json}
     */
    public String id() {
        return id;
    }
}
