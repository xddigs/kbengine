package org.kbeng.data;

/**
 * LavaPuddle is an immutable carrier for lava puddle state in the data subsystem.
 *
 * Record semantics make instances cheap to pass across systems while preserving value-based equality and snapshot safety.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public record LavaPuddle(int x, int y, int z) {}