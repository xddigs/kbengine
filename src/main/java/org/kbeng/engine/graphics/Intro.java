package org.kbeng.engine.graphics;

import org.joml.Vector4f;
import org.kbeng.engine.input.ControlAction;
import org.kbeng.engine.input.Controls;
import org.kbeng.engine.ui.Frontend;
import org.kbeng.engine.ui.UIFont;
import org.kbeng.engine.ui.UIManager;
import org.kbeng.engine.ui.UIProgressBar;
import org.kbeng.engine.utils.K;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Generic splash, loading screen and frame-loop host for an {@link Application}.
 * It deliberately knows nothing about the application's world or gameplay.
 */
public final class Intro {
    private static final long LOADING_FRAME_INTERVAL_NANOS = 100_000_000L;
    private static final float SPLASH_DURATION_SECONDS = 4.0f;
    private static final float SPLASH_FADE_DURATION_SECONDS = 1.5f;
    private static final float SPLASH_MAX_WIDTH = 900.0f;

    private final long window;
    private final Application application;
    private final Application.Configuration configuration;
    private UIManager uiManager;
    private UIProgressBar progressBar;
    private int framebufferWidth;
    private int framebufferHeight;
    private long lastLoadingFrameNanos;
    private boolean fullscreen;
    private int windowedX;
    private int windowedY;
    private int windowedWidth;
    private int windowedHeight;

    public Intro(long window, Application application,
                 Application.Configuration configuration) {
        this.window = window;
        this.application = application;
        this.configuration = configuration;
    }

    /** Runs the generic startup flow followed by the application frame loop. */
    public void show() {
        setupUI();
        setupCallbacks();
        if (configuration.startFullscreen()) toggleFullscreen();
        updateFramebufferSize();
        application.onResize(framebufferWidth, framebufferHeight);
        showSplashScreen();
        if (glfwWindowShouldClose(window)) return;

        Application.Context context = new Application.Context(window, uiManager,
                framebufferWidth, framebufferHeight);
        application.initialize(context, this::onLoadingProgress);
        if (glfwWindowShouldClose(window)) return;

        progressBar.hide();
        renderLoadingFrame(null);
        application.start();
        if (!glfwWindowShouldClose(window)) loop();
    }

    private void setupUI() {
        updateFramebufferSize();
        float barWidth = 500.0f;
        float barHeight = 25.0f;
        float x = (framebufferWidth - barWidth) / 2.0f;
        float y = (framebufferHeight - barHeight) / 2.0f;

        uiManager = new UIManager(framebufferWidth, framebufferHeight);
        progressBar = new UIProgressBar(x, y, barWidth, barHeight,
                0.0f, 100.0f, false);
        progressBar.setColors(new Vector4f(0.0f, 0.90f, 0.4f, 1.0f),
                new Vector4f(0.2f, 0.2f, 0.2f, 1.0f));
        progressBar.show();
        uiManager.getRoot().show();
        uiManager.getRoot().addChild(progressBar);
        uiManager.resize(framebufferWidth, framebufferHeight);
    }

    private void onLoadingProgress(Application.LoadingProgress progress) {
        progressBar.setValue(progress.progress() * 100.0f);
        renderLoadingProgress(progress.statusText());
    }

    private void showSplashScreen() {
        String logoPath = configuration.splashLogoPath();
        if (logoPath == null || logoPath.isBlank()) return;

        Texture logo = new Texture(logoPath);
        double startTime = glfwGetTime();
        try {
            while (!glfwWindowShouldClose(window)) {
                glfwPollEvents();
                float elapsed = (float) (glfwGetTime() - startTime);
                if (elapsed >= SPLASH_DURATION_SECONDS) return;
                renderSplashFrame(logo, elapsed);
            }
        } finally {
            logo.dispose();
        }
    }

    private void renderSplashFrame(Texture logo, float elapsed) {
        updateFramebufferSize();
        glViewport(0, 0, framebufferWidth, framebufferHeight);
        glClearColor(0.15f, 0.15f, 0.15f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        float fadeProgress = Math.min(elapsed, SPLASH_DURATION_SECONDS - elapsed)
                / SPLASH_FADE_DURATION_SECONDS;
        float opacity = smoothStep(Math.clamp(fadeProgress, 0.0f, 1.0f));
        float scale = 0.9f + 0.1f * opacity;
        float logoWidth = Math.min(SPLASH_MAX_WIDTH, framebufferWidth * 0.7f) * scale;
        float logoHeight = logoWidth * logo.getHeight() / logo.getWidth();
        float x = (framebufferWidth - logoWidth) / 2.0f;
        float y = (framebufferHeight - logoHeight) / 2.0f;

        Frontend.begin(framebufferWidth, framebufferHeight);
        Frontend.drawTexture(logo, x, y, logoWidth, logoHeight,
                new Vector4f(1.0f, 1.0f, 1.0f, opacity));
        Frontend.end();
        glfwSwapBuffers(window);
    }

    private float smoothStep(float value) {
        return value * value * (3.0f - 2.0f * value);
    }

    private void renderLoadingProgress(String statusText) {
        long now = System.nanoTime();
        if (lastLoadingFrameNanos != 0L
                && now - lastLoadingFrameNanos < LOADING_FRAME_INTERVAL_NANOS) return;
        renderLoadingFrame(statusText);
        lastLoadingFrameNanos = System.nanoTime();
    }

    private void renderLoadingFrame(String statusText) {
        updateFramebufferSize();
        glViewport(0, 0, framebufferWidth, framebufferHeight);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glClearColor(0.15f, 0.15f, 0.15f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        uiManager.update(0.016f);
        Frontend.begin(framebufferWidth, framebufferHeight);
        uiManager.render();

        if (statusText != null && !statusText.isBlank()) {
            float textX = (framebufferWidth - 500.0f) / 2.0f;
            float textY = ((framebufferHeight - 25.0f) / 2.0f) - 30.0f;
            UIFont font = Frontend.getNormalFont();
            Frontend.drawString(statusText, textX, textY, font, K.UI.UI_TEXT_COLOR);
        }

        Frontend.end();
        glfwSwapBuffers(window);
        glFlush();
    }

    private void setupCallbacks() {
        glfwSetFramebufferSizeCallback(window, (windowHandle, width, height) -> {
            if (width <= 0 || height <= 0) return;
            framebufferWidth = width;
            framebufferHeight = height;
            glViewport(0, 0, width, height);
            if (uiManager != null) uiManager.resize(width, height);
            Frontend.resize(width, height);
            application.onResize(width, height);
            repositionProgressBar();
        });
    }

    private void loop() {
        double lastTime = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double currentTime = glfwGetTime();
            float delta = (float) (currentTime - lastTime);
            lastTime = currentTime;

            glfwPollEvents();
            if (Controls.isDown(ControlAction.SMART_SHIFT)
                    && Controls.isPressed(ControlAction.QUIT)) {
                glfwSetWindowShouldClose(window, true);
            }
            if (Controls.isPressed(ControlAction.TOGGLE_FULLSCREEN)) toggleFullscreen();

            application.update(delta);
            application.render();
            glfwSwapBuffers(window);
        }
    }

    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        if (fullscreen) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer x = stack.mallocInt(1);
                IntBuffer y = stack.mallocInt(1);
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                glfwGetWindowPos(window, x, y);
                glfwGetWindowSize(window, width, height);
                windowedX = x.get(0);
                windowedY = y.get(0);
                windowedWidth = width.get(0);
                windowedHeight = height.get(0);
            }

            long monitor = glfwGetPrimaryMonitor();
            GLFWVidMode videoMode = monitor == 0 ? null : glfwGetVideoMode(monitor);
            if (videoMode == null) {
                fullscreen = false;
                return;
            }
            glfwSetWindowAttrib(window, GLFW_DECORATED, GLFW_FALSE);
            glfwSetWindowPos(window, 0, 0);
            glfwSetWindowSize(window, videoMode.width(), videoMode.height());
        } else {
            glfwSetWindowAttrib(window, GLFW_DECORATED, GLFW_TRUE);
            glfwSetWindowPos(window, windowedX, windowedY);
            glfwSetWindowSize(window, windowedWidth, windowedHeight);
        }

        Game.setWindowIcon(window, configuration.windowIcons());
        updateFramebufferSize();
        glViewport(0, 0, framebufferWidth, framebufferHeight);
        if (uiManager != null) uiManager.resize(framebufferWidth, framebufferHeight);
        Frontend.resize(framebufferWidth, framebufferHeight);
        application.onResize(framebufferWidth, framebufferHeight);
        repositionProgressBar();
    }

    private void repositionProgressBar() {
        if (progressBar == null) return;
        progressBar.setPosition((framebufferWidth - 500.0f) / 2.0f,
                (framebufferHeight - 25.0f) / 2.0f);
    }

    private void updateFramebufferSize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            glfwGetFramebufferSize(window, width, height);
            framebufferWidth = width.get(0);
            framebufferHeight = height.get(0);
        }
    }
}
