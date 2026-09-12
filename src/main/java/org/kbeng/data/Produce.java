package org.kbeng.data;

import org.kbeng.item.Craftable;
import org.kbeng.item.Item;

import java.util.Objects;

/**
 * Produce provides produce capabilities within the data subsystem.
 * It represents strongly typed domain data shared between simulation, rendering, input, and persistence boundaries.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It implements Craftable, Consumable, providing a concrete strategy for this subsystem contract.
 */
@DataClass
public class Produce implements Craftable, Consumable {
    private final byte id;
    private final String name;
    private final String displayName;
    private final int value;
    private final CropType type;

    /**
     * Creates a new {@code Produce} instance.
     * @param type the {@link CropType} supplied as {@code type}
     */
    public Produce(CropType type) {
        this.type = type;
        this.id = type.getId();
        this.name = type.getName();
        this.displayName = type.getDisplayName();
        this.value = type.getValue();
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
     * Returns the name.
     * @return the {@link String} representing the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    @Override
    public String getDisplayName() {
        return displayName;
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

    /** {@inheritDoc} */
    @Override
    public float getFoodValue() {
        return switch (type) {
            case CARROT, BEETROOT -> 2.0f;
            case POTATO, SUGAR_CANE_CROP -> 1.0f;
            case WHEAT -> 0.0f;
        };
    }

    /**
     * Returns the type.
     * @return the {@link CropType} representing the type
     */
    public CropType getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     * Determines whether equals satisfies the required comparison or validity rules.
     * @param o the {@link Object} supplied as {@code o}
     * @return {@code boolean}; the equals result
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Produce produce)) return false;
        return type == produce.type;
    }

    /**
     * {@inheritDoc}
     * Checks whether hash code.
     * @return {@code int}; the hash code result
     */
    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Produce(type);
    }
}
