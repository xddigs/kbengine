package org.kbeng.engine.graphics;

import org.kbeng.engine.ui.UIManager;

import java.util.List;
import java.util.function.Consumer;

/**
 * Application hosted by the engine runtime.
 * <p>The engine owns the window, intro and frame loop. Implementations own
 * their resources, startup flow, simulation and rendering.</p>
 */
public interface Application {
    /** Describes the window and splash without coupling the host to a game. */
    Configuration configuration();

    /** Loads the application after the OpenGL context and intro UI exist. */
    void initialize(Context context, Consumer<LoadingProgress> progressCallback);

    /** Runs application-specific startup UI after loading has completed. */
    default void start() { }

    /** Advances the application by one frame. */
    void update(float delta);

    /** Clears and renders the application frame. */
    void render();

    /** Receives framebuffer-size changes. */
    void onResize(int width, int height);

    /** Releases every resource owned by the application. */
    void dispose();

    record Context(long windowHandle, UIManager uiManager,
                   int framebufferWidth, int framebufferHeight) { }

    record LoadingProgress(float progress, String statusText) {
        public LoadingProgress {
            progress = Math.clamp(progress, 0.0f, 1.0f);
        }
    }

    record WindowIcon(int size, String resourcePath) { }

    record Configuration(String windowTitle, int initialWidth, int initialHeight,
                         boolean startFullscreen, String splashLogoPath,
                         String cursorImagePath,
                         List<WindowIcon> windowIcons) {
        public Configuration {
            windowIcons = windowIcons == null ? List.of() : List.copyOf(windowIcons);
        }
    }
}
