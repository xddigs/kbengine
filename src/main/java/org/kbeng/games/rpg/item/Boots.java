package org.kbeng.games.rpg.item;

import org.kbeng.games.rpg.data.ArmorData;
import org.kbeng.games.rpg.data.Tier;

/**
 * Boots provides boots capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Armor, inheriting shared behavior while specializing subsystem-specific logic.
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
