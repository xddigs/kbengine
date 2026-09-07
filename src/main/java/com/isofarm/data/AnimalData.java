package com.isofarm.data;

import com.isofarm.item.Craftable;
import com.isofarm.item.Material;

/**
 * Enumerates the supported animal data values.
 */
@DataClass
@TODO(reason="Models, lack there of, are not complete")
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
