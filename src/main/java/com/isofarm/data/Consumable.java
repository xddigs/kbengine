package com.isofarm.data;

import com.isofarm.item.Item;

/**
 * Defines the consumable contract, whether an item can be consumed.
 */
@DataClass
public interface Consumable {
    /**
     * Consumes the item
     * @return {@code true} if the item was consumed, {@code false} otherwise
     */
    boolean consume();
}
