package org.kbeng.data;

/**
 * Holds the cause of death of the player
 */
@DataClass
public enum Cause {
    NULL((byte) 0),
    ENTITY((byte) 1),
    SELF((byte) 2),
    BURN((byte) 3),
    DROWN((byte) 4),
    FALL((byte) 5),
    VOID((byte) 6),
    STARVATION((byte) 7);

    private final byte id;

    Cause(byte id) {
        this.id = id;
    }

    /**
     * Returns the id of the cause of death
     * @return {@link Byte} the id
     */
    public byte getId() {
        return id;
    }

    /**
     * Returns the cause of death from the id
     * @param id the {@code byte} supplied as {@code id}
     * @return {@link Cause} the cause of death
     */
    public static Cause fromId(byte id) {
        for (Cause cause : values()) {
            if (cause.getId() == id) {
                return cause;
            }
        }
        return null;
    }
}
