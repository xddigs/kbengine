package org.kbeng.data;

import org.kbeng.utils.K;

/**
 * Represents the toast component of the Isofarm runtime.
 *
 * <p>This type centralizes the state, lifecycle and behavior required by its callers,
 * keeping domain rules together with the data they operate on.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li><b>State:</b> Owns the data needed to represent the component consistently.</li>
 *   <li><b>Lifecycle:</b> Exposes the operations used to create, update and release its state.</li>
 *   <li><b>Integration:</b> Coordinates with the surrounding game, rendering or UI systems through its public API.</li>
 * </ul>
 *
 * <p>Callers should use the documented public operations and allow this type to preserve
 * its invariants rather than modifying implementation details directly.
 */
@DataClass
public class Toast {
    private final ToastData type;
    private final String message;

    private float x;
    private float y;

    private float targetX;
    private float targetY;

    private final float duration;

    private float elapsed;
    private boolean exiting;

    private float windowWidth;

    /**
     * Creates a new {@code Toast} instance.
     * @param startX the {@code float} supplied as {@code startX}
     * @param startY the {@code float} supplied as {@code startY}
     * @param targetX the {@code float} supplied as {@code targetX}
     * @param targetY the {@code float} supplied as {@code targetY}
     * @param type the {@link ToastData} supplied as {@code type}
     * @param message the {@link String} supplied as {@code message}
     * @param duration the {@code float} supplied as {@code duration}
     */
    public Toast(float startX, float startY, float targetX, float targetY,
                 ToastData type, String message, float duration) {
        this.x = startX;
        this.y = startY;

        this.targetX = targetX;
        this.targetY = targetY;

        this.type = type;
        this.message = message;
        this.duration = duration;

        this.elapsed = 0.0f;
        this.exiting = false;

        this.windowWidth = K.Window.DEFAULT_WIDTH;
    }

    /**
     * Sets the window width.
     * @param windowWidth the {@code float} supplied as {@code windowWidth}
     */
    public void setWindowWidth(float windowWidth) {
        this.windowWidth = windowWidth;

    }

    /**
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void update(float delta) {
        elapsed += delta;

        if (!exiting && elapsed >= duration) {
            exiting = true;
            targetX = windowWidth + K.UI.TOAST_WIDTH;
        }

        float speed = exiting ? K.UI.TOAST_EXIT_SPEED
                : K.UI.TOAST_SLIDE_SPEED;

        float factor = 1.0f - (float) Math.exp(-speed * delta);
        x += (targetX - x) * factor;
        y += (targetY - y) * factor;
    }

    /**
     * Checks whether the finished condition is met.
     * @return {@code true} if finished; otherwise {@code false}
     */
    public boolean isFinished() {
        if (!exiting) {
            return false;
        }

        return x >= targetX - 2.0f;
    }

    /**
     * Returns the type.
     * @return the {@link ToastData} representing the type
     */
    public ToastData getType() {
        return type;
    }

    /**
     * Returns the message.
     * @return the {@link String} representing the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the x.
     * @return {@code float}; the x
     */
    public float getX() {
        return x;
    }

    /**
     * Returns the y.
     * @return {@code float}; the y
     */
    public float getY() {
        return y;
    }

    /**
     * Returns the target x.
     * @return {@code float}; the target x
     */
    public float getTargetX() {
        return targetX;
    }

    /**
     * Returns the target y.
     * @return {@code float}; the target y
     */
    public float getTargetY() {
        return targetY;
    }

    /**
     * Returns the duration.
     * @return {@code float}; the duration
     */
    public float getDuration() {
        return duration;
    }

    /**
     * Returns the elapsed.
     * @return {@code float}; the elapsed
     */
    public float getElapsed() {
        return elapsed;
    }

    /**
     * Checks whether the exiting condition is met.
     * @return {@code true} if exiting; otherwise {@code false}
     */
    public boolean isExiting() {
        return exiting;
    }

    /**
     * Sets the target x.
     * @param targetX the {@code float} supplied as {@code targetX}
     */
    public void setTargetX(float targetX) {
        this.targetX = targetX;
    }

    /**
     * Sets the target y.
     * @param targetY the {@code float} supplied as {@code targetY}
     */
    public void setTargetY(float targetY) {
        this.targetY = targetY;
    }
}