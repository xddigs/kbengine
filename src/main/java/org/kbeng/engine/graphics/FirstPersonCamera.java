package org.kbeng.engine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Perspective camera intended to be attached to a locally controlled
 * observer's eye position.
 * <p>
 * The class belongs to the engine layer and therefore knows nothing about a
 * player entity, input bindings or a particular game. A game updates
 * {@link #getPosition()} from its observer and calls {@link #rotate(float, float)}
 * from its input controller. Pitch is clamped before matrices are built so the
 * view cannot flip at the vertical poles, while yaw is normalized to keep
 * long-running mouse input numerically stable.
 * <p>
 * Projection values are retained across framebuffer resizes. Calling
 * {@link #updateProjection(float, float)} changes only the aspect ratio; field
 * of view and clipping planes remain those supplied at construction time.
 */
public final class FirstPersonCamera implements CameraView {
    private static final float FULL_ROTATION = 360.0f;
    private static final float MIN_PITCH = -89.0f;
    private static final float MAX_PITCH = 89.0f;

    private final Vector3f position = new Vector3f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final float fieldOfView;
    private final float nearPlane;
    private final float farPlane;
    private float yaw;
    private float pitch;

    /**
     * Creates a perspective camera and immediately builds its projection.
     *
     * @param width initial framebuffer width in pixels; values below one are
     *              treated as one to keep the aspect ratio finite
     * @param height initial framebuffer height in pixels; values below one are
     *               treated as one to keep the aspect ratio finite
     * @param fieldOfView vertical field of view in degrees, exclusively between
     *                    zero and 180
     * @param nearPlane nearest positive view distance accepted by the projection
     * @param farPlane farthest view distance; must be greater than
     *                 {@code nearPlane}
     * @throws IllegalArgumentException if the field of view or clipping range
     *                                  cannot define a perspective projection
     */
    public FirstPersonCamera(float width, float height, float fieldOfView,
                             float nearPlane, float farPlane) {
        if (!Float.isFinite(fieldOfView) || fieldOfView <= 0.0f || fieldOfView >= 180.0f) {
            throw new IllegalArgumentException("Field of view must be between 0 and 180 degrees");
        }
        if (!Float.isFinite(nearPlane) || !Float.isFinite(farPlane)
                || nearPlane <= 0.0f || farPlane <= nearPlane) {
            throw new IllegalArgumentException("Clipping planes must satisfy 0 < near < far");
        }
        this.fieldOfView = fieldOfView;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
        updateProjection(width, height);
    }

    /**
     * Rebuilds the perspective projection for a framebuffer size. This method
     * is safe during minimized-window resize callbacks because each dimension
     * is clamped to at least one pixel before the aspect ratio is calculated.
     *
     * @param width current framebuffer width in pixels
     * @param height current framebuffer height in pixels
     */
    public void updateProjection(float width, float height) {
        float safeWidth = Math.max(width, 1.0f);
        float safeHeight = Math.max(height, 1.0f);
        projectionMatrix.identity().perspective(
                (float) Math.toRadians(fieldOfView), safeWidth / safeHeight,
                nearPlane, farPlane);
    }

    /**
     * Applies relative mouse-look rotation in degrees.
     *
     * @param yawDelta signed horizontal delta; positive values turn right
     * @param pitchDelta signed vertical delta; positive values look downward
     */
    public void rotate(float yawDelta, float pitchDelta) {
        setOrientation(yaw + yawDelta, pitch + pitchDelta);
    }

    /**
     * Replaces the view orientation. Yaw wraps into {@code [0, 360)} and pitch
     * is clamped to {@code [-89, 89]} to preserve a stable up direction.
     *
     * @param yaw horizontal angle in degrees
     * @param pitch vertical angle in degrees; positive values look downward
     */
    public void setOrientation(float yaw, float pitch) {
        this.yaw = yaw % FULL_ROTATION;
        if (this.yaw < 0.0f) this.yaw += FULL_ROTATION;
        this.pitch = Math.clamp(pitch, MIN_PITCH, MAX_PITCH);
    }

    /**
     * Builds a world-to-view transform using the same yaw/pitch convention as
     * the engine's orthographic camera: zero yaw faces negative Z and positive
     * pitch looks downward.
     *
     * @return a new view matrix that callers may mutate safely
     */
    @Override
    public Matrix4f getViewMatrix() {
        return new Matrix4f()
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw))
                .translate(-position.x, -position.y, -position.z);
    }

    /**
     * Returns the live perspective projection. Callers must treat the returned
     * matrix as read-only because it is reused between frames.
     *
     * @return the current perspective projection matrix
     */
    @Override
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    /**
     * Returns the mutable world-space eye position. Attachment code may update
     * the vector in place each frame to avoid allocating temporary objects.
     *
     * @return the live eye-position vector
     */
    @Override
    public Vector3f getPosition() {
        return position;
    }

    /** @return the vertical look angle in degrees, positive downward */
    @Override
    public float getPitch() {
        return pitch;
    }

    /** @return the normalized horizontal look angle in degrees */
    @Override
    public float getYaw() {
        return yaw;
    }

    /** @return {@link CameraMode#FIRST_PERSON} for renderer policy decisions */
    @Override
    public CameraMode getMode() {
        return CameraMode.FIRST_PERSON;
    }
}
