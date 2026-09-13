package org.kbeng.engine.graphics;

/**
 * Semantic modes supported by an engine {@link CameraView}.
 * <p>
 * The distinction is intentionally stronger than perspective versus
 * orthographic projection. {@link #FIRST_PERSON} means that the view origin is
 * expected to represent the local observer, which lets higher-level renderers
 * suppress observer-only effects such as the local avatar or a tactical
 * fog-of-war overlay. {@link #ORTHOGRAPHIC} represents a detached scene view
 * where those elements remain meaningful.
 */
public enum CameraMode {
    /** A detached view using an orthographic projection. */
    ORTHOGRAPHIC,

    /** A perspective view located at the local observer's eye position. */
    FIRST_PERSON
}
