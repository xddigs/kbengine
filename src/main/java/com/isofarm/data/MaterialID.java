package com.isofarm.data;

import com.isofarm.item.Craftable;
import com.isofarm.item.Item;
import com.isofarm.utils.Local;

import java.util.Locale;

/**
 * Enumerates the supported material id values.
 */
@DataClass
public enum MaterialID implements Craftable {
    RAW_ORE((byte) 0, (byte) 0, 15),
    INGOT((byte) 1, (byte) 0, 50),
    CHARCOAL((byte) 0, (byte) 1, 15),
    STICK((byte) 1, (byte) 1, 1),
    PAPER((byte) 2, (byte) 1, 10),
    LEATHER((byte) 3, (byte) 1, 40),
    SUGAR_CANE((byte) 4, (byte) 1, 10),
    SUGAR((byte) 5, (byte) 1, 20);

    private final byte id;
    private final byte row;
    private final int value;

    /**
     * Creates a new {@code MaterialID} instance.
     * @param id the {@code byte} supplied as {@code id}
     * @param row the {@code byte} supplied as {@code row}
     * @param value the {@code int} supplied as {@code value}
     */
    MaterialID(byte id, byte row, int value) {
        this.id = id;
        this.row = row;
        this.value = value;
    }

    /**
     * Returns the id.
     * @return {@code byte}; the id
     */
    public byte getId() { return id; }
    /**
     * Returns the row.
     * @return {@code byte}; the row
     */
    public byte getRow() { return row; }

    /**
     * {@inheritDoc}
     * Returns the name.
     * @return the {@link String} representing the name
     */
    @Override
    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * {@inheritDoc}
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    @Override
    public String getDisplayName() {
        return Local.lang.t("item.material." + name().toLowerCase(Locale.ROOT));
    }

    /**
     * {@inheritDoc}
     * Returns the value.
     * @return {@code int}; the value
     */
    @Override
    public int getValue() { return value; }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return this;
    }
}