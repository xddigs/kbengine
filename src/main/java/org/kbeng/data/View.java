package org.kbeng.data;

/**
 * Describes how the world around the player must be presented.
 */
@DataClass
public enum View {
    EXTERIOR(0),
    INTERIOR(1),
    UNDERGROUND(2);

    private final int shaderId;

    View(int shaderId) {
        this.shaderId = shaderId;
    }

    /** Numeric representation consumed by the world shaders. */
    public int getShaderId() {
        return shaderId;
    }
}
