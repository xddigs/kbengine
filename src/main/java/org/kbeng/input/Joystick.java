package org.kbeng.input;

import org.lwjgl.glfw.GLFWGamepadState;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Represents the joystick component of the Isofarm runtime.
 *
 * <p>This type centralizes the state, lifecycle and behavior required by its callers,
 * keeping domain rules together with the data they operate on.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li><b>State:</b> Owns the data needed to represent the component consistently.</li>
 *   <li><b>Lifecycle:</b> Exposes the operations used to create, update and release its state.</li>
 *   <li><b>Integration:</b> Coordinates with the surrounding game, rendering or UI systems through its public API.</li>
 * </ul>
 *
 * <p>Callers should use the documented public operations and allow this type to preserve
 * its invariants rather than modifying implementation details directly.
 */
public final class Joystick {
    public static final int BUTTON_A = GLFW_GAMEPAD_BUTTON_A;
    public static final int BUTTON_B = GLFW_GAMEPAD_BUTTON_B;
    public static final int BUTTON_X = GLFW_GAMEPAD_BUTTON_X;
    public static final int BUTTON_Y = GLFW_GAMEPAD_BUTTON_Y;
    public static final int BUTTON_LEFT_BUMPER = GLFW_GAMEPAD_BUTTON_LEFT_BUMPER;
    public static final int BUTTON_RIGHT_BUMPER = GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER;
    public static final int BUTTON_BACK = GLFW_GAMEPAD_BUTTON_BACK;
    public static final int BUTTON_START = GLFW_GAMEPAD_BUTTON_START;
    public static final int BUTTON_GUIDE = GLFW_GAMEPAD_BUTTON_GUIDE;
    public static final int BUTTON_LEFT_THUMB = GLFW_GAMEPAD_BUTTON_LEFT_THUMB;
    public static final int BUTTON_RIGHT_THUMB = GLFW_GAMEPAD_BUTTON_RIGHT_THUMB;
    public static final int BUTTON_DPAD_UP = GLFW_GAMEPAD_BUTTON_DPAD_UP;
    public static final int BUTTON_DPAD_RIGHT = GLFW_GAMEPAD_BUTTON_DPAD_RIGHT;
    public static final int BUTTON_DPAD_DOWN = GLFW_GAMEPAD_BUTTON_DPAD_DOWN;
    public static final int BUTTON_DPAD_LEFT = GLFW_GAMEPAD_BUTTON_DPAD_LEFT;

    public static final int AXIS_LEFT_X = GLFW_GAMEPAD_AXIS_LEFT_X;
    public static final int AXIS_LEFT_Y = GLFW_GAMEPAD_AXIS_LEFT_Y;
    public static final int AXIS_RIGHT_X = GLFW_GAMEPAD_AXIS_RIGHT_X;
    public static final int AXIS_RIGHT_Y = GLFW_GAMEPAD_AXIS_RIGHT_Y;
    public static final int AXIS_LEFT_TRIGGER = GLFW_GAMEPAD_AXIS_LEFT_TRIGGER;
    public static final int AXIS_RIGHT_TRIGGER = GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER;

    private static final int JOYSTICK_COUNT = GLFW_JOYSTICK_LAST + 1;
    private static final int BUTTON_COUNT = GLFW_GAMEPAD_BUTTON_LAST + 1;
    private static final int AXIS_COUNT = GLFW_GAMEPAD_AXIS_LAST + 1;

    private static final boolean[][] buttons = new boolean[JOYSTICK_COUNT][BUTTON_COUNT];
    private static final boolean[][] lastButtons = new boolean[JOYSTICK_COUNT][BUTTON_COUNT];
    private static final float[][] axes = new float[JOYSTICK_COUNT][AXIS_COUNT];
    private static final boolean[] connected = new boolean[JOYSTICK_COUNT];
    private static int activeJoystick = -1;

    /**
     * Creates a new private {@code Joystick} instance.
     */
    private Joystick() {}

    /**
     * Initializes the component.
     */
    public static void init() {
        poll();
        for (int joystick = 0; joystick < JOYSTICK_COUNT; joystick++) {
            System.arraycopy(buttons[joystick], 0, lastButtons[joystick], 0, BUTTON_COUNT);
        }
    }

    /**
     * Updates the current state.
     */
    public static void update() {
        for (int joystick = 0; joystick < JOYSTICK_COUNT; joystick++) {
            System.arraycopy(buttons[joystick], 0, lastButtons[joystick], 0, BUTTON_COUNT);
        }
        poll();
    }

    /**
     * Processes each applicable element for poll.
     */
    private static void poll() {
        activeJoystick = -1;

        int preferredJoystick = ControlConfigParser.controls.getPreferredJoystick();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWGamepadState state = GLFWGamepadState.malloc(stack);

            for (int joystick = GLFW_JOYSTICK_1; joystick <= GLFW_JOYSTICK_LAST; joystick++) {
                boolean available = glfwJoystickPresent(joystick) && glfwJoystickIsGamepad(joystick);
                connected[joystick] = available;
                clearCurrentState(joystick);

                if (!available || !glfwGetGamepadState(joystick, state)) continue;
                if (activeJoystick < 0 || joystick == preferredJoystick) activeJoystick = joystick;

                ByteBuffer polledButtons = state.buttons();
                for (int button = 0; button < BUTTON_COUNT; button++) {
                    buttons[joystick][button] = polledButtons.get(button) == GLFW_PRESS;
                }

                FloatBuffer polledAxes = state.axes();
                for (int axis = 0; axis < AXIS_COUNT; axis++) {
                    axes[joystick][axis] = polledAxes.get(axis);
                }
            }
        }
    }

    /**
     * Clears the current state.
     * @param joystick the {@code int} supplied as {@code joystick}
     */
    private static void clearCurrentState(int joystick) {
        for (int button = 0; button < BUTTON_COUNT; button++) buttons[joystick][button] = false;
        for (int axis = 0; axis < AXIS_COUNT; axis++) axes[joystick][axis] = 0.0f;
    }

    /**
     * Checks whether the button pressed condition is met.
     * @param button the {@code int} supplied as {@code button}
     * @return {@code true} if button pressed; otherwise {@code false}
     */
    public static boolean isButtonPressed(int button) {
        return isButtonPressed(activeJoystick, button);
    }

    /**
     * Checks whether any gamepad binding for the logical action was pressed.
     */
    public static boolean isButtonPressed(ControlAction action) {
        for (int button : ControlConfigParser.controls.getJoystickButtonCodes(action)) {
            if (isButtonPressed(button)) return true;
        }
        return false;
    }

    /**
     * Checks whether the button pressed condition is met.
     * @param joystick the {@code int} supplied as {@code joystick}
     * @param button the {@code int} supplied as {@code button}
     * @return {@code true} if button pressed; otherwise {@code false}
     */
    public static boolean isButtonPressed(int joystick, int button) {
        return valid(joystick, button) && buttons[joystick][button] && !lastButtons[joystick][button];
    }

    /**
     * Checks whether the button down condition is met.
     * @param button the {@code int} supplied as {@code button}
     * @return {@code true} if button down; otherwise {@code false}
     */
    public static boolean isButtonDown(int button) {
        return isButtonDown(activeJoystick, button);
    }

    /**
     * Checks whether any gamepad binding for the logical action is down.
     */
    public static boolean isButtonDown(ControlAction action) {
        for (int button : ControlConfigParser.controls.getJoystickButtonCodes(action)) {
            if (isButtonDown(button)) return true;
        }
        return false;
    }

    /**
     * Checks whether the button down condition is met.
     * @param joystick the {@code int} supplied as {@code joystick}
     * @param button the {@code int} supplied as {@code button}
     * @return {@code true} if button down; otherwise {@code false}
     */
    public static boolean isButtonDown(int joystick, int button) {
        return valid(joystick, button) && buttons[joystick][button];
    }

    /**
     * Checks whether the button released condition is met.
     * @param button the {@code int} supplied as {@code button}
     * @return {@code true} if button released; otherwise {@code false}
     */
    public static boolean isButtonReleased(int button) {
        return isButtonReleased(activeJoystick, button);
    }

    /**
     * Checks whether any gamepad binding for the logical action was released.
     */
    public static boolean isButtonReleased(ControlAction action) {
        for (int button : ControlConfigParser.controls.getJoystickButtonCodes(action)) {
            if (isButtonReleased(button)) return true;
        }
        return false;
    }

    /**
     * Checks whether the button released condition is met.
     * @param joystick the {@code int} supplied as {@code joystick}
     * @param button the {@code int} supplied as {@code button}
     * @return {@code true} if button released; otherwise {@code false}
     */
    public static boolean isButtonReleased(int joystick, int button) {
        return valid(joystick, button) && !buttons[joystick][button] && lastButtons[joystick][button];
    }

    /**
     * Checks whether the key pressed condition is met.
     * @param button the {@code int} supplied as {@code button}
     * @return {@code true} if key pressed; otherwise {@code false}
     */
    public static boolean isKeyPressed(int button) {
        return isButtonPressed(button);
    }

    /**
     * Checks whether the key down condition is met.
     * @param button the {@code int} supplied as {@code button}
     * @return {@code true} if key down; otherwise {@code false}
     */
    public static boolean isKeyDown(int button) {
        return isButtonDown(button);
    }

    /**
     * Checks whether the key released condition is met.
     * @param button the {@code int} supplied as {@code button}
     * @return {@code true} if key released; otherwise {@code false}
     */
    public static boolean isKeyReleased(int button) {
        return isButtonReleased(button);
    }

    /**
     * Returns the axis.
     * @param axis the {@code int} supplied as {@code axis}
     * @return {@code float}; the axis
     */
    public static float getAxis(int axis) {
        return getAxis(activeJoystick, axis);
    }

    /**
     * Returns the axis.
     * @param joystick the {@code int} supplied as {@code joystick}
     * @param axis the {@code int} supplied as {@code axis}
     * @return {@code float}; the axis
     */
    public static float getAxis(int joystick, int axis) {
        if (joystick < 0 || joystick >= JOYSTICK_COUNT || axis < 0 || axis >= AXIS_COUNT) return 0.0f;
        return axes[joystick][axis];
    }

    /**
     * Returns the axis.
     * @param axis the {@code int} supplied as {@code axis}
     * @param deadZone the {@code float} supplied as {@code deadZone}
     * @return {@code float}; the axis
     */
    public static float getAxis(int axis, float deadZone) {
        float value = getAxis(axis);
        return Math.abs(value) < Math.clamp(deadZone, 0.0f, 1.0f) ? 0.0f : value;
    }

    /**
     * Returns the configured axis value after applying the configured dead zone.
     */
    public static float getAxis(ControlAction action) {
        int axis = ControlConfigParser.controls.getJoystickAxis(action);
        return axis < 0 ? 0.0f : getAxis(axis,
                ControlConfigParser.controls.getJoystickDeadZone());
    }

    /**
     * Checks whether the connected condition is met.
     * @return {@code true} if connected; otherwise {@code false}
     */
    public static boolean isConnected() {
        return activeJoystick >= 0;
    }

    /**
     * Checks whether the connected condition is met.
     * @param joystick the {@code int} supplied as {@code joystick}
     * @return {@code true} if connected; otherwise {@code false}
     */
    public static boolean isConnected(int joystick) {
        return joystick >= 0 && joystick < JOYSTICK_COUNT && connected[joystick];
    }

    /**
     * Returns the active joystick.
     * @return {@code int}; the active joystick
     */
    public static int getActiveJoystick() {
        return activeJoystick;
    }

    /**
     * Returns the name.
     * @param joystick the {@code int} supplied as {@code joystick}
     * @return the {@link String} representing the name
     */
    public static String getName(int joystick) {
        return isConnected(joystick) ? glfwGetGamepadName(joystick) : null;
    }

    /**
     * Determines whether valid satisfies the required comparison or validity rules.
     * @param joystick the {@code int} supplied as {@code joystick}
     * @param button the {@code int} supplied as {@code button}
     * @return {@code boolean}; the valid result
     */
    private static boolean valid(int joystick, int button) {
        return joystick >= 0 && joystick < JOYSTICK_COUNT && button >= 0 && button < BUTTON_COUNT;
    }
}
