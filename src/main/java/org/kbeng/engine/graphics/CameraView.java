package org.kbeng.engine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * CameraView defines the camera view contract within the graphics subsystem.
 * Implementations are expected to preserve these behavioral guarantees while choosing their own storage and execution strategy.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
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