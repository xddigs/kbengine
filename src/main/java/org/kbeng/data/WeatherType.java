package org.kbeng.data;

import org.kbeng.utils.Local;

import java.util.Locale;

/**
 * WeatherType declares the canonical weather type set for the data subsystem.
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public enum WeatherType {
    CLEAR(1.0f, 0, 0.0f),
    RAIN(1.25f, 2, 0.0f),
    THUNDERSTORM(0.5f, 5, 0.05f),
    DROUGHT(0.0f, -1, 0.0f),
    FROST(0.0f, 0, 0.10f);

    private final float growthMultiplier;
    private final int waterDPS;
    private final float cropDamageChance;

    /**
     * Creates a new {@code WeatherType} instance.
     * @param growthMultiplier the {@code float} supplied as {@code growthMultiplier}
     * @param waterDPS the {@code int} supplied as {@code waterDPS}
     * @param cropDamageChance the {@code float} supplied as {@code cropDamageChance}
     */
    WeatherType(float growthMultiplier,
                int waterDPS, float cropDamageChance) {
        this.growthMultiplier = growthMultiplier;
        this.waterDPS = waterDPS;
        this.cropDamageChance = cropDamageChance;
    }

    /**
     * Returns the name.
     * @return the {@link String} representing the name
     */
    public String getName() { return name().toLowerCase(Locale.ROOT); }
    /**
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    public String getDisplayName() { return Local.lang.t("weather." + name().toLowerCase()); }
    /**
     * Returns the growth multiplier.
     * @return {@code float}; the growth multiplier
     */
    public float getGrowthMultiplier() { return growthMultiplier; }
    /**
     * Returns the water dps.
     * @return {@code int}; the water dps
     */
    public int getWaterDPS() { return waterDPS; }
    /**
     * Returns the crop damage chance.
     * @return {@code float}; the crop damage chance
     */
    public float getCropDamageChance() { return cropDamageChance; }
}