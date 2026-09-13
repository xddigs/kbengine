package org.kbeng.games.rpg.data;

/**
 * ChunkPos is an immutable carrier for chunk pos state in the data subsystem.
 * Record semantics make instances cheap to pass across systems while preserving value-based equality and snapshot safety.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public record ChunkPos(int x, int z) {}
