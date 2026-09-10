package com.isofarm.utils;

import java.util.Random;

/**
 * This class serves as a bridge between {@link Random}
 * and static instantiation.
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
