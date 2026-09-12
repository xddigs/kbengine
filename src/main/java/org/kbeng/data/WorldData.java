package org.kbeng.data;

import org.kbeng.wrld.*;

/**
 * Shows available world types that can be created
 */
public enum WorldData {
    /** A world with a small island */
    ISLAND,
    /** A world with a flat terrain */
    FLAT,
    /** A world with a large open terrain */
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

    /**
     * Creates a new world generator based on the world type
     * @param world the {@link WorldData} representing the world type
     * @param fluidSimulation the {@link FluidSimulation} representing the fluid simulation
     * @return the {@link Generator} representing the world generator
     */
    public static Generator create(WorldData world, FluidSimulation fluidSimulation) {
        return switch (world) {
            case ISLAND -> new IslandGenerator(World.wrld, fluidSimulation);
            case FLAT -> new FlatworldGenerator(World.wrld);
            default -> throw new IllegalArgumentException("Unknown world type: " + world);
        };
    }
}
