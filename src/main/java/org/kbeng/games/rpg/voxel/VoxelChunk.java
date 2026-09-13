package org.kbeng.games.rpg.voxel;

import java.util.Arrays;

/** A 16-by-16 world-unit chunk with immutable compressed voxel columns. CPU
 * mesh builders may retain a column safely while the game replaces another. */
public final class VoxelChunk {
    public final int x, z;
    private final VoxelColumn[] columns = new VoxelColumn[VoxelGrid.WIDTH * VoxelGrid.WIDTH];
    public VoxelChunk(int x, int z) {
        this.x = x; this.z = z;
        Arrays.fill(columns, VoxelColumn.AIR);
    }
    public VoxelColumn column(int x, int z) { return columns[x + z * VoxelGrid.WIDTH]; }
    public void column(int x, int z, VoxelColumn column) { columns[x + z * VoxelGrid.WIDTH] = column; }
}
