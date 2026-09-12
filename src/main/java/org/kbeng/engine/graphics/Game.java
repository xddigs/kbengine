package org.kbeng.engine.graphics;

import org.kbeng.engine.input.Joystick;
import org.kbeng.engine.input.Keyboard;
import org.kbeng.engine.input.Mouse;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Game provides game capabilities within the core subsystem.
 * It belongs to the application bootstrap layer and integrates top-level runtime wiring.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public final class Game {
    private static final Logger log = LoggerFactory.getLogger(Game.class);
    private static final int OPENGL_MAJOR_VERSION = 3;
    private static final int OPENGL_MINOR_VERSION = 3;
    private static final int VSYNC_INTERVAL = 1;

    private long window;
    private final Application application;
    private final Application.Configuration configuration;

    public Game(Application application) {
        this.application = java.util.Objects.requireNonNull(application, "application");
        this.configuration = java.util.Objects.requireNonNull(
                application.configuration(), "application configuration");
    }

    /**
     * Executes run as part of the application lifecycle.
     */
    public void run() {
        log.info("Starting LWJGL 3 application...");
        boolean applicationInitialized = false;
        try {
            init();
            applicationInitialized = true;
            new Intro(window, application, configuration).show();
        } finally {
            log.debug("Closing window and releasing native resources...");
            try {
                if (applicationInitialized) application.dispose();
            } finally {
                Mouse.dispose();
                if (window != MemoryUtil.NULL) {
                    Callbacks.glfwFreeCallbacks(window);
                    GLFW.glfwDestroyWindow(window);
                }
                GLFW.glfwTerminate();
                GLFWErrorCallback callback = GLFW.glfwSetErrorCallback(null);
                if (callback != null) callback.free();
                log.info("Application terminated successfully.");
            }
        }
    }

    /**
     * Initializes the component.
     */
    private void init() {
        GLFWErrorCallback.create((error, description) ->
                log.error("GLFW Error [0x{}]: {}", Integer.toHexString(error),
                        GLFWErrorCallback.getDescription(description))).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, OPENGL_MAJOR_VERSION);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, OPENGL_MINOR_VERSION);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(
                configuration.initialWidth(),
                configuration.initialHeight(),
                configuration.windowTitle(), MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        setWindowIcon(window, configuration.windowIcons());

        GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 16);
        Keyboard.init(window);
        Mouse.init(window);
        if (configuration.cursorImagePath() != null
                && !configuration.cursorImagePath().isBlank()) {
            Mouse.setCursorImage(configuration.cursorImagePath());
        }
        Joystick.init();

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        log.info("OpenGL context loaded successfully.");
        log.info("GPU Renderer: {}", GL11.glGetString(GL11.GL_RENDERER));
        log.info("OpenGL Version: {}", GL11.glGetString(GL11.GL_VERSION));

        GLFW.glfwSwapInterval(VSYNC_INTERVAL);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        GLFW.glfwSwapBuffers(window);
        GLFW.glfwShowWindow(window);

        GLFW.glfwSetCursorPos(window, configuration.initialWidth() / 2.0,
                configuration.initialHeight() / 2.0);
        log.info("GLFW window successfully initialized.");
    }

    /**
     * Sets the window icon.
     */
    public static void setWindowIcon(long windowHandle,
                                     java.util.List<Application.WindowIcon> windowIcons) {
        if (windowIcons == null || windowIcons.isEmpty()) return;
        ByteBuffer[] pixels = new ByteBuffer[windowIcons.size()];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage.Buffer icons = GLFWImage.malloc(windowIcons.size(), stack);
            for (int i = 0; i < windowIcons.size(); i++) {
                Application.WindowIcon icon = windowIcons.get(i);
                pixels[i] = loadIcon(icons, i, icon.resourcePath(), icon.size(), stack);
            }
            icons.position(0);
            icons.limit(windowIcons.size());
            GLFW.glfwSetWindowIcon(windowHandle, icons);
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
}
