package com.isofarm.data;

import com.isofarm.item.*;

/**
 * Represents the kit component of the Isofarm runtime.
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
public abstract class Kit {
    private Item[] items;

    /**
     * Returns the items.
     * @return an array of {@link Item} values; the items
     */
    public Item[] getItems() {
        return items;
    }

    /**
     * Sets the items.
     * @param items an array of {@link Item} values supplied as {@code items}
     */
    public void setItems(Item[] items) {
        this.items = items;
    }
}
