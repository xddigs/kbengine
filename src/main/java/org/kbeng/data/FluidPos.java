package org.kbeng.data;

/**
 * Immutable value object containing fluid pos.
 */
@DataClass
public record FluidPos(int x, int y, int z) {}