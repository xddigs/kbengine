package org.kbeng.item;

import org.kbeng.data.Enchantment;

/**
 * Enchantable defines the enchantable contract within the item subsystem.
 *
 * Implementations are expected to preserve these behavioral guarantees while choosing their own storage and execution strategy.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@FunctionalInterface
public interface Enchantable {
    /**
     * Applies enchanting and updates the affected character or item state.
     * @param enchantment the {@link Enchantment} supplied as {@code enchantment}
     * @return {@code boolean}; the enchanting result
     */
    boolean enchanting(Enchantment enchantment);
}
