package org.kbeng.games.kdom.core;

import org.kbeng.engine.graphics.Game;
import org.kbeng.engine.utils.Menu;
import org.kbeng.engine.utils.MenuProvider;

/** Registers Kingdom in the terminal launcher. */
public final class KingdomMenuProvider implements MenuProvider {
    @Override
    public void configure(Menu menu) {
        menu.option("Kingdom", () -> new Game(new KingdomCore()).run());
    }

    @Override
    public int order() {
        return 20;
    }
}
