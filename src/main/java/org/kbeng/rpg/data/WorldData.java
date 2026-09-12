package org.kbeng.rpg.data;

import org.kbeng.rpg.wrld.*;

/**
 * WorldData declares the canonical world data set for the data subsystem.
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 * The data type is used as a normalized schema for transport, persistence, and runtime inspection.
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
            case FLAT -> new FlatworldGenerator();
            case ISLAND -> new IslandGenerator(fluidSimulation);
            case OPEN_WORLD -> new WorldGenerator(fluidSimulation);
            default -> throw new IllegalArgumentException("Unknown world type: " + world);
        };
    }
}
