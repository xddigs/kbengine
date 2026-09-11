package org.kbeng.entity;

import org.kbeng.data.BlockPos;
import org.kbeng.data.RenderPass;
import org.kbeng.wrld.GameMaster;
import org.joml.Vector3f;

/**
 * Represents the sun component of the Isofarm runtime.
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
public class Sun extends Entity {
    private final Vector3f direction;
    private final Vector3f color;
    private float intensity;

    /**
     * Creates a new {@code Sun} instance.
     * @param name the {@link String} supplied as {@code name}
     */
    public Sun(String name) {
        super(name);
        this.direction = new Vector3f();
        this.color = new Vector3f(1.0f, 0.95f, 0.85f);
        this.intensity = 0.0f;
    }

    /**
     * Returns the direction.
     * @return the {@link Vector3f} representing the direction
     */
    public Vector3f getDirection() {
        return direction;
    }

    /**
     * Returns the color.
     * @return the {@link Vector3f} representing the color
     */
    public Vector3f getColor() {
        return color;
    }

    /**
     * Returns the intensity.
     * @return {@code float}; the intensity
     */
    public float getIntensity() {
        return intensity;
    }

    /**
     * {@inheritDoc}
     * Updates the current state.
     * @param blockPos the {@link BlockPos} supplied as {@code blockPos}
     * @param timeOfDay the {@code float} supplied as {@code timeOfDay}
     */
    @Override
    public void update(BlockPos blockPos, float timeOfDay) {
        float angle = ((timeOfDay - 6.0f) / 24.0f) * ((float) Math.PI * 2.0f);
        float x = (float) Math.cos(angle);
        float y = (float) Math.sin(angle);
        direction.set(x, -y, 0.25f).normalize();

        if (timeOfDay >= 5.0f && timeOfDay < 7.0f) {
            float t = (timeOfDay - 5.0f) / 2.0f;
            intensity = 0.15f + (t * 0.85f);
        } else if (timeOfDay >= 7.0f && timeOfDay < 19.0f) {
            intensity = 1.0f;
        } else if (timeOfDay >= 19.0f && timeOfDay < 22.0f) {
            float t = (timeOfDay - 19.0f) / 3.0f;
            intensity = 1.0f - (t * 0.9f);
        } else {
            intensity = 0.0f;
        }
    }

    /**
     * {@inheritDoc}
     * Renders this object in the requested render pass.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param pass the {@link RenderPass} supplied as {@code pass}
     */
    @Override
    public void render(GameMaster gameMaster, RenderPass pass) {}
}