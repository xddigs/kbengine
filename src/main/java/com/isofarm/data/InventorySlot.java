package com.isofarm.data;

import com.isofarm.item.Item;

/**
 * Represents the inventory slot component of the Isofarm runtime.
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
@DataClass
public class InventorySlot {
    private Item item;
    private int amount;

    /**
     * Creates a new {@code InventorySlot} instance.
     */
    public InventorySlot() {
        this.item = null;
        this.amount = 0;
    }

    /**
     * Creates a new {@code InventorySlot} instance.
     * @param item the {@link Item} supplied as {@code item}
     */
    public InventorySlot(Item item) {
        this(item, 1);
    }

    /**
     * Creates a new {@code InventorySlot} instance.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} supplied as {@code amount}
     */
    public InventorySlot(Item item, int amount) {
        if (item == null || amount <= 0) {
            this.item = null;
            this.amount = 0;
            return;
        }

        this.item = item;
        this.amount = amount;
    }

    /**
     * Returns the item.
     * @return the {@link Item} representing the item
     */
    public Item getItem() {
        return item;
    }

    /**
     * Sets the item.
     * @param item the {@link Item} supplied as {@code item}
     */
    public void setItem(Item item) {
        this.item = item;

        if (item == null) {
            this.amount = 0;
        } else if (this.amount <= 0) {
            this.amount = 1;
        }
    }

    /**
     * Returns the amount.
     * @return {@code int}; the amount
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Sets the amount.
     * @param amount the {@code int} supplied as {@code amount}
     */
    public void setAmount(int amount) {
        if (item == null || amount <= 0) {
            clear();
            return;
        }

        this.amount = amount;
    }

    /**
     * Adds the amount.
     * @param amount the {@code int} supplied as {@code amount}
     */
    public void addAmount(int amount) {
        setAmount(this.amount + amount);
    }

    /**
     * Checks whether the empty condition is met.
     * @return {@code true} if empty; otherwise {@code false}
     */
    public boolean isEmpty() {
        return item == null || amount <= 0;
    }

    /**
     * Removes clear.
     */
    public void clear() {
        item = null;
        amount = 0;
    }
}