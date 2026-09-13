package org.kbeng.games.rpg.data;

import org.kbeng.games.rpg.item.Item;

/**
 * InventorySlot provides inventory slot capabilities within the data subsystem.
 * It represents strongly typed domain data shared between simulation, rendering, input, and persistence boundaries.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
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
