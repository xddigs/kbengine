package com.isofarm.data;

import com.isofarm.entity.NPC;

/**
 * Enumerates the available {@link NPC} jobs, such as Farmer, Miner, etc.
 */
public enum Job {
    FARMER((byte) 0),
    MINER((byte) 1),
    TRADER((byte) 2),;

    private final byte id;

    /** Creates a new {@code Job} instance. */
    Job(byte id) {
        this.id = id;
    }

    /**
     * Retrieves id
     * @return {@link byte} value of id
     */
    public byte getId() {
        return id;
    }
}
