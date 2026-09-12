package org.kbeng.rpg.service;

import org.kbeng.rpg.data.Season;
import org.kbeng.rpg.data.Singleton;
import org.kbeng.engine.utils.Local;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TimeService provides time service capabilities within the service subsystem.
 * It provides shared runtime services, registries, and policy logic consumed by orchestrators and feature modules.
 * The service acts as a shared policy and state access point for other runtime modules.
 */
@Singleton
public class TimeService {
    public static final TimeService ts = new TimeService();
    private static final Logger log = LoggerFactory.getLogger(TimeService.class);
    private static final float REAL_SECONDS_PER_IN_GAME_MINUTE = 0.7f;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int HOURS_PER_DAY = 24;
    private static final int DAYS_PER_SEASON = 28;
    private static final int STARTING_HOUR = 8;

    private Season currentSeason = Season.SPRING;
    private float secondAccumulator = 0.0f;
    private static int minute = 0;
    private static int hour = STARTING_HOUR;
    private int day = 1;
    private int year = 0;
    private float timeScale = 2.0f;

    /**
     * Creates a new {@code TimeService} instance.
     */
    private TimeService() {}

    /**
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     * @param weatherService the {@link WeatherService} supplied as {@code weatherService}
     */
    public void update(float delta, WeatherService weatherService) {
        secondAccumulator += delta * timeScale;
        while (secondAccumulator >= REAL_SECONDS_PER_IN_GAME_MINUTE) {
            secondAccumulator -= REAL_SECONDS_PER_IN_GAME_MINUTE;
            advanceMinute(weatherService);
        }
    }

    /**
     * Returns the minute.
     * @return {@code int}; the minute
     */
    public int getMinute() {
        return minute;
    }

    /**
     * Returns the hour.
     * @return {@code int}; the hour
     */
    public int getHour() {
        return hour;
    }

    /**
     * Returns the day.
     * @return {@code int}; the day
     */
    public int getDay() {
        return day;
    }

    /**
     * Returns the current season.
     * @return the {@link Season} representing the current season
     */
    public Season getCurrentSeason() {
        return currentSeason;
    }

    /**
     * Returns the year.
     * @return {@code int}; the year
     */
    public int getYear() {
        return year;
    }

    /**
     * Returns the formatted time.
     * @return the {@link String} representing the formatted time
     */
    public String getFormattedTime() {
        String hour = String.format("%02d", TimeService.hour);
        String minute = String.format("%02d", TimeService.minute);
        return Local.lang.f("time.formatted", year, currentSeason.getDisplayName(), day, hour, minute);
    }

    /**
     * Sets the time scale.
     * @param timeScale the {@code float} supplied as {@code timeScale}
     */
    public void setTimeScale(float timeScale) {
        this.timeScale = Math.max(0.0f, timeScale);
    }

    /**
     * Returns the time scale.
     * @return {@code float}; the time scale
     */
    public float getTimeScale() {
        return timeScale;
    }

    /**
     * Adds the time scale.
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void addTimeScale(float delta) {
        setTimeScale(getTimeScale() + delta);
    }

    /**
     * Transforms time scale according to the supplied values.
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void slowTimeScale(float delta) {
        setTimeScale(getTimeScale() * (1.0f - delta));
    }

    /**
     * Updates minute for the current simulation step.
     * @param weatherService the {@link WeatherService} supplied as {@code weatherService}
     */
    private void advanceMinute(WeatherService weatherService) {
        minute++;
        if (minute >= MINUTES_PER_HOUR) {
            minute = 0;
            advanceHour(weatherService);
        }
    }

    /**
     * Updates hour for the current simulation step.
     * @param weatherService the {@link WeatherService} supplied as {@code weatherService}
     */
    private void advanceHour(WeatherService weatherService) {
        hour++;
        weatherService.setWeather(weatherService.nextWeather());
        if (hour >= HOURS_PER_DAY) {
            advanceDay();
        }
    }

    /**
     * Updates day for the current simulation step.
     */
    public void advanceDay() {
        hour = STARTING_HOUR;
        minute = 0;
        secondAccumulator = 0.0f;
        day++;

        if (day > DAYS_PER_SEASON) {
            day = 1;
            advanceSeason();
        }

        log.info("New day started: Year {} {}, Day {}", year, currentSeason, day);
    }

    /**
     * Updates season for the current simulation step.
     */
    private void advanceSeason() {
        Season[] seasons = Season.values();
        int nextSeasonIndex = (currentSeason.ordinal() + 1) % seasons.length;
        currentSeason = seasons[nextSeasonIndex];

        if (nextSeasonIndex == 0) {
            year++;
            log.info("Happy New Year! Welcome to Year {}", year);
        }

        log.info("Season changed to {}", currentSeason);
    }

    /**
     * Returns the sky color.
     * @return the {@link Vector3f} representing the sky color
     */
    public static Vector3f getSkyColor() {
        float time = hour + minute / 60.0f;
        Vector3f cloudy = new Vector3f(0.48f, 0.48f, 0.48f);
        Vector3f night = new Vector3f(0.025f, 0.035f, 0.09f);
        Vector3f dawn = new Vector3f(0.85f, 0.40f, 0.22f);
        Vector3f day = new Vector3f(0.35f, 0.65f, 0.95f);
        Vector3f dusk = new Vector3f(0.70f, 0.25f, 0.30f);
        Vector3f result = new Vector3f();

        boolean isRaining = WeatherService.isRaining();
        if (isRaining) {
            float t = smoothStep(5.0f, 7.0f, time);
            cloudy.lerp(cloudy, t, result);
            return result;
        }

        if (time < 5.0f) {
            result.set(night);
        } else if (time < 7.0f) {
            float t = smoothStep(5.0f, 7.0f, time);
            night.lerp(dawn, t, result);
        } else if (time < 12.0f) {
            float t = smoothStep(7.0f, 12.0f, time);
            dawn.lerp(day, t, result);
        } else if (time < 17.0f) {
            float t = smoothStep(12.0f, 17.0f, time);
            day.lerp(day, t, result);
        } else if (time < 20.0f) {
            float t = smoothStep(17.0f, 20.0f, time);
            day.lerp(dusk, t, result);
        } else if (time < 22.0f) {
            float t = smoothStep(20.0f, 22.0f, time);
            dusk.lerp(night, t, result);
        } else {
            result.set(night);
        }

        return result;
    }

    /**
     * Transforms step according to the supplied values.
     * @param start the {@code float} supplied as {@code start}
     * @param end the {@code float} supplied as {@code end}
     * @param value the {@code float} supplied as {@code value}
     * @return {@code float}; the smooth step result
     */
    private static float smoothStep(float start, float end, float value) {
        float t = (value - start) / (end - start);
        t = Math.clamp(t, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
}
