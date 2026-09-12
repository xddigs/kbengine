package org.kbeng.data;

/**
 * Shows available world types that can be created
 */
public enum WorldData {
    /**
     * A world with a flat terrain and no features
     */
    FLAT,

    /**
     * A world with a normal terrain and features
     */
    ISLAND,

    /**
     * A world with a large biome size
     */
    OPEN_WORLD;

    private final byte id;

    WorldData() {
        this.id = (byte) ordinal();
    }

    /**
     * Returns the {@code id} value
     * @return {@link byte} value of {@code id}
     */
    public byte getId() {
        return id;
    }
}
