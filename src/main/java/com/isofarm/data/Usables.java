package com.isofarm.data;

import com.isofarm.utils.Local;

import java.util.Locale;

/**
 * Enumerates the supported usables values.
 */
@DataClass
public enum Usables {
    BACKPACK((byte) 0, (byte) 0, (byte) 0, 500),
    BOOK((byte) 1, (byte) 1, (byte) 0, 100),
    CRAFTING_BOOK((byte) 2, (byte) 2, (byte) 0, 200),
    BUCKET((byte) 3, (byte) 3, (byte) 0, 10),
    WALLET((byte) 4, (byte) 6, (byte) 0, 10);

    private final byte id;
    private final byte col;
    private final byte row;
    private final int value;

    /**
     * Creates a new {@code Usables} instance.
     * @param id the {@code byte} supplied as {@code id}
     * @param col the {@code byte} supplied as {@code col}
     * @param row the {@code byte} supplied as {@code row}
     * @param value the {@code int} supplied as {@code value}
     */
    Usables(byte id, byte col, byte row, int value) {
        this.id = id;
        this.col = col;
        this.row = row;
        this.value = value;
    }

    /**
     * Returns the id.
     * @return {@code byte}; the id
     */
    public byte getId() {
        return id;
    }

    /**
     * Returns the col.
     * @return {@code byte}; the col
     */
    public byte getCol() {
        return col;
    }

    /**
     * Returns the row.
     * @return {@code byte}; the row
     */
    public byte getRow() {
        return row;
    }

    /**
     * Returns the name.
     * @return the {@link String} representing the name
     */
    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    public String getDisplayName() {
        return Local.lang.t("item.usable." + getName());
    }

    /**
     * Returns the value.
     * @return {@code int}; the value
     */
    public int getValue() {
        return value;
    }
}
