package org.kbeng;

import org.kbeng.engine.graphics.Application;
import org.kbeng.engine.graphics.Game;
import org.kbeng.games.kdom.core.KingdomCore;

/**
 * Main is the entry point for the app.
 * Boots the application and initializes the application lifecycle.
 */
public final class Main {

    /**
     * Executes main as part of the application lifecycle.
     * @param ignoredArgs an array of {@link String} values supplied as {@code ignoredArgs}
     */
    public static void main(String[] ignoredArgs) {
        Application app = new KingdomCore();
        new Game(app).run();
    }
}
