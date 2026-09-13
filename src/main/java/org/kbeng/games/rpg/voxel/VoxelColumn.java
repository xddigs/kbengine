package org.kbeng.games.rpg.voxel;

import java.util.Arrays;

/** Immutable vertical run-length encoding. Each end is exclusive; air is encoded
 * too, so caves and individually removed voxels do not require a second store.
 * A homogeneous 256-unit column costs one run rather than 1024 material bytes. */
public final class VoxelColumn {
    public static final VoxelColumn AIR = new VoxelColumn(new short[]{1024}, new byte[]{0});
    private final short[] ends;
    private final byte[] materials;
    public VoxelColumn(short[] ends, byte[] materials) {
        this.ends = ends.clone();
        this.materials = materials.clone();
        if (ends.length == 0 || ends.length != materials.length || ends[ends.length - 1] != VoxelGrid.HEIGHT)
            throw new IllegalArgumentException("Column must cover exactly 1024 cells");
        int previous = 0;
        for (short end : ends) {
            if (end <= previous) throw new IllegalArgumentException("Run ends must increase");
            previous = end;
        }
    }
    public int runs() { return ends.length; }
    public int start(int run) { return run == 0 ? 0 : ends[run - 1]; }
    public int end(int run) { return ends[run]; }
    public byte material(int run) { return materials[run]; }
    public byte get(int y) {
        if (y < 0 || y >= VoxelGrid.HEIGHT) return 0;
        int index = Arrays.binarySearch(ends, (short) (y + 1));
        return materials[index < 0 ? -index - 1 : index];
    }
    /** Replaces one cell, preserving all 1023 neighbours; storage is recompressed
     * immediately. The temporary buffer is local to editing, never retained. */
    public VoxelColumn with(int y, byte material) {
        if (y < 0 || y >= VoxelGrid.HEIGHT) throw new IllegalArgumentException("Voxel height out of range");
        if (get(y) == material) return this;
        byte[] cells = new byte[VoxelGrid.HEIGHT];
        for (int r = 0; r < runs(); r++) Arrays.fill(cells, start(r), end(r), materials[r]);
        cells[y] = material;
        return compress(cells);
    }
    public static VoxelColumn compress(byte[] cells) {
        if (cells.length != VoxelGrid.HEIGHT) throw new IllegalArgumentException("Incorrect column height");
        short[] ends = new short[cells.length];
        byte[] types = new byte[cells.length];
        int count = 0;
        for (int y = 1; y <= cells.length; y++) {
            if (y == cells.length || cells[y] != cells[y - 1]) {
                ends[count] = (short) y;
                types[count++] = cells[y - 1];
            }
        }
        return new VoxelColumn(Arrays.copyOf(ends, count), Arrays.copyOf(types, count));
    }
}
