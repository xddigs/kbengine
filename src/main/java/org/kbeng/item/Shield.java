package org.kbeng.item;

import org.kbeng.data.Enchantment;
import org.kbeng.data.Tier;
import org.kbeng.data.ToolType;
import org.kbeng.entity.Player;

/**
 * Shield provides shield capabilities within the item subsystem.
 *
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 *
 * It extends Tool and implements Equippable, combining inherited behavior with explicit runtime contracts.
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
        return Player.plyr.getInventory().equipShield(this);
    }

    /** {@inheritDoc} */
    @Override
    public boolean unequip() {
        return isEquipped() && Player.plyr.getInventory().unequipShield();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEquipped() {
        return Player.plyr.getInventory().getShield() == this;
    }

    /** Returns how many incoming damage points this shield can absorb per hit. */
    public float getDefense() {
        return getType().getBaseDefense();
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
