package org.kbeng.data;

/**
 * Stores the position of the guaranteed one-block lava puddle.
 */
@DataClass
public record LavaPuddle(int x, int y, int z) {}