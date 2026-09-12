package org.kbeng.item;

import org.kbeng.data.DataClass;

/**
 * Item defines the item contract within the item subsystem.
 * Implementations are expected to preserve these behavioral guarantees while choosing their own storage and execution strategy.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public interface Item {
    /**
     * Returns the id.
     * @return {@code byte}; the id
     */
    byte getId();
    /**
     * Returns the name.
     * @return the {@link String} representing the name
     */
    String getName();
    /**
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    String getDisplayName();
    /**
     * Returns the value.
     * @return {@code int}; the value
     */
    int getValue();
    /**
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    Item copy();
}