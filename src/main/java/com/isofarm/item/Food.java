package com.isofarm.item;

import com.isofarm.data.Consumable;
import com.isofarm.data.DataClass;
import com.isofarm.data.FoodData;

/**
 * Represents a food item, which can be consumed.
 */
@DataClass
public record Food(FoodData type) implements Craftable,
        Consumable {
    /**
     * Creates a new {@code Food} instance.
     */
    public Food {}

    /**
     * Creates a new {@code Food} instance. Specifically a {@link FoodData#BREAD}
     */
    public Food() {
        this(FoodData.BREAD);
    }

    /** {@inheritDoc} */
    @Override
    public float getFoodValue() {
        return type.getFoodValue();
    }

    /**
     * Returns the id.
     * @return {@code byte}; the id
     */
    @Override
    public byte getId() {
        return type.getId();
    }

    /**
     * Returns the name.
     * @return the {@link String} representing the name
     */
    @Override
    public String getName() {
        return type.getName();
    }

    /**
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    @Override
    public String getDisplayName() {
        return type.getDisplayName();
    }

    /**
     * Returns the value.
     * @return {@code int}; the value
     */
    @Override
    public int getValue() {
        return type.getValue();
    }

    /**
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Food(type());
    }

    /**
     * Returns the {@code type} value
     * @return {@link FoodData} value of type
     */
    @Override
    public FoodData type() {
        return type;
    }
}
