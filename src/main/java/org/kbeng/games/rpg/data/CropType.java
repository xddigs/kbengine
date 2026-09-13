package org.kbeng.games.rpg.data;

import org.kbeng.engine.utils.Local;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * CropType declares the canonical crop type set for the data subsystem.
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public enum CropType {
    WHEAT((byte) 0, "crop.wheat", 4, 5, 4),
    CARROT((byte) 1, "crop.carrot", 3, 8, 6),
    POTATO((byte) 2, "crop.potato", 6, 12, 8),
    BEETROOT((byte) 3, "crop.beetroot", 4, 16, 2),
    SUGAR_CANE_CROP((byte) 4, "crop.sugar_cane", 3, 10, 2);

    private final byte id;
    private final String displayName;
    private final int yield;
    private final int value;
    private final int seeds;

    /**
     * Creates a new {@code CropType} instance.
     * @param id the {@code byte} supplied as {@code id}
     * @param displayName the {@link String} supplied as {@code displayName}
     * @param yield the {@code int} supplied as {@code yield}
     * @param value the {@code int} supplied as {@code value}
     * @param seeds the {@code int} supplied as {@code seeds}
     */
    CropType(byte id, String displayName,
             int yield, int value, int seeds) {
        this.id = id;
        this.displayName = displayName;
        this.yield = yield;
        this.value = value;
        this.seeds = seeds;
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
        return Local.lang.t(displayName);
    }

    /**
     * Returns the yield.
     * @return {@code int}; the yield
     */
    public int getYield() {
        return yield;
    }

    /**
     * Returns the value.
     * @return {@code int}; the value
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns the seeds.
     * @return {@code int}; the seeds
     */
    public int getSeeds() {
        return seeds;
    }

    /**
     * Checks whether this crop may grow in a vertical stack.
     * @return {@code true} only for sugar cane
     */
    public boolean isStackable() {
        return this == SUGAR_CANE_CROP;
    }

    /**
     * Checks whether this crop uses the crossed plant mesh.
     * @return {@code false}; all crops use the standard crop mesh
     */
    public boolean usesPlantMesh() {
        return false;
    }

    /**
     * Creates or returns from id from the supplied arguments.
     * @param id the {@code byte} supplied as {@code id}
     * @return the {@link CropType} representing the from id result
     */
    public static CropType fromId(byte id) {
        for (CropType crop : CropType.values()) {
            if (crop.getId() == id) {
                return crop;
            }
        }
        return null;
    }

    /**
     * Performs the given action for each crop in the enum.
     * @param consumer the action to be performed for each crop
     */
    public static void forEach(Consumer<CropType> consumer) {
        for (CropType crop : CropType.values()) {
            consumer.accept(crop);
        }
    }
}
