package org.kbeng.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Defines the camera view contract.
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
}