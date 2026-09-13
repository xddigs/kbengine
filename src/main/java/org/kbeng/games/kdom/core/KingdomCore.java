package org.kbeng.games.kdom.core;

import org.kbeng.engine.graphics.Application;
import org.kbeng.engine.utils.K;

import java.util.List;
import java.util.function.Consumer;

/**
 * KingdomCore is the entry point for the Kingdom application.
 * Boots the application and initializes the application lifecycle.
 */
public class KingdomCore implements Application {

    @Override
    public Configuration configuration() {
        return new Configuration("Kingdom", (int) K.Window.DEFAULT_WIDTH,
                (int) K.Window.DEFAULT_HEIGHT, true, K.Paths.LOGO,
                K.Paths.CURSOR_POINTER, List.of(
                new WindowIcon(16, "/assets/ui/iconx16.png"),
                new WindowIcon(32, "/assets/ui/iconx32.png"),
                new WindowIcon(64, "/assets/ui/iconx64.png"),
                new WindowIcon(128, "/assets/ui/iconx128.png"),
                new WindowIcon(256, "/assets/ui/iconx256.png")));
    }

    @Override
    public void initialize(Context context,
                           Consumer<LoadingProgress> progressCallback) {

    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void render() {

    }

    @Override
    public void onResize(int width, int height) {

    }

    @Override
    public void dispose() {

    }
}
