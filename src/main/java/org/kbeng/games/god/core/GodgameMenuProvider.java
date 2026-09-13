package org.kbeng.games.god.core;

import org.kbeng.engine.graphics.Game;
import org.kbeng.engine.utils.Menu;
import org.kbeng.engine.utils.MenuProvider;

/** Registers Kingdom in the terminal launcher. */
public final class GodgameMenuProvider implements MenuProvider {
    @Override
    public void configure(Menu menu) {
        menu.option("Godgame", () -> new Game(new Godgame()).run());
    }

    @Override
    public int order() {
        return 20;
    }
}
