package org.kbeng.engine.input;

import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Mouse provides mouse capabilities within the input subsystem.
 * It defines device mappings, control-state tracking, and interaction orchestration for player actions.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public final class Mouse {
    private static final int CURSOR_HOTSPOT_X = 20;
    private static final int CURSOR_HOTSPOT_Y = 8;

    public static final int BUTTON_1 = GLFW_MOUSE_BUTTON_1;
    public static final int BUTTON_2 = GLFW_MOUSE_BUTTON_2;
    public static final int BUTTON_3 = GLFW_MOUSE_BUTTON_3;
    public static final int BUTTON_4 = GLFW_MOUSE_BUTTON_4;
    public static final int BUTTON_5 = GLFW_MOUSE_BUTTON_5;
    public static final int BUTTON_6 = GLFW_MOUSE_BUTTON_6;
    public static final int BUTTON_7 = GLFW_MOUSE_BUTTON_7;
    public static final int BUTTON_8 = GLFW_MOUSE_BUTTON_8;
    public static final int BUTTON_LEFT = GLFW_MOUSE_BUTTON_LEFT;
    public static final int BUTTON_RIGHT = GLFW_MOUSE_BUTTON_RIGHT;
    public static final int BUTTON_MIDDLE = GLFW_MOUSE_BUTTON_MIDDLE;
    public static final int BUTTON_LAST = GLFW_MOUSE_BUTTON_LAST;

    private static final boolean[] buttons = new boolean[BUTTON_LAST + 1];
    private static final boolean[] lastButtons = new boolean[BUTTON_LAST + 1];

    private static float x = 0, y = 0;
    private static float lastX = 0, lastY = 0;
    private static float deltaX = 0, deltaY = 0;
    private static boolean firstMouse = true;
    private static float scrollY = 0.0f;
    private static long windowId;
    private static long defaultCursor;
    private static long hoverCursor;
    private static boolean cursorHovered;

    /**
     * Creates a new private* {@code Mouse} instance.
     */
    private Mouse() {}

    /**
     * Initializes the component.
     * @param windowId the {@code long} supplied as {@code windowId}
     */
    public static void init(long windowId) {
        Mouse.windowId = windowId;
        glfwSetCursorPosCallback(windowId, (window, xpos, ypos) -> {
            float currentX = (float) xpos;
            float currentY = (float) ypos;

            if (firstMouse) {
                lastX = currentX;
                lastY = currentY;
                firstMouse = false;
            }

            deltaX = currentX - lastX;
            deltaY = currentY - lastY;

            lastX = currentX;
            lastY = currentY;
            x = currentX;
            y = currentY;
        });

        glfwSetMouseButtonCallback(windowId, (window, button, action, scanner) -> {
            if (button >= 0 && button <= BUTTON_LAST) {
                buttons[button] = (action != GLFW_RELEASE);
            }
        });

        glfwSetScrollCallback(windowId, (window, xoffset, yoffset) -> {
            scrollY = (float) yoffset;
        });
    }

    /**
     * Updates the current state.
     */
    public static void update() {
        System.arraycopy(buttons, 0, lastButtons, 0, buttons.length);
        deltaX = 0;
        deltaY = 0;
        scrollY = 0.0f;
    }

    /**
     * Checks whether the button down condition is met.
     * @param button the {@code int} supplied as {@code button}
     * @return {@code true} if button down; otherwise {@code false}
     */
    public static boolean isButtonDown(int button) {
        return button >= 0 && button <= BUTTON_LAST && buttons[button];
    }

    /**
     * Checks whether any mouse binding for the logical action is down.
     */
    public static boolean isButtonDown(ControlAction action) {
        for (int button : ControlConfigParser.controls.getMouseCodes(action)) {
            if (isButtonDown(button)) return true;
        }
        return false;
    }

    /**
     * Checks whether the button pressed condition is met.
     * @param button the {@code int} supplied as {@code button}
     * @return {@code true} if button pressed; otherwise {@code false}
     */
    public static boolean isButtonPressed(int button) {
        return button >= 0 && button <= BUTTON_LAST
                && buttons[button] && !lastButtons[button];
    }

    /**
     * Checks whether any mouse binding for the logical action was pressed.
     */
    public static boolean isButtonPressed(ControlAction action) {
        for (int button : ControlConfigParser.controls.getMouseCodes(action)) {
            if (isButtonPressed(button)) return true;
        }
        return false;
    }

    /**
     * Checks whether the button released condition is met.
     * @param button the {@code int} supplied as {@code button}
     * @return {@code true} if button released; otherwise {@code false}
     */
    public static boolean isButtonReleased(int button) {
        return button >= 0 &&
                button <= BUTTON_LAST &&
                !buttons[button] &&
                lastButtons[button];
    }

    /**
     * Checks whether any mouse binding for the logical action was released.
     */
    public static boolean isButtonReleased(ControlAction action) {
        for (int button : ControlConfigParser.controls.getMouseCodes(action)) {
            if (isButtonReleased(button)) return true;
        }
        return false;
    }

    /**
     * Loads a horizontal two-frame cursor spritesheet. Frame 0 is the default
     * cursor and frame 1 is used while hovering an interactive UI element.
     * @param path the {@link String} argument; classpath path to the cursor spritesheet
     */
    public static void setCursorImage(String path) {
        byte[] rawData;
        try (InputStream in = Mouse.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("Cursor resource not found: " + path);
            }
            rawData = in.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read cursor spritesheet [" + path + "]", e);
        }

        ByteBuffer rawBuffer = MemoryUtil.memAlloc(rawData.length);
        ByteBuffer sheetPixels = null;
        long newDefaultCursor = NULL;
        long newHoverCursor = NULL;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            rawBuffer.put(rawData).flip();
            IntBuffer widthBuffer = stack.mallocInt(1);
            IntBuffer heightBuffer = stack.mallocInt(1);
            IntBuffer channelsBuffer = stack.mallocInt(1);

            stbi_set_flip_vertically_on_load(false);
            sheetPixels = stbi_load_from_memory(rawBuffer, widthBuffer,
                    heightBuffer, channelsBuffer, 4);
            if (sheetPixels == null) {
                throw new IllegalArgumentException("Failed to decode cursor spritesheet [" +
                        path + "]: " + stbi_failure_reason());
            }

            int sheetWidth = widthBuffer.get(0);
            int frameHeight = heightBuffer.get(0);
            if (sheetWidth % 2 != 0) {
                throw new IllegalArgumentException("Cursor spritesheet must contain two equal horizontal frames: " + path);
            }

            int frameWidth = sheetWidth / 2;
            newDefaultCursor = createCursorFrame(sheetPixels, sheetWidth,
                    frameWidth, frameHeight, 0, stack);
            newHoverCursor = createCursorFrame(sheetPixels, sheetWidth,
                    frameWidth, frameHeight, 1, stack);

            if (newDefaultCursor == NULL || newHoverCursor == NULL) {
                throw new IllegalStateException("Failed to create GLFW cursors from: " + path);
            }
        } finally {
            MemoryUtil.memFree(rawBuffer);
            if (sheetPixels != null) stbi_image_free(sheetPixels);
            if (newDefaultCursor == NULL && newHoverCursor != NULL) {
                glfwDestroyCursor(newHoverCursor);
            } else if (newHoverCursor == NULL && newDefaultCursor != NULL) {
                glfwDestroyCursor(newDefaultCursor);
            }
        }

        destroyCursors();
        defaultCursor = newDefaultCursor;
        hoverCursor = newHoverCursor;
        cursorHovered = false;
        glfwSetCursor(windowId, defaultCursor);
    }

    private static long createCursorFrame(ByteBuffer sheetPixels, int sheetWidth,
                                          int frameWidth, int frameHeight,
                                          int frame, MemoryStack stack) {
        ByteBuffer framePixels = MemoryUtil.memAlloc(frameWidth * frameHeight * 4);
        try {
            int frameOffset = frame * frameWidth;
            for (int row = 0; row < frameHeight; row++) {
                for (int column = 0; column < frameWidth; column++) {
                    int source = (row * sheetWidth + frameOffset + column) * 4;
                    int destination = (row * frameWidth + column) * 4;
                    framePixels.put(destination, sheetPixels.get(source));
                    framePixels.put(destination + 1, sheetPixels.get(source + 1));
                    framePixels.put(destination + 2, sheetPixels.get(source + 2));
                    framePixels.put(destination + 3, sheetPixels.get(source + 3));
                }
            }

            int hotspotX = Math.clamp(CURSOR_HOTSPOT_X, 0, frameWidth - 1);
            int hotspotY = Math.clamp(CURSOR_HOTSPOT_Y, 0, frameHeight - 1);
            GLFWImage image = GLFWImage.malloc(stack)
                    .set(frameWidth, frameHeight, framePixels);
            return glfwCreateCursor(image, hotspotX, hotspotY);
        } finally {
            MemoryUtil.memFree(framePixels);
        }
    }

    /**
     * Selects the hover or default frame of the configured cursor.
     */
    public static void setCursorHovered(boolean hovered) {
        if (cursorHovered == hovered || defaultCursor == NULL || hoverCursor == NULL) return;
        cursorHovered = hovered;
        glfwSetCursor(windowId, hovered ? hoverCursor : defaultCursor);
    }

    /**
     * Releases the native cursor handles.
     */
    public static void dispose() {
        destroyCursors();
        windowId = NULL;
    }

    private static void destroyCursors() {
        if (defaultCursor != NULL) glfwDestroyCursor(defaultCursor);
        if (hoverCursor != NULL) glfwDestroyCursor(hoverCursor);
        defaultCursor = NULL;
        hoverCursor = NULL;
        cursorHovered = false;
    }

    /**
     * Returns the delta x.
     * @return {@code float}; the delta x
     */
    public static float getDeltaX() { return deltaX; }
    /**
     * Returns the delta y.
     * @return {@code float}; the delta y
     */
    public static float getDeltaY() { return deltaY; }
    /**
     * Returns the x.
     * @return {@code float}; the x
     */
    public static float getX() { return x; }
    /**
     * Returns the y.
     * @return {@code float}; the y
     */
    public static float getY() { return y; }

    /**
     * Returns the scroll y.
     * @return {@code float}; the scroll y
     */
    public static float getScrollY() {
        return scrollY;
    }

    /**
     * Sets the scroll y.
     * @param scrollY the {@code float} supplied as {@code scrollY}
     */
    public static void setScrollY(float scrollY) {
        Mouse.scrollY = scrollY;
    }
}
