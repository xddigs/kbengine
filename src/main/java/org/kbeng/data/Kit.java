package org.kbeng.data;

import org.kbeng.item.Item;

/**
 * Kit provides kit capabilities within the data subsystem.
 *
 * It represents strongly typed domain data shared between simulation, rendering, input, and persistence boundaries.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
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
