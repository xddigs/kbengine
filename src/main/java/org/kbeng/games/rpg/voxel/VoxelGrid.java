package org.kbeng.games.rpg.voxel;

/** Coordinate boundary between the RPG's quarter-unit cells and world-space actors.
 * Chunk coordinates still span 16 world units; a column therefore contains 64 by
 * 64 voxels and reaches 256 world units high. Never scale entity transforms. */
public final class VoxelGrid {
    public static final float SIZE = 0.25f;
    public static final int PER_UNIT = 4;
    public static final int WIDTH = 64;
    public static final int HEIGHT = 1024;
    private VoxelGrid() {}
    /** Floors rather than truncates, including the negative side of the origin. */
    public static int cell(float world) { return (int) Math.floor(world * PER_UNIT); }
    public static float world(int cell) { return cell * SIZE; }
    public static long key(int x, int z) { return ((long) x << 32) | (z & 0xffffffffL); }
}
