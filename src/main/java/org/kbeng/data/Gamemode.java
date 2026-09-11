package org.kbeng.data;

import java.util.Locale;

/**
 * Enumerates the supported gamemode values.
 */
@DataClass
public enum Gamemode {
    SURVIVAL, GODMODE, NO_CLIP;

    private final byte id;

    /**
     * Creates a new {@code Gamemode} instance.
     */
    Gamemode() {
        this.id = (byte) ordinal();
    }

    /**
     * Returns the id.
     * @return {@code byte}; the id
     */
    public byte getId() {
        return id;
    }

    /**
     * Returns the name.
     * @return the {@link String} result; {@code String} the name
     */
    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Checks whether the survival condition is met.
     * @return {@code true} if survival; otherwise {@code false}
     */
    public boolean isSurvival() {
        return this.equals(SURVIVAL);
    }

    /**
     * Checks whether the godmode condition is met.
     * @return {@code true} if godmode; otherwise {@code false}
     */
    public boolean isGodmode() {
        return this.equals(GODMODE);
    }

    /**
     * Checks whether the no clip condition is met.
     * @return {@code true} if no clip; otherwise {@code false}
     */
    public boolean isNoClip() {
        return this.equals(NO_CLIP);
    }

    /**
     * Creates or returns from string from the supplied arguments.
     * @param text the {@link String} supplied as {@code text}
     * @return the {@link Gamemode} representing the from string result
     */
    public static Gamemode fromString(String text) {
        if (text == null) return null;
        for (Gamemode mode : values()) {
            if (mode.name().toLowerCase().replace("_", "")
                    .equalsIgnoreCase(text.replace("_", ""))) {
                return mode;
            }
        }
        return null;
    }
}
