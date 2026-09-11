package org.kbeng.item;

import org.kbeng.data.Enchantment;
import org.kbeng.data.Tier;
import org.kbeng.data.ToolType;

/**
 * Represents the shovel component of the Isofarm runtime.
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
public class Shovel extends Tool {

    /**
     * Creates a new {@code Shovel} instance.
     * @param tier the {@link Tier} supplied as {@code tier}
     */
    public Shovel(Tier tier) {
        super((byte) 4, ToolType.SHOVEL.getName(), 50, ToolType.SHOVEL,
                tier, tier.getDurability() + ToolType.SHOVEL.getBaseDurability());
    }

    /**
     * Creates a new {@code Shovel} instance.
     */
    public Shovel() {
        this(Tier.WOODEN);
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Shovel(getTier());
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
