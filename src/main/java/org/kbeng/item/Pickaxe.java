package org.kbeng.item;

import org.kbeng.data.Enchantment;
import org.kbeng.data.Tier;
import org.kbeng.data.ToolType;

/**
 * Represents the pickaxe component of the Isofarm runtime.
 *
 * <p>This type centralizes the state, lifecycle and behavior required by its callers,
 * keeping domain rules together with the data they operate on.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li><b>State:</b> Owns the data needed to represent the component consistently.</li>
 *   <li><b>Lifecycle:</b> Exposes the operations used to create, update and release its state.</li>
 *   <li><b>Integration:</b> Coordinates with the surrounding game, rendering or UI systems through its public API.</li>
 * </ul>
 *
 * <p>Callers should use the documented public operations and allow this type to preserve
 * its invariants rather than modifying implementation details directly.
 */
public class Pickaxe extends Tool {

    /**
     * Creates a new {@code Pickaxe} instance.
     * @param tier the {@link Tier} supplied as {@code tier}
     */
    public Pickaxe(Tier tier) {
        super((byte) 1, ToolType.PICKAXE.getName(), 100, ToolType.PICKAXE,
                tier, tier.getDurability() + ToolType.PICKAXE.getBaseDurability());
    }

    /**
     * Creates a new {@code Pickaxe} instance.
     */
    public Pickaxe() {
        this(Tier.WOODEN);
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Pickaxe(getTier());
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
