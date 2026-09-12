package org.kbeng.graphics;

import org.kbeng.Game;
import org.kbeng.entity.Player;
import org.kbeng.input.ControlAction;
import org.kbeng.input.Controls;
import org.kbeng.input.Keyboard;
import org.kbeng.input.Mouse;
import org.kbeng.service.BookService;
import org.kbeng.service.TimeService;
import org.kbeng.ui.*;
import org.kbeng.utils.K;
import org.kbeng.utils.Local;
import org.kbeng.utils.Settings;
import org.kbeng.utils.ToastFactory;
import org.kbeng.wrld.GameMaster;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Intro provides intro capabilities within the graphics subsystem.
 * It contributes to OpenGL resource ownership, render-state setup, or frame-pipeline execution.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public class Intro {
    private static final float CLEAR_COLOR_ALPHA = 1.0f;
    private static final long LOADING_FRAME_INTERVAL_NANOS = 100_000_000L;
    private static final String SPLASH_LOGO_PATH = K.Paths.LOGO;
    private static final float SPLASH_DURATION_SECONDS = 4.0f;
    private static final float SPLASH_FADE_DURATION_SECONDS = 1.5f;
    private static final float SPLASH_MAX_WIDTH = 900.0f;
    private static long window;
    private static UIManager uiManager;
    private UIProgressBar progressBar;
    private UILabel namePrompt;
    private UITextField nameField;
    private int framebufferWidth;
    private int framebufferHeight;
    private long lastLoadingFrameNanos;

    private boolean isFullScreen = false;

    private int windowedX;
    private int windowedY;
    private int windowedWidth;
    private int windowedHeight;

    /**
     * Creates a new {@code Intro} instance.
     * @param window the {@code long} supplied as {@code window}
     */
    public Intro(long window) {
        Intro.window = window;
    }

    /**
     * Returns the window.
     * @return {@code long}; the window
     */
    public static long getWindow() {
        return window;
    }

    /**
     * Returns the ui manager.
     * @return the {@link UIManager} representing the ui manager
     */
    public static UIManager getUiManager() {
        return uiManager;
    }

    /**
     * Sets setup ui.
     */
    public void setupUI() {
        updateFramebufferSize();
        float barWidth = 500f;
        float barHeight = 25f;

        float x = (framebufferWidth - barWidth) / 2.0f;
        float y = (framebufferHeight - barHeight) / 2.0f;

        Vector4f foreground = new Vector4f(0.0f, 0.90f, 0.4f, 1.0f);
        Vector4f background = new Vector4f(0.2f, 0.2f, 0.2f, 1.0f);
        progressBar = new UIProgressBar(x, y, barWidth, barHeight, 0.0f, 100.0f, false);
        uiManager = new UIManager(framebufferWidth, framebufferHeight);
        progressBar.setColors(foreground, background);
        progressBar.show();
        uiManager.getRoot().show();
        uiManager.getRoot().addChild(progressBar);

        namePrompt = new UILabel(0.0f, 0.0f, 360.0f, 30.0f,
                Local.lang.t("intro.who_are_you"));
        namePrompt.setHorizontalAlignment(UILabel.HorizontalAlignment.CENTER);
        namePrompt.hide();
        uiManager.getRoot().addChild(namePrompt);

        nameField = new UITextField(0.0f, 0.0f, 360.0f, 40.0f);
        nameField.setMaxLength(24);
        nameField.hide();
        uiManager.getRoot().addChild(nameField);

        repositionIntroElements();
        uiManager.resize(framebufferWidth, framebufferHeight);
    }

    /**
     * Activates this object and prepares any state it requires.
     */
    public void show() {
        setupUI();
        toggleFullscreen();
        updateFramebufferSize();
        GameMaster.game.onResize(framebufferWidth, framebufferHeight);
        setupCallbacks();
        showSplashScreen();

        int r = Settings.getRenderDistance();
        int totalChunks = countVisibleChunks(r);
        int resourceSteps = 10;
        int postProcessingSteps = 2;
        int totalTasks = resourceSteps + (totalChunks * 2) + postProcessingSteps;

        final int[] completedTasks = {0};

        GameMaster.game.loadResources(progress -> {
            completedTasks[0]++;
            float overallProgress = ((float) completedTasks[0] / totalTasks) * 100.0f;
            progressBar.setValue(overallProgress);
            renderLoadingProgress(Local.lang.t("engine.loading"));
        });

        for (int currentChunkX = -r; currentChunkX <= r; currentChunkX++) {
            for (int currentChunkZ = -r; currentChunkZ <= r; currentChunkZ++) {
                if (!isChunkVisible(currentChunkX, currentChunkZ, r)) {
                    continue;
                }

                glfwPollEvents();
                if (glfwWindowShouldClose(window)) {
                    return;
                }

                GameMaster.game.getChunkManager().getGenerator()
                        .generateChunk(currentChunkX, currentChunkZ);

                completedTasks[0]++;
                float overallProgress = ((float) completedTasks[0] / totalTasks) * 100.0f;
                progressBar.setValue(overallProgress);

                String stepText = String.format(Local.lang.f("engine.generating_terrain",
                        currentChunkX, currentChunkZ));
                renderLoadingProgress(stepText);
            }
        }

        for (int currentChunkX = -r; currentChunkX <= r; currentChunkX++) {
            for (int currentChunkZ = -r; currentChunkZ <= r; currentChunkZ++) {
                if (!isChunkVisible(currentChunkX, currentChunkZ, r)) {
                    continue;
                }

                glfwPollEvents();
                if (glfwWindowShouldClose(window)) {
                    return;
                }

                GameMaster.game.getChunkManager()
                        .buildSingleChunkMesh(currentChunkX, currentChunkZ);
                completedTasks[0]++;
                float overallProgress = ((float) completedTasks[0] / totalTasks) * 100.0f;
                progressBar.setValue(overallProgress);

                String stepText = String.format(Local.lang.f("engine.building_meshes",
                        currentChunkX, currentChunkZ));
                renderLoadingProgress(stepText);
            }
        }

        GameMaster.game.getChunkManager().setLastPlayerChunkX(0);
        GameMaster.game.getChunkManager().setLastPlayerChunkZ(0);

        GameMaster.game.spawn();
        completedTasks[0]++;
        progressBar.setValue(((float) completedTasks[0] / totalTasks) * 100.0f);
        renderLoadingFrame(Local.lang.t("engine.spawning_player"));

        GameMaster.game.initUI();
        GameUIService.ui.getHotbarUI().hide();
        completedTasks[0]++;
        progressBar.setValue(100.0f);
        renderLoadingFrame(Local.lang.t("engine.post_processing"));

        progressBar.hide();
        requestPlayerName();
        loop();
    }

    /**
     * Returns whether a chunk offset is inside the visible circular render range.
     * @param chunkX the chunk x offset from spawn
     * @param chunkZ the chunk z offset from spawn
     * @param renderDistance the active render distance in chunks
     * @return {@code true} when the chunk is visible; otherwise {@code false}
     */
    private boolean isChunkVisible(int chunkX, int chunkZ, int renderDistance) {
        int radiusSquared = renderDistance * renderDistance;
        return chunkX * chunkX + chunkZ * chunkZ <= radiusSquared;
    }

    /**
     * Counts visible chunks in the circular render range centered at spawn.
     * @param renderDistance the active render distance in chunks
     * @return the number of chunks that will be generated and meshed at startup
     */
    private int countVisibleChunks(int renderDistance) {
        int visibleChunks = 0;
        for (int chunkX = -renderDistance; chunkX <= renderDistance; chunkX++) {
            for (int chunkZ = -renderDistance; chunkZ <= renderDistance; chunkZ++) {
                if (isChunkVisible(chunkX, chunkZ, renderDistance)) {
                    visibleChunks++;
                }
            }
        }
        return visibleChunks;
    }

    /**
     * Displays the game logo before loading begins.
     */
    private void showSplashScreen() {
        Texture logo = new Texture(SPLASH_LOGO_PATH);
        double startTime = glfwGetTime();

        try {
            while (!glfwWindowShouldClose(window)) {
                glfwPollEvents();
                float elapsed = (float) (glfwGetTime() - startTime);
                if (elapsed >= SPLASH_DURATION_SECONDS) {
                    return;
                }

                renderSplashFrame(logo, elapsed);
            }
        } finally {
            logo.dispose();
        }
    }

    /**
     * Renders one frame of the animated splash screen.
     * @param logo the logo texture to render
     * @param elapsed the elapsed animation time in seconds
     */
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

    /**
     * Smoothly interpolates a normalized animation value.
     * @param value the normalized value to interpolate
     * @return the smoothed value
     */
    private float smoothStep(float value) {
        return value * value * (3.0f - 2.0f * value);
    }

    /**
     * Refreshes loading progress without tying every loading operation to a
     * buffer swap. Buffer swaps may block on VSync, so the loading UI is capped
     * independently while resource generation continues at full speed.
     * @param statusText the loading status to display
     */
    private void renderLoadingProgress(String statusText) {
        long now = System.nanoTime();
        if (lastLoadingFrameNanos != 0L
                && now - lastLoadingFrameNanos < LOADING_FRAME_INTERVAL_NANOS) {
            return;
        }

        renderLoadingFrame(statusText);
        lastLoadingFrameNanos = System.nanoTime();
    }

    /**
     * Waits for the player to enter a non-empty name after loading.
     */
    private void requestPlayerName() {
        namePrompt.show();
        nameField.show();
        uiManager.setFocusedElement(nameField);
        Keyboard.update();

        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            renderLoadingFrame(null);

            boolean submitted = Keyboard.isKeyPressed(Keyboard.KEY_ENTER)
                    || Keyboard.isKeyPressed(Keyboard.KEY_KP_ENTER);
            String playerName = nameField.getText().trim();
            if (submitted && !playerName.isEmpty()) {
                Player.plyr.setName(playerName);
                ToastFactory.info(Local.lang.f("toast.open_inventory", playerName));
                Mouse.update();
                Keyboard.update();
                break;
            }

            Mouse.update();
            Keyboard.update();
        }

        uiManager.clearFocus();
        namePrompt.hide();
        nameField.hide();
    }

    /**
     * Renders the loading frame.
     * @param statusText the {@link String} supplied as {@code statusText}
     */
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

        if (statusText != null) {
            float textX = (framebufferWidth - 500f) / 2.0f;
            float textY = ((framebufferHeight - 25f) / 2.0f) - 30.0f;
            UIFont font = Frontend.getNormalFont();
            Frontend.drawString(statusText, textX, textY, font, K.UI.UI_TEXT_COLOR);
        }

        Frontend.end();
        glfwSwapBuffers(window);
        glFlush();
    }

    /**
     * Sets setup callbacks.
     */
    public void setupCallbacks() {
        glfwSetFramebufferSizeCallback(window, (windowHandle, width, height) -> {
            if (width <= 0 || height <= 0) {
                return;
            }

            framebufferWidth = width;
            framebufferHeight = height;

            glViewport(0, 0, width, height);

            if (uiManager != null) {
                uiManager.resize(width, height);
                Frontend.resize(width, height);
            }

            if (GameMaster.game != null) {
                GameMaster.game.onResize(width, height);
            }

            repositionIntroElements();
        });
    }

    /**
     * Transforms progress bar according to the supplied values.
     */
    private void repositionProgressBar() {
        if (progressBar == null) return;

        float barWidth = 500f;
        float barHeight = 25f;

        float x = (framebufferWidth - barWidth) / 2.0f;
        float y = (framebufferHeight - barHeight) / 2.0f;

        progressBar.setPosition(x, y);
    }

    /**
     * Keeps the loading and player-name controls centered after a resize.
     */
    private void repositionIntroElements() {
        repositionProgressBar();
        if (namePrompt == null || nameField == null) return;

        float fieldWidth = nameField.getWidth();
        float centerX = (framebufferWidth - fieldWidth) / 2.0f;
        float centerY = (framebufferHeight - nameField.getHeight()) / 2.0f;
        namePrompt.setPosition(centerX, centerY - namePrompt.getHeight() - 12.0f);
        nameField.setPosition(centerX, centerY);
    }

    /**
     * Processes each applicable element for loop.
     */
    private void loop() {
        GameUIService.ui.getHotbarUI().show();
        double lastTime = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double currentTime = glfwGetTime();
            float delta = (float) (currentTime - lastTime);
            lastTime = currentTime;

            glfwPollEvents();

            if (Controls.isDown(ControlAction.SMART_SHIFT) &&
                    Controls.isPressed(ControlAction.QUIT)) {
                GameMaster.game.getChunkManager().shutdown();
                glfwSetWindowShouldClose(window, true);
            }

            if (Controls.isPressed(ControlAction.CHANGE_LANGUAGE)) {
                Local.lang.nextLanguage();
                if (BookService.bs.isOpen() && BookService.bs.getOpenedBook() != null) {
                    BookUI.bui.reload(BookService.bs.getOpenedBook());
                }
                ToastFactory.reload();
                ToastFactory.success(Local.lang.f("engine.language_changed",
                        Local.lang.getCurrentLanguage().getName()));
            }

            if (Controls.isPressed(ControlAction.SHOW_LANGUAGE)) {
                ToastFactory.info(Local.lang.f("engine.current_language",
                        Local.lang.getCurrentLanguage().getName()));
            }

            if (Controls.isPressed(ControlAction.TOGGLE_FULLSCREEN)) {
                toggleFullscreen();
            }

            Vector3f skyColor = TimeService.getSkyColor();
            glClearColor(skyColor.x, skyColor.y, skyColor.z, CLEAR_COLOR_ALPHA);
            GameMaster.game.update(delta);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glEnable(GL_DEPTH_TEST);
            GameMaster.game.render();
            glfwSwapBuffers(window);
        }
    }

    /**
     * Toggles the setting represented by fullscreen and applies it immediately.
     */
    private void toggleFullscreen() {
        isFullScreen = !isFullScreen;
        if (isFullScreen) {
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

            if (monitor == 0) {
                isFullScreen = false;
                return;
            }

            GLFWVidMode videoMode = glfwGetVideoMode(monitor);
            if (videoMode == null) {
                isFullScreen = false;
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

        Game.setWindowIcon(window);
        updateFramebufferSize();
        glViewport(0, 0, framebufferWidth, framebufferHeight);
        if (uiManager != null) {
            uiManager.resize(framebufferWidth, framebufferHeight);
        }

        Frontend.resize(framebufferWidth, framebufferHeight);
        if (GameMaster.game != null) {
            GameMaster.game.onResize(framebufferWidth, framebufferHeight);
        }

        repositionIntroElements();
    }

    /**
     * Updates the framebuffer size.
     */
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
