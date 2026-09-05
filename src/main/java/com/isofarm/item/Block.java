package com.isofarm.item;

import com.isofarm.data.BlockData;
import com.isofarm.data.BlockPos;
import com.isofarm.data.BlockShape;
import com.isofarm.data.DataClass;
import com.isofarm.utils.K;

/**
 * Encapsulates the state and operations required by block within the game runtime.
 */
@DataClass
public class Block implements Craftable {
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
