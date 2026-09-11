package org.kbeng.pathfinding;

import org.kbeng.data.DataClass;

/**
 * Immutable value object containing grid pos.
 */
@DataClass
public record GridPos(int x, int y, int z) {}