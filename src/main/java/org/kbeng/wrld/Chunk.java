package org.kbeng.wrld;

import org.kbeng.data.BlockData;
import org.kbeng.data.DataClass;

import java.util.BitSet;

/**
 * Chunk provides chunk capabilities within the wrld subsystem.
 * It maintains world simulation concerns including terrain, chunks, fluids, and authoritative spatial state.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public class Chunk {
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 256;
    public static final int SIZE_Z = 16;

    private final int chunkX;
    private final int chunkZ;
    private final byte[] blocks;
    private final byte[] waterLevels;
    private final BitSet generatedOceanWater;

    private int[] plantIndices;
    private boolean plantCacheDirty = true;

    /**
     * Creates a new {@code Chunk} instance.
     * @param chunkX the {@code int} supplied as {@code chunkX}
     * @param chunkZ the {@code int} supplied as {@code chunkZ}
     */
    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;

        int size = SIZE_X * SIZE_Y * SIZE_Z;

        this.blocks = new byte[size];
        this.waterLevels = new byte[size];
        this.generatedOceanWater = new BitSet(size);
    }

    /**
     * Returns the block.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code byte}; the block
     */
    public byte getBlock(int x, int y, int z) {
        if (!isOutOfBounds(x, y, z)) {
            return blocks[getIndex(x, y, z)];
        }

        return 0;
    }

    /**
     * Sets the block.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @param blockId the {@code byte} supplied as {@code blockId}
     */
    public void setBlock(int x, int y, int z, byte blockId) {
        if (isOutOfBounds(x, y, z)) {
            return;
        }

        int index = getIndex(x, y, z);
        byte oldBlockId = blocks[index];
        blocks[index] = blockId;
        if (blockId != BlockData.WATER.getId()) {
            generatedOceanWater.clear(index);
        }
        boolean oldWasPlant = isPlant(oldBlockId);
        boolean newIsPlant = isPlant(blockId);
        if (oldWasPlant || newIsPlant) {
            plantCacheDirty = true;
        }
    }

    /**
     * Checks whether the plant condition is met.
     * @param blockId the {@code byte} supplied as {@code blockId}
     * @return {@code true} if plant; otherwise {@code false}
     */
    private boolean isPlant(byte blockId) {
        BlockData data = BlockData.fromId(blockId);
        return data != null && data.isPlant();
    }

    /**
     * Returns the water level.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code byte}; the water level
     */
    public byte getWaterLevel(int x, int y, int z) {
        if (!isOutOfBounds(x, y, z)) {
            return waterLevels[getIndex(x, y, z)];
        }

        return 0;
    }

    /**
     * Returns the fluid level stored at a local chunk position.
     * @param x the {@code int} argument; the local x value
     * @param y the {@code int} argument; the local y value
     * @param z the {@code int} argument; the local z value
     * @return {@code byte}; the stored fluid level
     */
    public byte getFluidLevel(int x, int y, int z) {
        return getWaterLevel(x, y, z);
    }

    /**
     * Sets the water level.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @param waterLevel the {@code byte} supplied as {@code waterLevel}
     */
    public void setWaterLevel(int x, int y, int z, byte waterLevel) {
        if (!isOutOfBounds(x, y, z)) {
            waterLevels[getIndex(x, y, z)] = waterLevel;
        }
    }

    /**
     * Sets the fluid level at a local chunk position.
     * @param x the {@code int} argument; the local x value
     * @param y the {@code int} argument; the local y value
     * @param z the {@code int} argument; the local z value
     * @param fluidLevel the {@code byte} supplied as {@code fluidLevel}
     */
    public void setFluidLevel(int x, int y, int z, byte fluidLevel) {
        setWaterLevel(x, y, z, fluidLevel);
    }

    /** Marks a cell as ocean water created by the terrain generator. */
    public void setGeneratedOceanWater(int x, int y, int z, boolean generated) {
        if (!isOutOfBounds(x, y, z)) {
            generatedOceanWater.set(getIndex(x, y, z), generated);
        }
    }

    /** Returns whether the cell is generated ocean rather than lake or placed water. */
    public boolean isGeneratedOceanWater(int x, int y, int z) {
        return !isOutOfBounds(x, y, z) && generatedOceanWater.get(getIndex(x, y, z));
    }

    /** Returns whether this horizontal layer contains any generated ocean water. */
    public boolean hasGeneratedOceanWaterAtY(int y) {
        if (y < 0 || y >= SIZE_Y) return false;
        int start = y * SIZE_X * SIZE_Z;
        int next = generatedOceanWater.nextSetBit(start);
        return next >= start && next < start + SIZE_X * SIZE_Z;
    }

    /**
     * Returns the blocks.
     * @return an array of {@code byte} values; the blocks
     */
    public byte[] getBlocks() {
        return blocks;
    }

    /**
     * Returns the water levels.
     * @return an array of {@code byte} values; the water levels
     */
    public byte[] getWaterLevels() {
        return waterLevels;
    }

    /**
     * Returns the chunk x.
     * @return {@code int}; the chunk x
     */
    public int getChunkX() {
        return chunkX;
    }

    /**
     * Returns the chunk z.
     * @return {@code int}; the chunk z
     */
    public int getChunkZ() {
        return chunkZ;
    }

    /**
     * Returns the plant indices.
     * @return an array of {@code int} values; the plant indices
     */
    public int[] getPlantIndices() {
        if (!plantCacheDirty && plantIndices != null) {
            return plantIndices;
        }

        int[] temp = new int[64];
        int count = 0;

        for (int index = 0; index < blocks.length; index++) {
            byte blockId = blocks[index];

            if (blockId == BlockData.AIR.getId()) {
                continue;
            }

            BlockData data = BlockData.fromId(blockId);
            if (data == null || !data.isPlant()) {
                continue;
            }

            if (count >= temp.length) {
                int[] expanded = new int[temp.length * 2];
                System.arraycopy(temp, 0, expanded, 0, temp.length);
                temp = expanded;
            }

            temp[count++] = index;
        }

        plantIndices = new int[count];
        System.arraycopy(temp, 0, plantIndices, 0, count);
        plantCacheDirty = false;
        return plantIndices;
    }

    /**
     * Calculates the value represented by index to x from the current state.
     * @param index the {@code int} supplied as {@code index}
     * @return {@code int}; the index to x result
     */
    public static int indexToX(int index) {
        return index % SIZE_X;
    }

    /**
     * Calculates the value represented by index to z from the current state.
     * @param index the {@code int} supplied as {@code index}
     * @return {@code int}; the index to z result
     */
    public static int indexToZ(int index) {
        return (index / SIZE_X) % SIZE_Z;
    }

    /**
     * Calculates the value represented by index to y from the current state.
     * @param index the {@code int} supplied as {@code index}
     * @return {@code int}; the index to y result
     */
    public static int indexToY(int index) {
        return index / (SIZE_X * SIZE_Z);
    }

    /**
     * Returns the index.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code int}; the index
     */
    private int getIndex(int x, int y, int z) {
        return x + SIZE_X * (z + SIZE_Z * y);
    }

    /**
     * Checks whether the out of bounds condition is met.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code true} if out of bounds; otherwise {@code false}
     */
    private boolean isOutOfBounds(int x, int y, int z) {
        return x < 0 || x >= SIZE_X ||
                y < 0 || y >= SIZE_Y ||
                z < 0 || z >= SIZE_Z;
    }
}
