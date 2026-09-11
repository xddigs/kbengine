package org.kbeng.data;

import org.kbeng.utils.Local;

/**
 * Enumerates the supported season values.
 */
public enum Season {
    WINTER((byte) 0, "Winter", 0.5),
    SPRING((byte) 1, "Spring", 2.0),
    SUMMER((byte) 2, "Summer", 1.0),
    AUTUMN((byte) 3, "Autumn", 1.5);

    private final byte id;
    private final String name;
    private final double valueMultiplier;

    /**
     * Creates a new {@code Season} instance.
     * @param id the {@code byte} supplied as {@code id}
     * @param name the {@link String} supplied as {@code name}
     * @param valueMultiplier the {@code double} supplied as {@code valueMultiplier}
     */
    Season(byte id, String name, double valueMultiplier) {
        this.id = id;
        this.name = name;
        this.valueMultiplier = valueMultiplier;
    }

    /**
     * Returns the id.
     * @return {@code byte}; the id
     */
    public byte getId() {
        return id;
    }

    /**
     * Returns the name.
     * @return the {@link String} representing the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    public String getDisplayName() {
        return Local.lang.t("season." + name().toLowerCase());
    }

    /**
     * Returns the value multiplier.
     * @return {@code double}; the value multiplier
     */
    public double getValueMultiplier() {
        return valueMultiplier;
    }

    /**
     * Creates or returns from id from the supplied arguments.
     * @param id the {@code byte} supplied as {@code id}
     * @return the {@link Season} representing the from id result
     */
    public static Season fromId(byte id) {
        for (Season season : Season.values()) {
            if (season.getId() == id) {
                return season;
            }
        }
        return SPRING;
    }
}
