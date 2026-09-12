package org.kbeng.data;

import org.kbeng.item.*;

/**
 * Represents the starting kit component of the kbengine runtime.
 *
 * <p>This type centralizes the state, lifecycle and behavior required by its callers,
 * keeping domain rules together with the data they operate on.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li><b>State:</b> Owns the data needed to represent the component consistently.</li>
 *   <li><b>Lifecycle:</b> Exposes the operations used to create, update and release its state.</li>
 *   <li><b>Integration:</b> Coordinates with the surrounding game, rendering or UI systems through its public API.</li>
 * </ul>
 *
 * <p>Callers should use the documented public operations and allow this type to preserve
 * its invariants rather than modifying implementation details directly.
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
