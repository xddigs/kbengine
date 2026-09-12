package org.kbeng.utils;

import java.util.Random;

/**
 /**
  * serves provides serves capabilities within the utils subsystem.
  *
  * It centralizes reusable utilities such as constants, localization, settings, and small cross-cutting helpers.
  *
  * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
  */
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
