package org.kbeng.utils;

import java.util.Random;

/**
 * Shared pseudo-random source used by gameplay systems that do not require deterministic seeds.
 *
 * The class exposes one process-wide {@link Random} instance through {@link #get()} and {@link #rand},
 * avoiding repeated object allocation and keeping random calls centralized for utility and data layers.
 */
@Utils
public class RandomLocal extends Random {
    private static final Random random = new Random();
    public static final Random rand = random;

    private RandomLocal() {}

    public static Random get() {
        return rand;
    }
}
