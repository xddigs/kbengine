package org.kbeng.games.rpg.data;

/**
 * SoilPosition is an immutable carrier for soil position state in the data subsystem.
 * Record semantics make instances cheap to pass across systems while preserving value-based equality and snapshot safety.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public record SoilPosition(int x, int y, int z) {}
