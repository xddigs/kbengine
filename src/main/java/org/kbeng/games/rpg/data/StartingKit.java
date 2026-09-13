package org.kbeng.games.rpg.data;

import org.kbeng.games.rpg.item.*;
import org.kbeng.rpg.item.*;

/**
 * StartingKit provides starting kit capabilities within the data subsystem.
 * It represents strongly typed domain data shared between simulation, rendering, input, and persistence boundaries.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Kit, inheriting shared behavior while specializing subsystem-specific logic.
 */
public class StartingKit extends Kit {
    /**
     * Creates a new {@code StartingKit} instance.
     */
    public StartingKit() {
        setItems(new Item[]{
                new Sword(Tier.WOODEN),
                new Pickaxe(Tier.WOODEN),
                new Axe(Tier.WOODEN),
                new Hoe(Tier.WOODEN),
                new Shovel(Tier.WOODEN),
                new Backpack()
        });
    }
}
