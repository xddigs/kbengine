package org.kbeng.service;

import org.kbeng.data.BlockPos;
import org.kbeng.data.Season;
import org.kbeng.entity.Moon;
import org.kbeng.entity.Sun;
import org.kbeng.graphics.CelestialLighting;

/**
 * Centraliza el estado ambiental del mundo.
 *
 * <p>Este sistema agrupa el ciclo temporal, clima y luz celeste para que el
 * orquestador del juego no tenga que gestionar por separado {@link Sun},
 * {@link Moon}, {@link CelestialLighting}, {@link TimeService} y
 * {@link WeatherService}.
 */
public final class EnvironmentSystem {
    private final Sun sun = new Sun("Sun");
    private final Moon moon = new Moon("Moon");
    private final CelestialLighting celestialLighting = new CelestialLighting(sun, moon);

    /**
     * Avanza tiempo, clima y luz global del mundo.
     *
     * @param hoveredCell celda bajo cursor usada por los cuerpos celestes
     * @param delta       tiempo de frame en segundos
     */
    public void update(BlockPos hoveredCell, float delta) {
        TimeService.ts.update(delta, WeatherService.wes);
        float timeOfDay = TimeService.ts.getHour() + (TimeService.ts.getMinute() / 60.0f);
        celestialLighting.update(hoveredCell, timeOfDay);
    }

    /**
     * Devuelve la estación actual del calendario del juego.
     *
     * @return estación activa
     */
    public Season getSeason() {
        return TimeService.ts.getCurrentSeason();
    }

    /**
     * Expone la fuente de luz celeste consolidada (sol/luna).
     *
     * @return estado de iluminación celeste
     */
    public CelestialLighting getCelestialLighting() {
        return celestialLighting;
    }

    /**
     * Devuelve la entidad de sol gestionada por este sistema.
     *
     * @return sol
     */
    public Sun getSun() {
        return sun;
    }

    /**
     * Devuelve la entidad de luna gestionada por este sistema.
     *
     * @return luna
     */
    public Moon getMoon() {
        return moon;
    }
}

