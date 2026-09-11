package org.kbeng.item;

import org.kbeng.data.DataClass;

/**
 * Defines the item contract.
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