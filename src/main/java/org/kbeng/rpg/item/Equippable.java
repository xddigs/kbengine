package org.kbeng.rpg.item;

import org.kbeng.rpg.data.DataClass;

/**
 * Equippable defines the equippable contract within the item subsystem.
 * Implementations are expected to preserve these behavioral guarantees while choosing their own storage and execution strategy.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public interface Equippable {
    /**
     * Equips the item.
     * @return {@code boolean}; the equipping result
     */
    boolean equip();

    /**
     * Unequips the item.
     * @return {@code boolean}; the unequipping result
     */
    boolean unequip();

    /**
     * Checks whether the item is equipped.
     * @return {@code boolean}; the equipped condition
     */
    boolean isEquipped();
}
