package org.kbeng.item;

import org.kbeng.data.ArmorData;
import org.kbeng.data.Tier;
import org.kbeng.entity.Player;

/**
 * Represents a helmet item for the {@link Player} to equip
 */
public class Helmet extends Armor {

    /** {@inheritDoc} */
    public Helmet(Tier tier) {
        super((byte) 0, tier, ArmorData.HELMET,
                ArmorData.HELMET.getDefense() + tier.getDefense());
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
        return new Helmet(getTier());
    }
}
