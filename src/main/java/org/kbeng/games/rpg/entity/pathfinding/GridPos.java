package org.kbeng.games.rpg.entity.pathfinding;

import org.kbeng.games.rpg.data.DataClass;

/**
 * GridPos is an immutable carrier for grid pos state in the pathfinding subsystem.
 * Record semantics make instances cheap to pass across systems while preserving value-based equality and snapshot safety.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public record GridPos(int x, int y, int z) {}