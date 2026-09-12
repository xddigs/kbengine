package org.kbeng.rpg.item;

import org.kbeng.rpg.data.Enchantment;
import org.kbeng.rpg.data.Tier;
import org.kbeng.rpg.data.ToolType;

/**
 * Shovel provides shovel capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Tool, inheriting shared behavior while specializing subsystem-specific logic.
 */
public class Shovel extends Tool {

    /**
     * Creates a new {@code Shovel} instance.
     * @param tier the {@link Tier} supplied as {@code tier}
     */
    public Shovel(Tier tier) {
        super((byte) 4, ToolType.SHOVEL.getName(), 50, ToolType.SHOVEL,
                tier, tier.getDurability() + ToolType.SHOVEL.getBaseDurability());
    }

    /**
     * Creates a new {@code Shovel} instance.
     */
    public Shovel() {
        this(Tier.WOODEN);
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Shovel(getTier());
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
