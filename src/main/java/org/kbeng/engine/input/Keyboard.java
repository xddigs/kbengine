package org.kbeng.engine.input;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Keyboard provides keyboard capabilities within the input subsystem.
 * It defines device mappings, control-state tracking, and interaction orchestration for player actions.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public final class Keyboard {
    public static final int KEY_UNKNOWN = GLFW_KEY_UNKNOWN;
    public static final int KEY_SPACE = GLFW_KEY_SPACE;
    public static final int KEY_APOSTROPHE = GLFW_KEY_APOSTROPHE;
    public static final int KEY_COMMA = GLFW_KEY_COMMA;
    public static final int KEY_MINUS = GLFW_KEY_MINUS;
    public static final int KEY_PERIOD = GLFW_KEY_PERIOD;
    public static final int KEY_SLASH = GLFW_KEY_SLASH;
    public static final int KEY_0 = GLFW_KEY_0;
    public static final int KEY_1 = GLFW_KEY_1;
    public static final int KEY_2 = GLFW_KEY_2;
    public static final int KEY_3 = GLFW_KEY_3;
    public static final int KEY_4 = GLFW_KEY_4;
    public static final int KEY_5 = GLFW_KEY_5;
    public static final int KEY_6 = GLFW_KEY_6;
    public static final int KEY_7 = GLFW_KEY_7;
    public static final int KEY_8 = GLFW_KEY_8;
    public static final int KEY_9 = GLFW_KEY_9;
    public static final int KEY_SEMICOLON = GLFW_KEY_SEMICOLON;
    public static final int KEY_EQUAL = GLFW_KEY_EQUAL;
    public static final int KEY_A = GLFW_KEY_A;
    public static final int KEY_B = GLFW_KEY_B;
    public static final int KEY_C = GLFW_KEY_C;
    public static final int KEY_D = GLFW_KEY_D;
    public static final int KEY_E = GLFW_KEY_E;
    public static final int KEY_F = GLFW_KEY_F;
    public static final int KEY_G = GLFW_KEY_G;
    public static final int KEY_H = GLFW_KEY_H;
    public static final int KEY_I = GLFW_KEY_I;
    public static final int KEY_J = GLFW_KEY_J;
    public static final int KEY_K = GLFW_KEY_K;
    public static final int KEY_L = GLFW_KEY_L;
    public static final int KEY_M = GLFW_KEY_M;
    public static final int KEY_N = GLFW_KEY_N;
    public static final int KEY_O = GLFW_KEY_O;
    public static final int KEY_P = GLFW_KEY_P;
    public static final int KEY_Q = GLFW_KEY_Q;
    public static final int KEY_R = GLFW_KEY_R;
    public static final int KEY_S = GLFW_KEY_S;
    public static final int KEY_T = GLFW_KEY_T;
    public static final int KEY_U = GLFW_KEY_U;
    public static final int KEY_V = GLFW_KEY_V;
    public static final int KEY_W = GLFW_KEY_W;
    public static final int KEY_X = GLFW_KEY_X;
    public static final int KEY_Y = GLFW_KEY_Y;
    public static final int KEY_Z = GLFW_KEY_Z;
    public static final int KEY_LEFT_BRACKET = GLFW_KEY_LEFT_BRACKET;
    public static final int KEY_BACKSLASH = GLFW_KEY_BACKSLASH;
    public static final int KEY_RIGHT_BRACKET = GLFW_KEY_RIGHT_BRACKET;
    public static final int KEY_GRAVE_ACCENT = GLFW_KEY_GRAVE_ACCENT;
    public static final int KEY_WORLD_1 = GLFW_KEY_WORLD_1;
    public static final int KEY_WORLD_2 = GLFW_KEY_WORLD_2;
    public static final int KEY_ESCAPE = GLFW_KEY_ESCAPE;
    public static final int KEY_ENTER = GLFW_KEY_ENTER;
    public static final int KEY_TAB = GLFW_KEY_TAB;
    public static final int KEY_BACKSPACE = GLFW_KEY_BACKSPACE;
    public static final int KEY_INSERT = GLFW_KEY_INSERT;
    public static final int KEY_DELETE = GLFW_KEY_DELETE;
    public static final int KEY_RIGHT = GLFW_KEY_RIGHT;
    public static final int KEY_LEFT = GLFW_KEY_LEFT;
    public static final int KEY_DOWN = GLFW_KEY_DOWN;
    public static final int KEY_UP = GLFW_KEY_UP;
    public static final int KEY_PAGE_UP = GLFW_KEY_PAGE_UP;
    public static final int KEY_PAGE_DOWN = GLFW_KEY_PAGE_DOWN;
    public static final int KEY_HOME = GLFW_KEY_HOME;
    public static final int KEY_END = GLFW_KEY_END;
    public static final int KEY_CAPS_LOCK = GLFW_KEY_CAPS_LOCK;
    public static final int KEY_SCROLL_LOCK = GLFW_KEY_SCROLL_LOCK;
    public static final int KEY_NUM_LOCK = GLFW_KEY_NUM_LOCK;
    public static final int KEY_PRINT_SCREEN = GLFW_KEY_PRINT_SCREEN;
    public static final int KEY_PAUSE = GLFW_KEY_PAUSE;
    public static final int KEY_F1 = GLFW_KEY_F1;
    public static final int KEY_F2 = GLFW_KEY_F2;
    public static final int KEY_F3 = GLFW_KEY_F3;
    public static final int KEY_F4 = GLFW_KEY_F4;
    public static final int KEY_F5 = GLFW_KEY_F5;
    public static final int KEY_F6 = GLFW_KEY_F6;
    public static final int KEY_F7 = GLFW_KEY_F7;
    public static final int KEY_F8 = GLFW_KEY_F8;
    public static final int KEY_F9 = GLFW_KEY_F9;
    public static final int KEY_F10 = GLFW_KEY_F10;
    public static final int KEY_F11 = GLFW_KEY_F11;
    public static final int KEY_F12 = GLFW_KEY_F12;
    public static final int KEY_F13 = GLFW_KEY_F13;
    public static final int KEY_F14 = GLFW_KEY_F14;
    public static final int KEY_F15 = GLFW_KEY_F15;
    public static final int KEY_F16 = GLFW_KEY_F16;
    public static final int KEY_F17 = GLFW_KEY_F17;
    public static final int KEY_F18 = GLFW_KEY_F18;
    public static final int KEY_F19 = GLFW_KEY_F19;
    public static final int KEY_F20 = GLFW_KEY_F20;
    public static final int KEY_F21 = GLFW_KEY_F21;
    public static final int KEY_F22 = GLFW_KEY_F22;
    public static final int KEY_F23 = GLFW_KEY_F23;
    public static final int KEY_F24 = GLFW_KEY_F24;
    public static final int KEY_F25 = GLFW_KEY_F25;
    public static final int KEY_KP_0 = GLFW_KEY_KP_0;
    public static final int KEY_KP_1 = GLFW_KEY_KP_1;
    public static final int KEY_KP_2 = GLFW_KEY_KP_2;
    public static final int KEY_KP_3 = GLFW_KEY_KP_3;
    public static final int KEY_KP_4 = GLFW_KEY_KP_4;
    public static final int KEY_KP_5 = GLFW_KEY_KP_5;
    public static final int KEY_KP_6 = GLFW_KEY_KP_6;
    public static final int KEY_KP_7 = GLFW_KEY_KP_7;
    public static final int KEY_KP_8 = GLFW_KEY_KP_8;
    public static final int KEY_KP_9 = GLFW_KEY_KP_9;
    public static final int KEY_KP_DECIMAL = GLFW_KEY_KP_DECIMAL;
    public static final int KEY_KP_DIVIDE = GLFW_KEY_KP_DIVIDE;
    public static final int KEY_KP_MULTIPLY = GLFW_KEY_KP_MULTIPLY;
    public static final int KEY_KP_SUBTRACT = GLFW_KEY_KP_SUBTRACT;
    public static final int KEY_KP_ADD = GLFW_KEY_KP_ADD;
    public static final int KEY_KP_ENTER = GLFW_KEY_KP_ENTER;
    public static final int KEY_KP_EQUAL = GLFW_KEY_KP_EQUAL;
    public static final int KEY_LEFT_SHIFT = GLFW_KEY_LEFT_SHIFT;
    public static final int KEY_LEFT_CONTROL = GLFW_KEY_LEFT_CONTROL;
    public static final int KEY_LEFT_ALT = GLFW_KEY_LEFT_ALT;
    public static final int KEY_LEFT_SUPER = GLFW_KEY_LEFT_SUPER;
    public static final int KEY_RIGHT_SHIFT = GLFW_KEY_RIGHT_SHIFT;
    public static final int KEY_RIGHT_CONTROL = GLFW_KEY_RIGHT_CONTROL;
    public static final int KEY_RIGHT_ALT = GLFW_KEY_RIGHT_ALT;
    public static final int KEY_RIGHT_SUPER = GLFW_KEY_RIGHT_SUPER;
    public static final int KEY_MENU = GLFW_KEY_MENU;
    public static final int KEY_LAST = GLFW_KEY_LAST;

    private static final boolean[] keys = new boolean[KEY_LAST + 1];
    private static final boolean[] lastKeys = new boolean[KEY_LAST + 1];
    private static final StringBuilder typedCharacters = new StringBuilder();
    private static int modifiers;

    /**
     * Creates a new {@code Keyboard} instance. In private because
     * this is a static class.
     */
    private Keyboard() {}

    /**
     * Initializes the component.
     * @param windowId the {@code long} supplied as {@code windowId}
     */
    public static void init(long windowId) {
        glfwSetKeyCallback(windowId, (window, key, scancode, action, mods) -> {
            modifiers = mods;
            if (key >= 0 && key <= KEY_LAST) {
                keys[key] = action != GLFW_RELEASE;
            }
        });

        glfwSetCharCallback(windowId, (window, codepoint) -> {
            typedCharacters.appendCodePoint(codepoint);
        });
    }

    /**
     * Updates the current state.
     */
    public static void update() {
        System.arraycopy(keys, 0, lastKeys, 0, keys.length);
    }

    /**
     * Checks whether the key pressed condition is met.
     * @param keyCode the {@code int} supplied as {@code keyCode}
     * @return {@code true} if key pressed; otherwise {@code false}
     */
    public static boolean isKeyPressed(int keyCode) {
        return keyCode >= 0 && keyCode <= KEY_LAST && keys[keyCode] && !lastKeys[keyCode];
    }

    /**
     * Checks whether any keyboard binding for the logical action was pressed.
     */
    public static boolean isKeyPressed(ControlAction action) {
        for (int keyCode : ControlConfigParser.controls.getKeyboardCodes(action)) {
            if (isKeyPressed(keyCode)) return true;
        }
        return false;
    }

    /**
     * Checks whether the key down condition is met.
     * @param keyCode the {@code int} supplied as {@code keyCode}
     * @return {@code true} if key down; otherwise {@code false}
     */
    public static boolean isKeyDown(int keyCode) {
        return keyCode >= 0 && keyCode <= KEY_LAST && keys[keyCode];
    }

    /**
     * Checks whether any keyboard binding for the logical action is down.
     */
    public static boolean isKeyDown(ControlAction action) {
        for (int keyCode : ControlConfigParser.controls.getKeyboardCodes(action)) {
            if (isKeyDown(keyCode)) return true;
        }
        return false;
    }

    /**
     * Checks whether the key released condition is met.
     * @param keyCode the {@code int} supplied as {@code keyCode}
     * @return {@code true} if key released; otherwise {@code false}
     */
    public static boolean isKeyReleased(int keyCode) {
        return keyCode >= 0 && keyCode <= KEY_LAST && !keys[keyCode] && lastKeys[keyCode];
    }

    /**
     * Checks whether any keyboard binding for the logical action was released.
     */
    public static boolean isKeyReleased(ControlAction action) {
        for (int keyCode : ControlConfigParser.controls.getKeyboardCodes(action)) {
            if (isKeyReleased(keyCode)) return true;
        }
        return false;
    }

    /**
     * Updates or derives runtime state for any key pressed according to the supplied arguments.
     * @return {@code boolean}; the any key pressed result
     */
    public static boolean anyKeyPressed() {
        for (boolean key : keys) {
            if (key) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the typed characters.
     * @return the {@link String} representing the typed characters
     */
    public static String getTypedCharacters() {
        String text = typedCharacters.toString();
        typedCharacters.setLength(0);
        return text;
    }

    /**
     * Returns the modifiers.
     * @return {@code int}; the modifiers
     */
    public static int getModifiers() {
        return modifiers;
    }
}
