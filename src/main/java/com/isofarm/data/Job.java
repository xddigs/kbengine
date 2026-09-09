package com.isofarm.data;

import com.isofarm.entity.NPC;
import com.isofarm.utils.Local;

import java.util.Locale;

/**
 * Enumerates the available {@link NPC} jobs, such as Farmer, Miner, etc.
 */
@DataClass
public enum Job {
    FARMER((byte) 0, "assets/models/npcs/farmer.gltf"),
    TRADER((byte) 1, "assets/models/npcs/trader.gltf");

    private final byte id;
    private final String modelPath;

    /**
     * Creates a job registration.
     *
     * @param id the stable serialized identifier
     * @param modelPath the classpath path of the NPC GLTF model
     */
    Job(byte id, String modelPath) {
        this.id = id;
        this.modelPath = modelPath;
    }

    /**
     * Retrieves id
     * @return {@link byte} value of id
     */
    public byte getId() {
        return id;
    }

    /**
     * Retrieves {@code modelPath}
     * @return {@link String} value of modelPath
     */
    public String getModelPath() {
        return modelPath;
    }

    /**
     * Returns the name in lowercase
     * @return {@link String} representing the name
     */
    public String getName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the display name, localized
     * @return {@link String} representing the display name
     */
    public String getDisplayName() {
        return Local.lang.t("npc.job." + getName());
    }

    /**
     * Retrieves {@code Job} from id
     * @param id the {@code byte} supplied as {@code id}
     * @return {@link Job} representing the from {@code id} result
     */
    public static Job fromID(byte id) {
        for (Job job : values()) {
            if (job.id == id) {
                return job;
            }
        }
        return null;
    }
}
