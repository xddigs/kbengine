package org.kbeng.games.rpg.item;

import org.kbeng.games.rpg.data.*;
import org.kbeng.engine.utils.Local;

/**
 * Material provides material capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It implements Craftable, Plantable, providing a concrete strategy for this subsystem contract.
 */
@DataClass
public class Material implements Craftable, Plantable {
    private final Tier tier;
    private final MaterialID materialID;
    private final boolean isSugarCane;

    /**
     * Creates a new {@code Material} instance.
     * @param tier the {@link Tier} supplied as {@code tier}
     * @param materialID the {@link MaterialID} supplied as {@code materialID}
     */
    public Material(Tier tier, MaterialID materialID) {
        this.tier = tier;
        this.materialID = materialID;
        this.isSugarCane = materialID.equals(MaterialID.SUGAR_CANE);
    }

    /**
     * Creates a new {@code Material} instance. Specifically, this constructor
     * creates a material with a tier of {@link Tier#NONE}.
     * @param materialID the {@link MaterialID} supplied as {@code materialID}
     */
    public Material(MaterialID materialID) {
        this(Tier.NONE, materialID);
    }

    /**
     * {@inheritDoc}
     * Returns the id.
     * @return {@code byte}; the id
     */
    @Override
    public byte getId() {
        return materialID.getId();
    }

    /**
     * {@inheritDoc}
     * Returns the name.
     * @return the {@link String} representing the name
     */
    @Override
    public String getName() {
        return materialID.getName();
    }

    /**
     * {@inheritDoc}
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    @Override
    public String getDisplayName() {
        return Local.lang.item(materialID.getDisplayName(),
                tier.getDisplayName());
    }

    /**
     * {@inheritDoc}
     * Returns the crop type, either {@code null} or {@code SUGAR_CANE}
     * @return the {@link CropType} result; {@code null} or {@code CropType.SUGAR_CANE}
     */
    @Override
    public CropType getType() {
        if (isSugarCane()) return CropType.SUGAR_CANE_CROP;
        return null;
    }

    /**
     * {@inheritDoc}
     * Returns the value.
     * @return {@code int}; the value
     */
    @Override
    public int getValue() {
        return materialID.getValue();
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Material(tier, materialID);
    }

    /**
     * Returns the tier.
     * @return the {@link Tier} representing the tier
     */
    public Tier getTier() {
        return tier;
    }

    /**
     * Returns the material id.
     * @return the {@link MaterialID} representing the material id
     */
    public MaterialID getMaterialID() {
        return materialID;
    }

    /**
     * Returns if the material is sugar cane.
     * @return {@code true} if sugar cane; otherwise {@code false}
     */
    public boolean isSugarCane() {
        return isSugarCane;
    }
}
