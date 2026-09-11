package org.kbeng.item;

import org.kbeng.data.ArmorData;
import org.kbeng.data.Tier;
import org.kbeng.entity.Player;

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
