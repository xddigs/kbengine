package org.kbeng.pathfinding;

import org.kbeng.data.DataClass;

/**
 * Immutable value object containing node.
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