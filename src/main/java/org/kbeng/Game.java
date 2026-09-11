package org.kbeng;

import org.kbeng.graphics.Intro;
import org.kbeng.input.Joystick;
import org.kbeng.input.Keyboard;
import org.kbeng.input.Mouse;
import org.kbeng.utils.K;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Represents the game component of the Isofarm runtime.
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
public class Game {
    private static final Logger log = LoggerFactory.getLogger(Game.class);
    private static final String WINDOW_TITLE = "Isofarm";

    private static final int OPENGL_MAJOR_VERSION = 3;
    private static final int OPENGL_MINOR_VERSION = 3;
    private static final int VSYNC_INTERVAL = 1;

    private long window;

    /**
     * Executes run as part of the application lifecycle.
     */
    public void run() {
        log.info("Starting LWJGL 3 application...");
        init();

        log.debug("Closing window and releasing native resources...");
        Mouse.dispose();
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
        log.info("Application terminated successfully.");
    }

    /**
     * Initializes the component.
     */
    private void init() {
        GLFWErrorCallback.create((error, description) ->
                log.error("GLFW Error [0x{}]: {}", Integer.toHexString(error),
                        GLFWErrorCallback.getDescription(description))
        ).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, OPENGL_MAJOR_VERSION);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, OPENGL_MINOR_VERSION);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(
                (int) K.Window.DEFAULT_WIDTH,
                (int) K.Window.DEFAULT_HEIGHT,
                WINDOW_TITLE, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        setWindowIcon(window);

        glfwWindowHint(GLFW_SAMPLES, 16);
        Keyboard.init(window);
        Mouse.init(window);
        Mouse.setCursorImage(K.Paths.CURSOR_POINTER);
        Joystick.init();

        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        log.info("OpenGL context loaded successfully.");
        log.info("GPU Renderer: {}", glGetString(GL_RENDERER));
        log.info("OpenGL Version: {}", glGetString(GL_VERSION));

        glfwSwapInterval(VSYNC_INTERVAL);
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glfwSwapBuffers(window);
        glfwShowWindow(window);
        Intro screen = new Intro(window);
        screen.show();
        glfwSetCursorPos(window, K.Window.DEFAULT_WIDTH / 2, K.Window.DEFAULT_HEIGHT / 2);
        log.info("GLFW window successfully initialized.");
    }

    /**
     * Sets the window icon.
     */
    public static void setWindowIcon(long windowHandle) {
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        String[] resources = {
                "/assets/ui/iconx32.png",
                "/assets/ui/iconx32.png",
                "/assets/ui/iconx32.png",
                "/assets/ui/iconx64.png",
                "/assets/ui/iconx64.png",
                "/assets/ui/iconx128.png",
                "/assets/ui/iconx256.png"
        };
        ByteBuffer[] pixels = new ByteBuffer[sizes.length];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage.Buffer icons = GLFWImage.malloc(sizes.length, stack);
            for (int i = 0; i < sizes.length; i++) {
                pixels[i] = loadIcon(icons, i, resources[i], sizes[i], stack);
            }
            icons.position(0);
            icons.limit(sizes.length);
            glfwSetWindowIcon(windowHandle, icons);
        } finally {
            for (ByteBuffer pixelBuffer : pixels) {
                if (pixelBuffer != null) MemoryUtil.memFree(pixelBuffer);
            }
        }
    }

    /**
     * Loads the icon.
     * @param icons the {@link GLFWImage.Buffer} supplied as {@code icons}
     * @param index the {@code int} supplied as {@code index}
     * @param resourcePath the {@link String} supplied as {@code resourcePath}
     * @param stack the {@link MemoryStack} supplied as {@code stack}
     * @return the {@link ByteBuffer} representing the load icon result
     */
    private static ByteBuffer loadIcon(GLFWImage.Buffer icons, int index, String resourcePath,
                                       int targetSize, MemoryStack stack) {
        try (InputStream input = Game.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new NullPointerException("Window icon not found: " + resourcePath);
            }

            byte[] data = input.readAllBytes();
            ByteBuffer imageBuffer = MemoryUtil.memAlloc(data.length);
            imageBuffer.put(data).flip();

            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer sourcePixels;
            try {
                sourcePixels = STBImage.stbi_load_from_memory(imageBuffer, width,
                        height, channels, 4);
            } finally {
                MemoryUtil.memFree(imageBuffer);
            }

            if (sourcePixels == null) {
                throw new NullPointerException(STBImage.stbi_failure_reason());
            }

            ByteBuffer squarePixels;
            try {
                squarePixels = fit(sourcePixels, width.get(0), height.get(0), targetSize);
            } finally {
                STBImage.stbi_image_free(sourcePixels);
            }

            icons.get(index)
                    .width(targetSize)
                    .height(targetSize)
                    .pixels(squarePixels);
            return squarePixels;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read window icon: " + resourcePath, e);
        }
    }

    /**
     * Fits a non-square source image into a transparent square RGBA canvas.
     */
    private static ByteBuffer fit(ByteBuffer source, int width, int height,
                                  int targetSize) {
        ByteBuffer result = MemoryUtil.memCalloc(targetSize * targetSize * 4);
        float scale = Math.min((float) targetSize / width, (float) targetSize / height);
        int scaledWidth = Math.max(1, Math.round(width * scale));
        int scaledHeight = Math.max(1, Math.round(height * scale));
        int offsetX = (targetSize - scaledWidth) / 2;
        int offsetY = (targetSize - scaledHeight) / 2;

        for (int y = 0; y < scaledHeight; y++) {
            int sourceY = Math.min(height - 1, y * height / scaledHeight);
            for (int x = 0; x < scaledWidth; x++) {
                int sourceX = Math.min(width - 1, x * width / scaledWidth);
                int sourceIndex = (sourceY * width + sourceX) * 4;
                int targetIndex = ((y + offsetY) * targetSize + x + offsetX) * 4;
                for (int channel = 0; channel < 4; channel++) {
                    result.put(targetIndex + channel, source.get(sourceIndex + channel));
                }
            }
        }
        return result;
    }

    /**
     * Executes main as part of the application lifecycle.
     * @param ignoredArgs an array of {@link String} values supplied as {@code ignoredArgs}
     */
    public static void main(String[] ignoredArgs) {
        new Game().run();
    }
}
