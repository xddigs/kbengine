package org.kbeng.games.rpg.item;

import org.kbeng.games.rpg.data.Enchantment;
import org.kbeng.games.rpg.data.Tier;
import org.kbeng.games.rpg.data.ToolType;

/**
 * Axe provides axe capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Tool, inheriting shared behavior while specializing subsystem-specific logic.
 */
public class Axe extends Tool {

    /**
     * Creates a new {@code Axe} instance.
     * @param tier the {@link Tier} supplied as {@code tier}
     */
    public Axe(Tier tier) {
        super((byte) 2, ToolType.AXE.getName(), 150, ToolType.AXE,
                tier, tier.getDurability() + ToolType.AXE.getBaseDurability());
    }

    /**
     * Creates a new {@code Axe} instance.
     */
    public Axe() {
        this(Tier.WOODEN);
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Axe(getTier());
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
