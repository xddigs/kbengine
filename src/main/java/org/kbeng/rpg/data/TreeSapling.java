package org.kbeng.rpg.data;

/**
 * TreeSapling provides tree sapling capabilities within the data subsystem.
 * It represents strongly typed domain data shared between simulation, rendering, input, and persistence boundaries.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public class TreeSapling {
    private final int x, y, z;
    private final BlockData treeType;
    private int currentTicks;
    private int targetTicks;

    /**
     * Creates a new {@code TreeSapling} instance.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @param treeType the {@link BlockData} supplied as {@code treeType}
     * @param targetTicks the {@code int} supplied as {@code targetTicks}
     */
    public TreeSapling(int x, int y, int z, BlockData treeType, int targetTicks) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.treeType = treeType;
        this.currentTicks = 0;
        this.targetTicks = targetTicks;
    }

    /**
     * Updates this object for the current simulation step.
     * @return {@code boolean}; the tick result
     */
    public boolean tick() {
        currentTicks++;
        return currentTicks >= targetTicks;
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
     * Returns the tree type.
     * @return the {@link BlockData} representing the tree type
     */
    public BlockData getTreeType() {
        return treeType;
    }
}
