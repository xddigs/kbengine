package org.kbeng.entity;

import org.kbeng.data.BlockPos;
import org.kbeng.data.RenderPass;
import org.kbeng.wrld.GameMaster;
import org.joml.Vector3f;

/**
 * Moon provides moon capabilities within the entity subsystem.
 *
 * It participates in actor simulation, state transitions, and per-frame world interaction contracts.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 *
 * It extends Entity, inheriting shared behavior while specializing subsystem-specific logic.
 */
public class Moon extends Entity {
    private final Vector3f direction;
    private final Vector3f color;
    private float intensity;

    /**
     * Creates a new {@code Moon} instance.
     * @param name the {@link String} supplied as {@code name}
     */
    public Moon(String name) {
        super(name);
        this.direction = new Vector3f();
        this.color = new Vector3f(0.45f, 0.55f, 0.85f);
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
        float angle = ((timeOfDay - 18.0f) / 24.0f) * ((float) Math.PI * 2.0f);
        float x = (float) Math.cos(angle);
        float y = (float) Math.sin(angle);
        direction.set(x, -y, -0.15f).normalize();
        if (timeOfDay >= 20.0f && timeOfDay < 22.0f) {
            float t = (timeOfDay - 20.0f) / 2.0f;
            intensity = t * 0.20f;
        } else if (timeOfDay >= 22.0f || timeOfDay < 5.0f) {
            intensity = 0.20f;
        } else if (timeOfDay >= 5.0f && timeOfDay < 7.0f) {
            float t = (7.0f - timeOfDay) / 2.0f;
            intensity = t * 0.20f;
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