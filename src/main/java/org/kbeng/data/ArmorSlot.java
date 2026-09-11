package org.kbeng.data;

/**
 * Enumerates the available armor slots.
 */
@DataClass
public enum ArmorSlot {
    HEAD((byte) 0, ArmorData.HELMET),
    CHEST((byte) 1, ArmorData.CHESTPLATE ),
    FEET((byte) 2, ArmorData.BOOTS);

    private final byte id;
    private final ArmorData equippable;

    ArmorSlot(byte id, ArmorData equippable) {
        this.id = id;
        this.equippable = equippable;
    }

    /**
     * Returns the {@code id} value
     * @return {@link byte} value of id
     */
    public byte getId() {
        return id;
    }

    /**
     * Returns the {@code equippable} value
     * @return {@link ArmorData} value of equippable
     */
    public ArmorData getEquippable() {
        return equippable;
    }
}
