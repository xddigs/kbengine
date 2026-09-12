package org.kbeng.item;

import org.kbeng.data.DataClass;
import org.kbeng.data.MaterialID;
import org.kbeng.data.Tier;
import org.kbeng.utils.Local;

/**
 * Represents the mining component component of the kbengine runtime.
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
@DataClass
public class MiningComponent extends Material implements Craftable {
    private final byte id;
    private final int value;

    /**
     * Creates a new {@code MiningComponent} instance.
     * @param tier the {@link Tier} supplied as {@code tier}
     * @param materialID the {@link MaterialID} supplied as {@code materialID}
     */
    public MiningComponent(Tier tier, MaterialID materialID) {
        super(tier, materialID);
        this.id = materialID.getId();
        this.value = materialID.getValue();
    }

    /**
     * {@inheritDoc}
     * Returns the id.
     * @return {@code byte}; the id
     */
    @Override
    public byte getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    @Override
    public String getDisplayName() {
        return Local.lang.item(getMaterialID().getDisplayName(),
                getTier().getDisplayName());
    }

    /**
     * {@inheritDoc}
     * Returns the value.
     * @return {@code int}; the value
     */
    @Override
    public int getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return this;
    }
}
