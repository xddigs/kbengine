package org.kbeng.item;

import org.kbeng.data.ArmorData;
import org.kbeng.data.Tier;
import org.kbeng.entity.Player;

/**
 * Represents a Chestplate item for the {@link Player} to equip
 */
public class Boots extends Armor {

    /** {@inheritDoc} */
    public Boots(Tier tier) {
        super((byte) 2, tier, ArmorData.BOOTS,
                ArmorData.BOOTS.getDefense() + tier.getDefense());
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
        return new Boots(getTier());
    }
}
