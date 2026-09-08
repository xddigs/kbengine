package com.isofarm.item;

import com.isofarm.data.Enchantment;
import com.isofarm.data.Tier;
import com.isofarm.data.ToolType;

/**
 * Encapsulates the state and operations required by Shield within the game runtime.
 */
public class Shield extends Tool implements Equippable {

    /**
     * Creates a new {@code Shield} instance.
     * @param tier the {@link Tier} supplied as {@code tier}
     */
    public Shield(Tier tier) {
        super((byte) 5, ToolType.SHIELD.getName(), 150, ToolType.SHIELD,
                tier, tier.getDurability() + ToolType.SHIELD.getBaseDurability());
    }

    /**
     * Creates a new {@code Shield} instance.
     */
    public Shield() {
        this(Tier.WOODEN);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equip() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean unequip() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEquipped() {
        return false;
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Shield(getTier());
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
