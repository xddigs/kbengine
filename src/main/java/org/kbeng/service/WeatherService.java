package org.kbeng.service;

import org.kbeng.data.Singleton;
import org.kbeng.data.WeatherType;
import org.kbeng.utils.K;

import java.util.Random;

/**
 * Represents the weather service component of the Isofarm runtime.
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
@Singleton
public class WeatherService implements Service<WeatherType> {
    public static final WeatherService wes = new WeatherService();
    private final static Random random = new Random();
    private static WeatherType weather = WeatherType.CLEAR;

    /**
     * Creates a new {@code WeatherService} instance.
     */
    private WeatherService() {}

    /**
     * Checks whether the raining condition is met.
     * @return {@code true} if raining; otherwise {@code false}
     */
    public static boolean isRaining() {
        return weather == WeatherType.RAIN ||
                weather == WeatherType.THUNDERSTORM;
    }

    /**
     * Returns the weather.
     * @return the {@link WeatherType} representing the weather
     */
    public WeatherType getWeather() {
        return weather;
    }

    /**
     * Sets the weather.
     * @param weather the {@link WeatherType} supplied as {@code weather}
     */
    public void setWeather(WeatherType weather) {
        WeatherService.weather = weather;
    }

    /**
     * Updates text or selection state for next weather.
     * @return the {@link WeatherType} representing the next weather result
     */
    public WeatherType nextWeather() {
        if (random.nextFloat() < K.World.WEATHER_CHANGE_PROBABILITY) {
            return WeatherType.values()[
                    (int) (Math.random() * WeatherType.values().length)];
        }
        return weather;
    }
}
