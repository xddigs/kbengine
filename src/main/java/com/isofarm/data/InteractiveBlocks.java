package com.isofarm.data;

import com.isofarm.utils.Local;

import java.util.Locale;

/**
 * Enumerates the supported interactive blocks values.
 */
@DataClass
public enum InteractiveBlocks implements Blockable {
    EMPTY(null, null, (byte) 0, (byte) -1, (byte) -1, -1, 0.0f, 0),
    CHEST("assets/models/blocks/chest.gltf", SoundGroup.CHEST, (byte) 1, (byte) 8, (byte) 1, 10, 2.5f, 1),
    OAK_DOOR("assets/models/blocks/oak_door.gltf", SoundGroup.DOOR, (byte) 2, (byte) 6, (byte) 1, 15, 2.0f, 2),
    SPRUCE_DOOR("assets/models/blocks/spruce_door.gltf", SoundGroup.DOOR, (byte) 3, (byte) 6, (byte) 2, 15, 2.0f, 2);

    private final String modelPath;
    private final SoundGroup soundGroup;
    private final byte id;
    private final byte col;
    private final byte row;
    private final int value;
    private final float destroyTime;
    private final int height;

    InteractiveBlocks(String modelPath, SoundGroup soundGroup, byte id, byte col, byte row, int value,
                      float destroyTime, int height) {
        this.modelPath = modelPath;
        this.soundGroup = soundGroup;
        this.id = id;
        this.col = col;
        this.row = row;
        this.value = value;
        this.destroyTime = destroyTime;
        this.height = height;
    }

    /**
     * Returns the matching {@link InteractiveBlocks} {@code primaryMat} with the corresponding door
     * @param primaryMat {@link BlockData} supplied as {@code primary material}
     * @return {@link InteractiveBlocks} the matching door
     */
    public static InteractiveBlocks toDoor(BlockData primaryMat) {
        return switch (primaryMat) {
            case OAK_PLANK -> OAK_DOOR;
            case SPRUCE_PLANK -> SPRUCE_DOOR;
            default -> throw new IllegalStateException("Unknown BlockData " + primaryMat);
        };
    }

    /**
     * Returns the name of the block.
     * @return {@link String} the name of the block
     */
    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the display name of the block.
     * @return {@link String} the display of the block
     */
    public String getDisplayName() {
        return Local.lang.t("block." + name().toLowerCase(Locale.ROOT));
    }

    /**
     * Returns the GLTF model path of the block.
     * @return the {@link String} representing the model path, or {@code null} when the type has no model
     */
    public String getModelPath() {
        return modelPath;
    }

    /**
     * Returns the sound group of the block.
     * @return {@link SoundGroup} the sound group of the block
     */
    public SoundGroup getSoundGroup() {
        return soundGroup;
    }

    /**
     * Returns the id of the block.
     * @return {@link Byte} the id of the block
     */
    public byte getId() {
        return id;
    }

    /**
     * Returns the col of the block.
     * @return {@link Byte} the col of the block
     */
    public byte getCol() {
        return col;
    }

    /**
     * Returns the row of the block.
     * @return {@link Byte} the row of the block
     */
    public byte getRow() {
        return row;
    }

    /**
     * Returns the value of the block.
     * @return {@link Integer} the value of the block
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns the time required to destroy the block.
     *
     * @return {@code float}; the destroy time in seconds
     */
    public float getDestroyTime() {
        return destroyTime;
    }

    /**
     * Returns how many vertical world cells this block occupies.
     *
     * @return the occupied height in blocks
     */
    public int getHeight() {
        return height;
    }

    /** Returns whether this interactive block is a door. */
    public boolean isDoor() {
        return this == OAK_DOOR || this == SPRUCE_DOOR;
    }
}
