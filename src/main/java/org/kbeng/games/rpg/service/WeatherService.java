package org.kbeng.games.rpg.service;

import org.kbeng.engine.service.Service;
import org.kbeng.games.rpg.data.Singleton;
import org.kbeng.games.rpg.data.WeatherType;
import org.kbeng.engine.utils.K;

import java.util.Random;

/**
 * WeatherService provides weather service capabilities within the service subsystem.
 * It provides shared runtime services, registries, and policy logic consumed by orchestrators and feature modules.
 * The service acts as a shared policy and state access point for other runtime modules.
 * It implements Service<WeatherType>, providing a concrete strategy for this subsystem contract.
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
