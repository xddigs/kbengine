package org.kbeng.engine.graphics;

import org.kbeng.rpg.data.BlockPos;
import org.kbeng.rpg.entity.Moon;
import org.kbeng.rpg.entity.Sun;
import org.joml.Vector3f;

/**
 * CelestialLighting provides celestial lighting capabilities within the graphics subsystem.
 * It contributes to OpenGL resource ownership, render-state setup, or frame-pipeline execution.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public class CelestialLighting {
    private final Sun sun;
    private final Moon moon;

    private final Vector3f direction;
    private final Vector3f color;

    private float intensity;
    private float ambientIntensity;

    /**
     * Creates a new {@code CelestialLighting} instance.
     * @param sun the {@link Sun} supplied as {@code sun}
     * @param moon the {@link Moon} supplied as {@code moon}
     */
    public CelestialLighting(Sun sun, Moon moon) {
        this.sun = sun;
        this.moon = moon;
        this.direction = new Vector3f();
        this.color = new Vector3f();
    }

    /**
     * Updates the current state.
     * @param hoveredCell the {@link BlockPos} supplied as {@code hoveredCell}
     * @param timeOfDay the {@code float} supplied as {@code timeOfDay}
     */
    public void update(BlockPos hoveredCell, float timeOfDay) {
        sun.update(hoveredCell, timeOfDay);
        moon.update(hoveredCell, timeOfDay);

        float sunIntensity = sun.getIntensity();
        float moonIntensity = moon.getIntensity();

        if (sunIntensity > moonIntensity) {
            direction.set(sun.getDirection());
            color.set(sun.getColor());
            intensity = sunIntensity;
        } else {
            direction.set(moon.getDirection());
            color.set(moon.getColor());
            intensity = moonIntensity;
        }

        float daylight = Math.clamp(sunIntensity, 0.0f, 1.0f);
        ambientIntensity = 0.35f + daylight * 0.40f;
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
     * Returns the ambient intensity.
     * @return {@code float}; the ambient intensity
     */
    public float getAmbientIntensity() {
        return ambientIntensity;
    }

    /**
     * Returns the sun.
     * @return the {@link Sun} representing the sun
     */
    public Sun getSun() {
        return sun;
    }

    /**
     * Returns the moon.
     * @return the {@link Moon} representing the moon
     */
    public Moon getMoon() {
        return moon;
    }
}
