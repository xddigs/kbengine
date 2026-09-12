package org.kbeng.item;

import org.kbeng.data.Enchantment;
import org.kbeng.data.Tier;
import org.kbeng.data.ToolType;

/**
 * Pickaxe provides pickaxe capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Tool, inheriting shared behavior while specializing subsystem-specific logic.
 */
public class Pickaxe extends Tool {

    /**
     * Creates a new {@code Pickaxe} instance.
     * @param tier the {@link Tier} supplied as {@code tier}
     */
    public Pickaxe(Tier tier) {
        super((byte) 1, ToolType.PICKAXE.getName(), 100, ToolType.PICKAXE,
                tier, tier.getDurability() + ToolType.PICKAXE.getBaseDurability());
    }

    /**
     * Creates a new {@code Pickaxe} instance.
     */
    public Pickaxe() {
        this(Tier.WOODEN);
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Pickaxe(getTier());
    }

    /**
     * {@inheritDoc}
     * Applies enchanting and updates the affected character or item state.
     * @param enchantment the {@link Enchantment} supplied as {@code enchantment}
     * @return {@code boolean}; the enchanting result
     */
    @Override
    public boolean enchanting(Enchantment enchantment) {
        return false;
    }
}
