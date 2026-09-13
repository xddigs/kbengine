package org.kbeng.games.rpg.data;

/**
 * Lake is an immutable carrier for lake state in the data subsystem.
 * Record semantics make instances cheap to pass across systems while preserving value-based equality and snapshot safety.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public record Lake(int x, int z, float radius) {}