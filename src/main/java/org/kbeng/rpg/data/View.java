package org.kbeng.rpg.data;

/**
 * View declares the canonical view set for the data subsystem.
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
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
