package org.kbeng.data;

import org.kbeng.utils.Local;

/**
 * Difficulty declares the canonical difficulty set for the data subsystem.
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public enum Difficulty {
    EASY((byte) 0, "difficulty.easy", 0.5f),
    NORMAL((byte) 1, "difficulty.normal", 1.0f),
    HARD((byte) 2, "difficulty.hard", 2.0f),
    NIGHTMARE((byte) 3, "difficulty.nightmare", 5.0f),;

    private final byte id;
    private final String name;
    private final float multiplier;

    /**
     * Creates a new {@code Difficulty} instance.
     * @param id the {@code byte} supplied as {@code id}
     * @param name the {@link String} supplied as {@code name}
     * @param multiplier the {@code float} supplied as {@code multiplier}
     */
    Difficulty(byte id, String name, float multiplier) {
        this.id = id;
        this.name = name;
        this.multiplier = multiplier;
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
        return Local.lang.t(name);
    }

    /**
     * Returns the multiplier.
     * @return {@code float}; the multiplier
     */
    public float getMultiplier() {
        return multiplier;
    }

    /**
     * Creates or returns from id from the supplied arguments.
     * @param id the {@code byte} supplied as {@code id}
     * @return the {@link Difficulty} representing the from id result
     */
    public static Difficulty fromId(byte id) {
        for (Difficulty difficulty : values()) {
            if (difficulty.getId() == id) {
                return difficulty;
            }
        }
        return null;
    }
}
