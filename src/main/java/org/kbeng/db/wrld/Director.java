package org.kbeng.db.wrld;

import org.kbeng.engine.graphics.Application;

import java.util.function.Consumer;

/**
 * Director class, acts as sole orchestrator for the application.
 */
public class Director implements Application {

    @Override
    public Configuration configuration() {
        return null;
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
