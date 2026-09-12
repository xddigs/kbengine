package org.kbeng.data;

/**
 * Represents the rain drop component of the kbengine runtime.
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
public class RainDrop {
    private final float velocity;
    private final float length;
    private float x, y, z;

    /**
     * Creates a new {@code RainDrop} instance.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param z the {@code float} supplied as {@code z}
     * @param velocity the {@code float} supplied as {@code velocity}
     * @param length the {@code float} supplied as {@code length}
     */
    public RainDrop(float x, float y, float z,
                    float velocity, float length) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.velocity = velocity;
        this.length = length;
    }

    /**
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void update(float delta) {
        y -= velocity * delta;
    }

    /**
     * Checks whether the dead condition is met.
     * @param groundY the {@code float} supplied as {@code groundY}
     * @return {@code true} if dead; otherwise {@code false}
     */
    public boolean isDead(float groundY) {
        return y + length < groundY;
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
     * Returns the z.
     * @return {@code float}; the z
     */
    public float getZ() {
        return z;
    }

    /**
     * Returns the length.
     * @return {@code float}; the length
     */
    public float getLength() {
        return length;
    }
}
