package org.kbeng.engine.data;

import org.kbeng.games.rpg.data.DataClass;

/**
 * ToastData declares the canonical toast data set for the data subsystem.
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 * The data type is used as a normalized schema for transport, persistence, and runtime inspection.
 */
@DataClass
public enum ToastData {
    SUCCESS((byte) 0, "Success"),
    INFO((byte) 1, "Info"),
    WARNING((byte) 2, "Warning"),
    ERROR((byte) 3, "Error"),
    REWARD((byte) 4, "Reward"),
    PURCHASE((byte) 5, "Purchase"),
    SELL((byte) 6, "Sell");

    private final byte id;
    private final String title;

    /**
     * Creates a new {@code ToastData} instance.
     * @param id the {@code byte} supplied as {@code id}
     * @param title the {@link String} supplied as {@code title}
     */
    ToastData(byte id, String title) {
        this.id = id;
        this.title = title;
    }

    /**
     * Returns the id.
     * @return {@code byte}; the id
     */
    public byte getId() {
        return id;
    }

    /**
     * Returns the title.
     * @return the {@link String} representing the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Creates or returns from id from the supplied arguments.
     * @param id the {@code byte} supplied as {@code id}
     * @return the {@link ToastData} representing the from id result
     */
    public static ToastData fromId(byte id) {
        for (ToastData data : values()) {
            if (data.getId() == id) {
                return data;
            }
        }
        return null;
    }
}
