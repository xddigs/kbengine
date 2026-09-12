package org.kbeng.data;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Tier declares the canonical tier set for the data subsystem.
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public enum Tier {
    NONE((byte) -1, 0.0f, 0),
    LEATHER((byte) 0, 1.0f, 1),
    WOODEN((byte) 0, 0.0f, 64),
    STONE((byte) 1, 3.0f, 128),
    COPPER((byte) 2, 4.0f, 156),
    IRON((byte) 3, 4.0f, 198),
    STEEL((byte) 4, 5.0f, 230),
    GOLDEN((byte) 5, 6.0f, 64),
    PLATINUM((byte) 6, 8.0f, 512),
    DIAMOND((byte) 7, 10.0f, 1024);

    private final byte id;
    private final float defense;
    private final int durability;

    /**
     * Creates a new {@code Tier} instance.
     * @param id         the {@code byte} supplied as {@code id}
     * @param defense    the {@code float} supplied as {@code defense}
     * @param durability the {@code int} supplied as {@code durability}
     */
    Tier(byte id, float defense, int durability) {
        this.id = id;
        this.defense = defense;
        this.durability = durability;
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
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    public String getDisplayName() {
        if (this == NONE) return null;
        return "item.tier." + toStr(this);
    }

    /**
     * Produces the textual or converted representation for to str.
     * @param tier the {@link Tier} supplied as {@code tier}
     * @return the {@link String} representing the to str result
     */
    public static String toStr(Tier tier) {
        return tier.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the {@code defense} value
     * @return {@link float} value of defense
     */
    public float getDefense() {
        return defense;
    }

    /**
     * Returns the durability.
     * @return {@code int}; the durability
     */
    public int getDurability() {
        return durability;
    }

    /**
     * Checks whether the invalid tier condition is met.
     * @return {@code true} if invalid tier; otherwise {@code false}
     */
    public boolean isInvalidTier() {
        return this == NONE || this == LEATHER || this == WOODEN;
    }

    /**
     * Processes each applicable element for for each.
     * @param consumer the {@link Consumer} supplied as {@code consumer}
     */
    public static void forEach(Consumer<Tier> consumer) {
        for (Tier tier : values()) {
            consumer.accept(tier);
        }
    }
}
