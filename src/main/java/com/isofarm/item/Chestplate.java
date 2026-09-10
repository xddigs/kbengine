package com.isofarm.item;

import com.isofarm.data.ArmorData;
import com.isofarm.data.Tier;
import com.isofarm.entity.Player;

/**
 * Represents a Chestplate item for the {@link Player} to equip
 */
public class Chestplate extends Armor {

    /** {@inheritDoc} */
    public Chestplate(Tier tier) {
        super((byte) 1, tier, ArmorData.CHESTPLATE,
                ArmorData.CHESTPLATE.getDefense() + tier.getDefense());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equip() {
        return super.equip();
    }

    /** {@inheritDoc} */
    @Override
    public boolean unequip() {
        return super.unequip();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEquipped() {
        return super.isEquipped();
    }

    /** {@inheritDoc} */
    @Override
    public Item copy() {
        return new Chestplate(getTier());
    }
}
