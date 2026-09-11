package org.kbeng.data;

/**
 * Immutable value object containing chunk pos.
 */
@DataClass
public record ChunkPos(int x, int z) {}
