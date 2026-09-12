package org.kbeng.service;

import org.kbeng.data.BlockPos;
import org.kbeng.data.Season;
import org.kbeng.entity.Moon;
import org.kbeng.entity.Sun;
import org.kbeng.graphics.CelestialLighting;

/**
 * Owns environment simulation that affects time, weather, and global lighting.
 * <p>This system keeps celestial state local to one place and bridges
 * {@link TimeService} + {@link WeatherService} with {@link CelestialLighting},
 * so callers consume one coherent "environment tick" per frame.
 */
public final class EnvironmentSystem {
    private final Sun sun = new Sun("Sun");
    private final Moon moon = new Moon("Moon");
    private final CelestialLighting celestialLighting = new CelestialLighting(sun, moon);

    /**
     * Advances the environment simulation by one frame.
     * <p>Time progression may trigger weather changes through
     * {@link TimeService#update(float, WeatherService)}; lighting is then
     * recalculated from the resulting time-of-day.
     * @param hoveredCell currently hovered world cell used by sun/moon update code
     * @param delta elapsed frame time in seconds
     */
    public void update(BlockPos hoveredCell, float delta) {
        TimeService.ts.update(delta, WeatherService.wes);
        float timeOfDay = TimeService.ts.getHour() + (TimeService.ts.getMinute() / 60.0f);
        celestialLighting.update(hoveredCell, timeOfDay);
    }

    /**
     * Returns the current in-game season from the shared calendar.
     * @return active season value
     */
    public Season getSeason() {
        return TimeService.ts.getCurrentSeason();
    }

    /**
     * Returns the blended celestial lighting state used by world shaders.
     * @return directional light source and intensity envelope
     */
    public CelestialLighting getCelestialLighting() {
        return celestialLighting;
    }

    /**
     * Returns the managed sun entity instance.
     * @return sun object
     */
    public Sun getSun() {
        return sun;
    }

    /**
     * Returns the managed moon entity instance.
     * @return moon object
     */
    public Moon getMoon() {
        return moon;
    }
}
