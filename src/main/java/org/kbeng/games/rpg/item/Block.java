package org.kbeng.games.rpg.item;

import org.kbeng.games.rpg.data.BlockData;
import org.kbeng.games.rpg.data.BlockPos;
import org.kbeng.games.rpg.data.BlockShape;
import org.kbeng.games.rpg.data.DataClass;
import org.kbeng.engine.utils.K;

/**
 * Block provides block capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It implements Craftable, providing a concrete strategy for this subsystem contract.
 */
@DataClass
public class Block implements Craftable {
    private static final int VOXEL_SIZE = 4;
    private final byte[][][] voxels = new byte[VOXEL_SIZE][VOXEL_SIZE][VOXEL_SIZE];
    private final int waterLevelMax = K.World.WATER_LEVEL_MAX;
    private final byte id;
    private final String name;
    private final int value;
    private BlockData type;
    private BlockShape shape;
    private int x, y, z;
    private int waterLevel = 15;
    private boolean isInteractive;

    /**
     * Creates a new {@code Block} instance.
     * @param type the {@link BlockData} supplied as {@code type}
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     */
    public Block(BlockData type, int x, int y, int z) {
        this.id = type.getId();
        this.name = type.getDisplayName();
        this.value = type.getValue();
        this.type = type;
        this.shape = type.getShape();
        this.x = x;
        this.y = y;
        this.z = z;
        this.isInteractive = false;
    }

    /**
     * Creates a new {@code Block} instance.
     * @param type the {@link BlockData} supplied as {@code type}
     * @param pos the {@link BlockPos} supplied as {@code pos}
     */
    public Block(BlockData type, BlockPos pos) {
        this(type, pos.x(), 0, pos.y());
    }

    /**
     * Creates a new {@code Block} instance.
     * @param type the {@link BlockData} supplied as {@code type}
     */
    public Block(BlockData type) {
        this.id = type.getId();
        this.name = type.getDisplayName();
        this.value = type.getValue();
        this.type = type;
        this.shape = type.getShape();
    }

    /**
     * Creates a new {@code Block} instance.
     */
    public Block() {
        this(BlockData.DIRT);
    }

    /**
     * {@inheritDoc}
     * Returns the id.
     * @return {@code byte}; the id
     */
    @Override
    public byte getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     * Returns the name.
     * @return the {@link String} representing the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    @Override
    public String getDisplayName() {
        return type.getDisplayName();
    }

    /**
     * {@inheritDoc}
     * Returns the value.
     * @return {@code int}; the value
     */
    @Override
    public int getValue() {
        return value;
    }

    /**
     * Returns the {@code voxels} value
     * @return {@link byte[][][]} value of voxels
     */
    public byte[][][] getVoxels() {
        return voxels;
    }

    /**
     * Initializes the voxels for breaking.
     */
    public void initVoxels() {
        for (int x = 0; x < VOXEL_SIZE; x++) {
            for (int y = 0; y < VOXEL_SIZE; y++) {
                for (int z = 0; z < VOXEL_SIZE; z++) {
                    voxels[x][y][z] = 1;
                }
            }
        }
    }

    /**
     * Updates the breaking progress.
     * @param progress the {@code float} supplied as {@code progress}
     */
    public void updateBreakingProgress(float progress) {
        int totalVoxels = VOXEL_SIZE * VOXEL_SIZE * VOXEL_SIZE;
        int voxelsToRemove = Math.min(totalVoxels, (int) (progress * totalVoxels));
        int removedCount = 0;
        for (int y = VOXEL_SIZE - 1; y >= 0; y--) {
            for (int x = 0; x < VOXEL_SIZE; x++) {
                for (int z = 0; z < VOXEL_SIZE; z++) {
                    if (removedCount < voxelsToRemove) {
                        voxels[x][y][z] = 0;
                        removedCount++;
                    } else {
                        voxels[x][y][z] = 1;
                    }
                }
            }
        }
    }

    /**
     * Checks whether the voxel at the specified coordinates is solid
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return {@code true} if voxel solid; otherwise {@code false}
     */
    public boolean isVoxelSolid(int x, int y, int z) {
        if (x < 0 || x >= VOXEL_SIZE || y < 0 || y >= VOXEL_SIZE || z < 0 || z >= VOXEL_SIZE) {
            return false;
        }
        return voxels[x][y][z] != 0;
    }

    /**
     * Returns the type.
     * @return the {@link BlockData} representing the type
     */
    public BlockData getType() {
        return type;
    }

    /**
     * Sets the type.
     * @param type the {@link BlockData} supplied as {@code type}
     */
    public void setType(BlockData type) {
        this.type = type;
        this.shape = type.getShape();
    }

    public BlockShape getShape() {
        return shape == null ? type.getShape() : shape;
    }

    public void setShape(BlockShape shape) {
        this.shape = shape == null ? type.getShape() : shape;
    }

    /**
     * Returns the x.
     * @return {@code int}; the x
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the y.
     * @return {@code int}; the y
     */
    public int getY() {
        return y;
    }

    /**
     * Returns the z.
     * @return {@code int}; the z
     */
    public int getZ() {
        return z;
    }

    /**
     * Returns the water level max.
     * @return {@code int}; the water level max
     */
    public int getWaterLevelMax() {
        return waterLevelMax;
    }

    /**
     * Returns the water level.
     * @return {@code int}; the water level
     */
    public int getWaterLevel() {
        return waterLevel;
    }

    /**
     * Adds the water.
     * @param amount the {@code int} supplied as {@code amount}
     */
    public void addWater(int amount) {
        waterLevel = Math.min(waterLevelMax, waterLevel + amount);
    }

    /**
     * Sets the water level.
     * @param waterLevel the {@code int} supplied as {@code waterLevel}
     */
    public void setWaterLevel(int waterLevel) {
        this.waterLevel = waterLevel;
    }

    /**
     * Checks whether the water condition is met.
     * @return {@code true} if water; otherwise {@code false}
     */
    public boolean hasWater() {
        return waterLevel > 0;
    }

    /**
     * Checks whether the interactive condition is met.
     * @return {@code true} if interactive; otherwise {@code false}
     */
    public boolean isInteractive() {
        return isInteractive;
    }

    /**
     * Sets the isInteractive value
     * @param interactive the {@code boolean} supplied as {@code interactive}
     */
    public void setInteractive(boolean interactive) {
        isInteractive = interactive;
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        Block copy = new Block(type, x, y, z);
        copy.setShape(getShape());
        return copy;
    }
}
