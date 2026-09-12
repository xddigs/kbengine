package org.kbeng.input;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.kbeng.data.Singleton;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ControlConfigParser provides control config parser capabilities within the input subsystem.
 *
 * It defines device mappings, control-state tracking, and interaction orchestration for player actions.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@Singleton
public final class ControlConfigParser {
    private static final Logger log = LoggerFactory.getLogger(ControlConfigParser.class);
    private static final String RESOURCE = "/config.json";
    private static final Map<String, Integer> GLFW_CONSTANTS = discoverGlfwConstants();
    public static final ControlConfigParser controls = new ControlConfigParser();

    private volatile Bindings bindings;

    private ControlConfigParser() {
        bindings = loadBindings();
    }

    /**
     * Reloads all bindings from the classpath resource.
     */
    public synchronized void reload() {
        bindings = loadBindings();
    }

    /**
     * Returns keyboard codes according to the current object state.
     * @return an array of {@code int} values; all keyboard codes assigned to the action
     */
    public int[] getKeyboardCodes(ControlAction action) {
        return copy(bindings.keyboard().get(action.id()));
    }

    /**
     * Returns mouse codes according to the current object state.
     * @return an array of {@code int} values; all mouse button codes assigned to the action
     */
    public int[] getMouseCodes(ControlAction action) {
        return copy(bindings.mouse().get(action.id()));
    }

    /**
     * Returns joystick button codes according to the current object state.
     * @return an array of {@code int} values; all gamepad button codes assigned to the action
     */
    public int[] getJoystickButtonCodes(ControlAction action) {
        return copy(bindings.joystickButtons().get(action.id()));
    }

    /**
     * Returns joystick axis according to the current object state.
     * @return gamepad axis assigned to the action, or {@code -1} when unbound
     */
    public int getJoystickAxis(ControlAction action) {
        return bindings.joystickAxes().getOrDefault(action.id(), -1);
    }

    /**
     * Returns preferred joystick according to the current object state.
     * @return configured joystick, or {@code -1} to select the first available gamepad
     */
    public int getPreferredJoystick() {
        return bindings.preferredJoystick();
    }

    /**
     * Returns joystick dead zone according to the current object state.
     * @return {@code float}; configured gamepad axis dead zone
     */
    public float getJoystickDeadZone() {
        return bindings.joystickDeadZone();
    }

    private Bindings loadBindings() {
        try (InputStream stream = ControlConfigParser.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Control configuration not found: " + RESOURCE);
            }

            JsonElement document = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!document.isJsonObject()) {
                throw new IllegalStateException("Control configuration root must be a JSON object");
            }

            JsonObject root = document.getAsJsonObject();
            Map<String, int[]> keyboard = readBindings(root, "keyboard", BindingType.KEYBOARD);
            Map<String, int[]> mouse = readBindings(root, "mouse", BindingType.MOUSE);

            JsonObject joystick = requiredObject(root, "joystick");
            Map<String, int[]> joystickButtons = readBindings(
                    joystick, "buttons", BindingType.JOYSTICK_BUTTON);
            Map<String, Integer> joystickAxes = readAxes(joystick);
            int preferredJoystick = readPreferredJoystick(joystick);
            float deadZone = readDeadZone(joystick);

            log.info("Loaded {} keyboard, {} mouse, {} gamepad-button and {} gamepad-axis bindings",
                    keyboard.size(), mouse.size(), joystickButtons.size(), joystickAxes.size());
            return new Bindings(Map.copyOf(keyboard), Map.copyOf(mouse),
                    Map.copyOf(joystickButtons), Map.copyOf(joystickAxes),
                    preferredJoystick, deadZone);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + RESOURCE, exception);
        }
    }

    private Map<String, int[]> readBindings(JsonObject parent, String member, BindingType type) {
        JsonObject object = requiredObject(parent, member);
        Map<String, int[]> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String action = normalizeAction(entry.getKey());
            JsonElement value = entry.getValue();
            int[] codes;
            if (value.isJsonArray()) {
                codes = new int[value.getAsJsonArray().size()];
                for (int index = 0; index < codes.length; index++) {
                    codes[index] = resolve(value.getAsJsonArray().get(index).getAsString(), type);
                }
            } else {
                codes = new int[]{resolve(value.getAsString(), type)};
            }
            result.put(action, codes);
        }
        return result;
    }

    private Map<String, Integer> readAxes(JsonObject joystick) {
        JsonObject axes = requiredObject(joystick, "axes");
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : axes.entrySet()) {
            result.put(normalizeAction(entry.getKey()),
                    resolve(entry.getValue().getAsString(), BindingType.JOYSTICK_AXIS));
        }
        return result;
    }

    private int readPreferredJoystick(JsonObject joystick) {
        String value = joystick.has("preferred")
                ? joystick.get("preferred").getAsString().trim().toUpperCase(Locale.ROOT)
                : "AUTO";
        return "AUTO".equals(value) ? -1 : resolve(value, BindingType.JOYSTICK);
    }

    private float readDeadZone(JsonObject joystick) {
        float deadZone = joystick.has("dead_zone")
                ? joystick.get("dead_zone").getAsFloat()
                : 0.2f;
        if (!Float.isFinite(deadZone) || deadZone < 0.0f || deadZone > 1.0f) {
            throw new IllegalArgumentException("joystick.dead_zone must be between 0 and 1");
        }
        return deadZone;
    }

    private int resolve(String configuredName, BindingType type) {
        String name = type.canonicalName(configuredName);
        Integer value = GLFW_CONSTANTS.get(name);
        if (value == null || !name.startsWith(type.prefix)) {
            throw new IllegalArgumentException("Unknown " + type.description
                    + " constant: " + configuredName);
        }
        if (value < type.minimum || value > type.maximum) {
            throw new IllegalArgumentException("GLFW constant is outside the valid "
                    + type.description + " range: " + name);
        }
        return value;
    }

    private static JsonObject requiredObject(JsonObject parent, String member) {
        JsonElement element = parent.get(member);
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("Missing JSON object: " + member);
        }
        return element.getAsJsonObject();
    }

    private static String normalizeAction(String action) {
        String normalized = action.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Control action names cannot be empty");
        }
        return normalized;
    }

    private static int[] copy(int[] values) {
        return values == null ? new int[0] : values.clone();
    }

    private static Map<String, Integer> discoverGlfwConstants() {
        Map<String, Integer> constants = new HashMap<>();
        for (Field field : GLFW.class.getFields()) {
            if (field.getType() != int.class || !Modifier.isStatic(field.getModifiers())) continue;
            try {
                constants.put(field.getName(), field.getInt(null));
            } catch (IllegalAccessException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }
        return Map.copyOf(constants);
    }

    /** Parsed control bindings grouped by input device. */
    private record Bindings(
            Map<String, int[]> keyboard,
            Map<String, int[]> mouse,
            Map<String, int[]> joystickButtons,
            Map<String, Integer> joystickAxes,
            int preferredJoystick,
            float joystickDeadZone) {}

    /** Supported control-device namespaces used by the configuration parser. */
    private enum BindingType {
        KEYBOARD("GLFW_KEY_", "keyboard key", GLFW.GLFW_KEY_UNKNOWN, GLFW.GLFW_KEY_LAST),
        MOUSE("GLFW_MOUSE_BUTTON_", "mouse button", GLFW.GLFW_MOUSE_BUTTON_1,
                GLFW.GLFW_MOUSE_BUTTON_LAST),
        JOYSTICK_BUTTON("GLFW_GAMEPAD_BUTTON_", "gamepad button",
                GLFW.GLFW_GAMEPAD_BUTTON_A, GLFW.GLFW_GAMEPAD_BUTTON_LAST),
        JOYSTICK_AXIS("GLFW_GAMEPAD_AXIS_", "gamepad axis",
                GLFW.GLFW_GAMEPAD_AXIS_LEFT_X, GLFW.GLFW_GAMEPAD_AXIS_LAST),
        JOYSTICK("GLFW_JOYSTICK_", "joystick", GLFW.GLFW_JOYSTICK_1,
                GLFW.GLFW_JOYSTICK_LAST);

        private final String prefix;
        private final String description;
        private final int minimum;
        private final int maximum;

        BindingType(String prefix, String description, int minimum, int maximum) {
            this.prefix = prefix;
            this.description = description;
            this.minimum = minimum;
            this.maximum = maximum;
        }

        private String canonicalName(String value) {
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            if (normalized.startsWith("GLFW_")) return normalized;
            if (normalized.startsWith(prefix.substring("GLFW_".length()))) {
                return "GLFW_" + normalized;
            }
            return prefix + normalized;
        }
    }
}
