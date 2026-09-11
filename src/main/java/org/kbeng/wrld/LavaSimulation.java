package org.kbeng.wrld;

import org.kbeng.data.BlockData;
import org.kbeng.data.Singleton;

/**
 * Simulates lava using the shared fluid flow implementation. Lava advances
 * more slowly than water and does not form renewable sources.
 */
@Singleton
public final class LavaSimulation extends FluidSimulation {
    /**
     * Shared lava simulation instance.
     */
    public static final LavaSimulation ls = new LavaSimulation();
    private static final float STEP_TIME = 0.85f;

    /**
     * Creates the singleton {@code LavaSimulation} instance.
     */
    private LavaSimulation() {
        super(BlockData.LAVA, STEP_TIME, false);
    }

    /**
     * Removes lava and recalculates its connected flow.
     * @param x the {@code int} argument; the lava x value
     * @param y the {@code int} argument; the lava y value
     * @param z the {@code int} argument; the lava z value
     * @return {@code true} when lava was removed; otherwise {@code false}
     */
    public boolean removeLava(int x, int y, int z) {
        return removeFluid(x, y, z);
    }
}
