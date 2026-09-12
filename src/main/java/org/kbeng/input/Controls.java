package org.kbeng.input;

import java.util.EnumSet;

/**
 * Controls provides controls capabilities within the input subsystem.
 *
 * It defines device mappings, control-state tracking, and interaction orchestration for player actions.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public final class Controls {
    private static final EnumSet<ControlAction> toggledActions =
            EnumSet.noneOf(ControlAction.class);
    private static final EnumSet<ControlAction> handledTogglePresses =
            EnumSet.noneOf(ControlAction.class);

    private Controls() {}

    public static boolean isPressed(ControlAction action) {
        return Keyboard.isKeyPressed(action)
                || Mouse.isButtonPressed(action)
                || Joystick.isButtonPressed(action);
    }

    public static boolean isDown(ControlAction action) {
        return Keyboard.isKeyDown(action)
                || Mouse.isButtonDown(action)
                || Joystick.isButtonDown(action);
    }

    public static boolean isReleased(ControlAction action) {
        return Keyboard.isKeyReleased(action)
                || Mouse.isButtonReleased(action)
                || Joystick.isButtonReleased(action);
    }

    /**
     * Returns the persistent toggle state for an action. A new press flips the
     * state, while holding or repeatedly querying the action leaves it unchanged.
     */
    public static boolean isToggled(ControlAction action) {
        if (!isDown(action)) {
            handledTogglePresses.remove(action);
        } else if (isPressed(action) && handledTogglePresses.add(action)) {
            if (!toggledActions.add(action)) {
                toggledActions.remove(action);
            }
        }

        return toggledActions.contains(action);
    }

    public static float getAxis(ControlAction action) {
        return Joystick.getAxis(action);
    }
}
