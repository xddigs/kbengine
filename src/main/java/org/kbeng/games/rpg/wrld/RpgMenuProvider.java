package org.kbeng.games.rpg.wrld;

import org.kbeng.engine.graphics.Game;
import org.kbeng.engine.utils.Menu;
import org.kbeng.engine.utils.MenuProvider;

/** Registers the RPG application in the terminal launcher. */
public final class RpgMenuProvider implements MenuProvider {
    @Override
    public void configure(Menu menu) {
        menu.option("RPG", () -> new Game(new GameMaster()).run());
    }

    @Override
    public int order() {
        return 10;
    }
}
