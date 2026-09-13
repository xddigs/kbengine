package org.kbeng.games.rpg.item;

import org.kbeng.games.rpg.data.Enchantment;
import org.kbeng.games.rpg.data.Tier;
import org.kbeng.games.rpg.data.ToolType;

/**
 * Sword provides sword capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Tool, inheriting shared behavior while specializing subsystem-specific logic.
 */
public class Sword extends Tool {

    /**
     * Creates a new {@code Sword} instance.
     * @param tier the {@link Tier} supplied as {@code tier}
     */
    public Sword(Tier tier) {
        super((byte) 0, ToolType.SWORD.getName(), 150, ToolType.SWORD,
                tier, tier.getDurability() + ToolType.SWORD.getBaseDurability());
    }

    /**
     * Creates a new {@code Sword} instance.
     */
    public Sword() {
        this(Tier.WOODEN);
    }
    
    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Sword(getTier());
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
