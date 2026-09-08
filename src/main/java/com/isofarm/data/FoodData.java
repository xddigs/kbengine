package com.isofarm.data;

import com.isofarm.utils.Local;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Available food types in the game
 */
@DataClass
public enum FoodData {
    BREAD((byte) 0, (byte) 0, (byte) 0, 4.0f, 25),
    CARROT_CAKE((byte) 1, (byte) 1, (byte) 0, 6.0f, 25),
    FRIES((byte) 2, (byte) 0, (byte) 2, 5.0f, 25),
    POTATO_CAKE((byte) 3, (byte) 3, (byte) 0, 6.0f, 25);

    private final byte id;
    private final byte col;
    private final byte row;
    private final float foodValue;
    private final int value;

    FoodData(byte id, byte col, byte row, float foodValue, int value) {
        this.id = id;
        this.col = col;
        this.row = row;
        this.foodValue = foodValue;
        this.value = value;
    }

    /**
     * Returns the {@code id} value
     * @return {@link byte} value of id
     */
    public byte getId() {
        return id;
    }

    /**
     * Returns the {@code col} value
     * @return {@link byte} value of col
     */
    public byte getCol() {
        return col;
    }

    /**
     * Returns the {@code row} value
     * @return {@link byte} value of row
     */
    public byte getRow() {
        return row;
    }

    /**
     * Returns the name
     * @return {@link String} name of food
     */
    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the display name
     * @return {@link String} display name of food
     */
    public String getDisplayName() {
        return Local.lang.t("item.food." + getName());
    }

    /**
     * Returns the {@code foodValue} value
     * @return {@link float} value of foodValue
     */
    public float getFoodValue() {
        return foodValue;
    }

    /**
     * Returns the {@code value} value
     * @return {@link int} value of value
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns the food from the id
     * @param id the id of the food
     * @return the food {@link FoodData} from the id {@code id}
     */
    public static FoodData fromId(byte id) {
        for (FoodData food : values()) {
            if (food.getId() == id) {
                return food;
            }
        }
        return null;
    }

    /**
     * Performs the given action for each food in the enum
     * @param consumer the action to be performed for each food
     */
    public static void forEach(Consumer<FoodData> consumer) {
        for (FoodData food : values()) {
            consumer.accept(food);
        }
    }
}
