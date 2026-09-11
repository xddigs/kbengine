package org.kbeng.data;

/**
 * Immutable value object containing block node.
 */
@DataClass
public record BlockNode(int x, int y, int z, int distance) {}