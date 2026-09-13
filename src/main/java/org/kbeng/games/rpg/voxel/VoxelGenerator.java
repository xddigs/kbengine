package org.kbeng.games.rpg.voxel;

import org.kbeng.games.rpg.data.BlockData;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/** Samples continuous terrain and rounded tree volumes directly at quarter-unit
 * resolution. All noise and tree anchors are global and seed-derived: generation
 * order cannot clip a crown or introduce a seam at a chunk boundary. */
public final class VoxelGenerator {
    private final long seed;
    private static final float SEA = 122f;
    public VoxelGenerator(long seed) { this.seed = seed; }

    /** Height in world units before the final quarter-unit quantization. */
    public float height(float x, float z) {
        return 126 + noise(x / 160, z / 160) * 18
                + noise(x / 42, z / 42) * 6 + noise(x / 14, z / 14) * 1.5f;
    }
    public VoxelChunk generate(int cx, int cz) {
        VoxelChunk chunk = new VoxelChunk(cx, cz);
        List<Tree> trees = trees(cx, cz);
        byte[] cells = new byte[VoxelGrid.HEIGHT];
        for (int z = 0; z < VoxelGrid.WIDTH; z++) for (int x = 0; x < VoxelGrid.WIDTH; x++) {
            float wx = cx * 16f + VoxelGrid.world(x) + 0.125f;
            float wz = cz * 16f + VoxelGrid.world(z) + 0.125f;
            int top = VoxelGrid.cell(height(wx, wz));
            Arrays.fill(cells, (byte) 0);
            Arrays.fill(cells, 0, 4, BlockData.VOIDSEAL.getId());
            Arrays.fill(cells, 4, top - 12, BlockData.STONE.getId());
            Arrays.fill(cells, top - 12, top - 2, BlockData.DIRT.getId());
            Arrays.fill(cells, top - 2, top, top < VoxelGrid.cell(SEA + 1)
                    ? BlockData.SAND.getId() : BlockData.GRASS.getId());
            if (top < VoxelGrid.cell(SEA)) Arrays.fill(cells, top, VoxelGrid.cell(SEA), BlockData.WATER.getId());
            for (Tree tree : trees) {
                float dx = wx - tree.x, dz = wz - tree.z;
                if (dx * dx + dz * dz > 30) continue;
                int from = Math.max(top, VoxelGrid.cell(tree.base));
                int to = Math.min(VoxelGrid.HEIGHT, VoxelGrid.cell(tree.base + tree.height + 4));
                for (int y = from; y < to; y++) {
                    float h = VoxelGrid.world(y) + 0.125f - tree.base;
                    float bend = 0.35f * (float) Math.sin(h * 0.32f + tree.phase);
                    float trunkRadius = Math.max(0.17f, 0.62f - h * 0.035f);
                    if (h < tree.height && square(dx - bend) + square(dz) < square(trunkRadius)) {
                        cells[y] = tree.spruce ? BlockData.SPRUCE_LOG.getId() : BlockData.OAK_LOG.getId();
                    } else {
                        float crown = square(dx / 3.8f) + square(dz / 3.3f)
                                + square((h - tree.height + 0.8f) / 3.6f);
                        float lobe = square((dx - 1.8f) / 2.8f) + square((dz + 1.1f) / 3f)
                                + square((h - tree.height + 2f) / 2.5f);
                        if (Math.min(crown, lobe) < 1 + 0.06f * Math.sin(wx * 2 + h) * Math.cos(wz * 1.6f)
                                && cells[y] == 0) {
                            cells[y] = tree.spruce ? BlockData.SPRUCE_LEAVES.getId() : BlockData.OAK_LEAVES.getId();
                        }
                    }
                }
            }
            chunk.column(x, z, VoxelColumn.compress(cells));
        }
        return chunk;
    }
    private List<Tree> trees(int cx, int cz) {
        List<Tree> result = new ArrayList<>();
        for (int z = Math.floorDiv(cz * 16 - 6, 12); z <= Math.floorDiv(cz * 16 + 22, 12); z++)
            for (int x = Math.floorDiv(cx * 16 - 6, 12); x <= Math.floorDiv(cx * 16 + 22, 12); x++) {
                long h = hash(x, z);
                if ((h & 3) == 0) continue;
                float tx = x * 12 + 3 + ((h >>> 4) & 255) / 255f * 6;
                float tz = z * 12 + 3 + ((h >>> 12) & 255) / 255f * 6;
                float base = height(tx, tz);
                if (base <= SEA + 1 || tx * tx + tz * tz < 25) continue;
                result.add(new Tree(tx, tz, base, 7 + ((h >>> 20) & 255) / 255f * 4,
                        ((h >>> 28) & 255) / 40f, (h & 4) == 0));
            }
        return result;
    }
    private float noise(float x, float z) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        float fx = x - ix, fz = z - iz;
        fx = fx * fx * fx * (fx * (fx * 6 - 15) + 10);
        fz = fz * fz * fz * (fz * (fz * 6 - 15) + 10);
        return mix(mix(value(ix, iz), value(ix + 1, iz), fx),
                mix(value(ix, iz + 1), value(ix + 1, iz + 1), fx), fz);
    }
    private float value(int x, int z) { return (hash(x, z) & 65535) / 32767.5f - 1; }
    private long hash(int x, int z) {
        long n = seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
        n = (n ^ (n >>> 30)) * 0xBF58476D1CE4E5B9L;
        n = (n ^ (n >>> 27)) * 0x94D049BB133111EBL;
        return n ^ (n >>> 31);
    }
    private static float square(float x) { return x * x; }
    private static float mix(float a, float b, float t) { return a + (b - a) * t; }
    private record Tree(float x, float z, float base, float height, float phase, boolean spruce) {}
}
