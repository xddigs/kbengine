package org.kbeng.rpg.data;

import org.kbeng.rpg.item.Craftable;
import org.kbeng.rpg.item.Material;
import org.kbeng.engine.utils.Local;

import java.util.Locale;

/**
 * AnimalData declares the canonical animal data set for the data subsystem.
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 * The data type is used as a normalized schema for transport, persistence, and runtime inspection.
 */
@DataClass
@Task(reason="Models, lack there of, are not complete")
public enum AnimalData {
    COW((byte) 0, "assets/models/animals/cow.gltf", new Material[]{new Material(Tier.NONE, MaterialID.LEATHER)}),
    CHICKEN((byte) 1, "assets/models/animals/chicken.gltf", new Material[]{});

    private final byte id;
    private final String modelPath;
    private final Craftable[] drops;

    /** Creates a new {@code AnimalData} instance. */
    AnimalData(byte id, String modelPath, Material[] drops) {
        this.id = id;
        this.modelPath = modelPath;
        this.drops = drops;
    }

    /**
     * Retrieves {@code id}
     * @return {@link byte} value of id
     */
    public byte getId() {
        return id;
    }

    public String getDisplayName() {
        return Local.lang.t("animal." + name().toLowerCase(Locale.ROOT));
    }

    /**
     * Retrieves {@code modelPath}
     * @return {@link String} value of modelPath
     */
    public String getModelPath() {
        return modelPath;
    }

    /**
     * Retrieves {@code drops}
     * @return {@link Craftable[]} value of drops
     */
    public Craftable[] getDrops() {
        return drops;
    }
}
