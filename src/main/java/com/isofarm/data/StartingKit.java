package com.isofarm.data;

import com.isofarm.item.*;

/**
 * Encapsulates the state and operations required by starting kit within the game runtime.
 */
public class StartingKit extends Kit {
    /**
     * Creates a new {@code StartingKit} instance.
     */
    public StartingKit() {
        setItems(new Item[]{
                new Backpack(),
        });
    }
}
