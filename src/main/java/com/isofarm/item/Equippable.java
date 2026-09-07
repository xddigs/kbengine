package com.isofarm.item;

import com.isofarm.data.DataClass;

/**
 * Defines the equippable contract.
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
