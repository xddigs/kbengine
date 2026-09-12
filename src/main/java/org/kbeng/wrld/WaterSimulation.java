package org.kbeng.wrld;

import org.kbeng.data.BlockData;
import org.kbeng.data.Singleton;

/**
 * WaterSimulation provides water simulation capabilities within the wrld subsystem.
 *
 * It maintains world simulation concerns including terrain, chunks, fluids, and authoritative spatial state.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 *
 * It extends FluidSimulation, inheriting shared behavior while specializing subsystem-specific logic.
 */
@Singleton
public final class WaterSimulation extends FluidSimulation {
    /**
     * Shared water simulation instance.
     */
    public static final WaterSimulation ws = new WaterSimulation();
    private static final float STEP_TIME = 0.25f;

    /**
     * Creates the singleton {@code WaterSimulation} instance.
     */
    private WaterSimulation() {
        super(BlockData.WATER, STEP_TIME, true);
    }

    /**
     * Removes water and recalculates its connected flow.
     * @param x the {@code int} argument; the water x value
     * @param y the {@code int} argument; the water y value
     * @param z the {@code int} argument; the water z value
     * @return {@code true} when water was removed; otherwise {@code false}
     */
    public boolean removeWater(int x, int y, int z) {
        return removeFluid(x, y, z);
    }
}
