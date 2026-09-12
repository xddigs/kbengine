package org.kbeng.data;

import org.kbeng.utils.Local;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * ArmorData declares the canonical armor data set for the data subsystem.
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 * The data type is used as a normalized schema for transport, persistence, and runtime inspection.
 */
@DataClass
public enum ArmorData {
    HELMET((byte) 0, (byte) 0, 2.0f, 10),
    CHESTPLATE((byte) 1, (byte) 1, 5.0f, 20),
    BOOTS((byte) 2, (byte) 2, 1.0f, 5);

    private final byte id;
    private final byte col;
    private final float defense;
    private final int value;

    ArmorData(byte id, byte col, float defense, int value) {
        this.id = id;
        this.col = col;
        this.defense = defense;
        this.value = value;
    }

    /**
     * Returns the name
     * @return the {@link String} representing the name
     */
    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the localized display name
     * @return the {@link String} representing the display name
     */
    public String getDisplayName() {
        return Local.lang.t("item.armor." + getName());
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
     * Returns the {@code defense} value
     * @return {@link float} value of defense
     */
    public float getDefense() {
        return defense;
    }

    /**
     * Returns the {@code value} value
     * @return {@link int} value of value
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns the armor from the id
     * @param id the {@code byte} supplied as {@code id}
     * @return the {@link ArmorData} representing the from id result
     */
    public static ArmorData fromId(byte id) {
        for (ArmorData armorData : ArmorData.values()) {
            if (armorData.getId() == id) {
                return armorData;
            }
        }
        return null;
    }

    /**
     * Returns the armor from the defense
     * @param defense the {@code float} supplied as {@code defense}
     * @return the {@link ArmorData} representing the from defense result
     */
    public static ArmorData fromDefense(float defense) {
        for (ArmorData armorData : ArmorData.values()) {
            if (armorData.getDefense() == defense) {
                return armorData;
            }
        }
        return null;
    }

    /**
     * Performs the given action for each armor in the enum
     * @param consumer the action to be performed for each armor
     */
    public static void forEach(Consumer<ArmorData> consumer) {
        for (ArmorData armorData : ArmorData.values()) {
            consumer.accept(armorData);
        }
    }
}
