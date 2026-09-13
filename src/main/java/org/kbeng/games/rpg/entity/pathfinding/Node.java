package org.kbeng.games.rpg.entity.pathfinding;

import org.kbeng.games.rpg.data.DataClass;

/**
 * Node is an immutable carrier for node state in the pathfinding subsystem.
 * Record semantics make instances cheap to pass across systems while preserving value-based equality and snapshot safety.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public record Node(GridPos position, Node parent, float gCost, float hCost) {

    /**
     * Produces the textual or converted representation for f cost.
     * @return {@code float}; the f cost result
     */
    public float fCost() {
        return gCost + hCost;
    }
}