package org.kbeng.engine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Read-only camera state consumed by the engine render pipeline.
 * <p>
 * Implementations own their projection policy and mutable pose, while renderers
 * receive only the matrices, world-space origin and Euler angles required for
 * culling, billboarding and motion effects. {@link #getMode()} deliberately
 * exposes the semantic view mode as well: a projection matrix alone cannot tell
 * a game whether the camera represents the local player's eyes or a detached
 * orthographic view.
 */
public interface CameraView {
    /**
     * Returns the view matrix.
     * @return the {@link Matrix4f} representing the view matrix
     */
    Matrix4f getViewMatrix();
    /**
     * Returns the projection matrix.
     * @return the {@link Matrix4f} representing the projection matrix
     */
    Matrix4f getProjectionMatrix();
    /**
     * Returns the position.
     * @return the {@link Vector3f} representing the position
     */
    Vector3f getPosition();
    /**
     * Returns the pitch.
     * @return {@code float}; the pitch
     */
    float getPitch();
    /**
     * Returns the yaw.
     * @return {@code float}; the yaw
     */
    float getYaw();

    /**
     * Identifies how this view participates in camera-dependent presentation.
     * Renderers can use this value for policies such as hiding the local avatar
     * in first person without depending on a concrete camera implementation.
     *
     * @return the stable semantic mode of this camera
     */
    CameraMode getMode();
}
