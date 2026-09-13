package org.kbeng.games.rpg.service;

import org.kbeng.engine.utils.Settings;
import org.kbeng.engine.utils.ToastFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GameRules provides game rules capabilities within the service subsystem.
 * It provides shared runtime services, registries, and policy logic consumed by orchestrators and feature modules.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@SuppressWarnings("all")
public final class GameRules {
    private static final Map<String, Object> RULES = new LinkedHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(GameRules.class);

    static {
        RULES.put("doKeepInventory", Settings.doKeepInventory());
        RULES.put("doEnableMotions", Settings.doEnableMotions());
        RULES.put("doEnableShadows", Settings.doEnableShadows());
        RULES.put("doBookAnimation", Settings.doBookAnimation());
        RULES.put("doEnableIds", Settings.doEnableIds());
        RULES.put("doEnablePaper", Settings.doEnablePaper());
        RULES.put("renderDistance", Settings.getRenderDistance());
        RULES.put("fov", Settings.getFov());
        RULES.put("unloadMargin", Settings.getUnloadMargin());
        RULES.put("maxInteractionDistance", Settings.getMaxInteractionDistance());
    }

    /**
     * Creates a new {@code GameRules} instance.
     */
    private GameRules() {}

    /**
     * Determines whether exists satisfies the required comparison or validity rules.
     * @param rule the {@link String} supplied as {@code rule}
     * @return {@code boolean}; the exists result
     */
    public static boolean exists(String rule) {
        return RULES.containsKey(rule);
    }

    /**
     * Returns get.
     * @param rule the {@link String} supplied as {@code rule}
     * @return the {@link Object} representing the get result
     */
    public static Object get(String rule) {
        return RULES.get(rule);
    }

    /**
     * Returns the rules.
     * @return the {@link Map} representing the rules
     */
    public static Map<String, Object> getRules() {
        return Map.copyOf(RULES);
    }

    /**
     * Returns the boolean.
     * @param rule the {@link String} supplied as {@code rule}
     * @return {@code boolean}; the boolean
     */
    public static boolean getBoolean(String rule) {
        Object value = RULES.get(rule);

        if (!(value instanceof Boolean booleanValue)) {
            throw new IllegalArgumentException(
                    "GameRule is not boolean: " + rule
            );
        }

        return booleanValue;
    }

    /**
     * Returns the int.
     * @param rule the {@link String} supplied as {@code rule}
     * @return {@code int}; the int
     */
    public static int getInt(String rule) {
        Object value = RULES.get(rule);

        if (!(value instanceof Integer intValue)) {
            throw new IllegalArgumentException(
                    "GameRule is not integer: " + rule
            );
        }

        return intValue;
    }

    /**
     * Returns the float.
     * @param rule the {@link String} supplied as {@code rule}
     * @return {@code float}; the float
     */
    public static float getFloat(String rule) {
        Object value = RULES.get(rule);

        if (!(value instanceof Float floatValue)) {
            throw new IllegalArgumentException(
                    "GameRule is not float: " + rule
            );
        }

        return floatValue;
    }

    /**
     * Sets set.
     * @param rule the {@link String} supplied as {@code rule}
     * @param value the {@link Object} supplied as {@code value}
     */
    public static void set(String rule, Object value) {
        if (!RULES.containsKey(rule)) {
            throw new IllegalArgumentException(
                    "Unknown gamerule: " + rule
            );
        }

        Object current = RULES.get(rule);
        if (!current.getClass().equals(value.getClass())) {
            throw new IllegalArgumentException(
                    "Invalid value type for gamerule: " + rule
            );
        }

        RULES.put(rule, value);
        apply(rule, value);
    }

    /**
     * Applies this object to the current state.
     * @param rule the {@link String} supplied as {@code rule}
     * @param value the {@link Object} supplied as {@code value}
     */
    private static void apply(String rule, Object value) {
        try {
            switch (rule) {
                case "doKeepInventory" -> {
                    Settings.setDoKeepInventory((Boolean) value);
                }

                case "doEnableMotions" -> {
                    Settings.setDoEnableMotions((Boolean) value);
                }

                case "doEnableShadows" -> {
                    Settings.setDoEnableShadows((Boolean) value);
                }

                case "doBookAnimation" -> {
                    Settings.setDoBookAnimation((Boolean) value);
                }

                case "doEnableIds" -> {
                    Settings.setDoEnableIds((Boolean) value);
                }

                case "doEnablePaper" -> {
                    Settings.setDoEnablePaper((Boolean) value);
                }

                case "renderDistance" -> {
                    Settings.setRenderDistance((Integer) value);
                }

                case "unloadMargin" -> {
                    Settings.setUnloadMargin((Integer) value);
                }

                case "fov" -> {
                    Settings.setFov((Float) value);
                }

                case "maxInteractionDistance" -> {
                    Settings.setMaxInteractionDistance((Float) value);
                }
            }
        } catch (ClassCastException e) {
            ToastFactory.error(String.format("Invalid value for %s: %s", rule, value));
        }
    }
}
