package org.kbeng.rpg.item;

import org.kbeng.rpg.data.DataClass;
import org.kbeng.rpg.data.MaterialID;
import org.kbeng.rpg.data.Tier;
import org.kbeng.engine.utils.Local;

/**
 * MiningComponent provides mining component capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Material and implements Craftable, combining inherited behavior with explicit runtime contracts.
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
