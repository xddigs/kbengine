package org.kbeng.games.rpg.voxel;

import org.kbeng.games.rpg.data.BlockData;
import org.kbeng.games.rpg.wrld.Generator;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/** Samples a finite island and its vegetation directly at quarter-unit
 * resolution. A radial falloff creates a real coastline and ocean floor around
 * the playable landmass; all tree/plant anchors are global and seed-derived, so
 * chunk generation order cannot create seams at boundaries. */
public final class VoxelGenerator implements Generator {
    private final long seed;
    private static final float SEA = 122f;
    public VoxelGenerator(long seed) { this.seed = seed; }

    /** Height in world units before quantization. The island is centred at the
     * origin and fades into an ocean floor outside a 190-unit radius. */
    public float height(float x, float z) {
        float distance = (float) Math.sqrt(x * x + z * z);
        float island = 1.0f - smoothstep(132.0f, 190.0f, distance);
        float broad = noise(x / 160, z / 160) * (3.0f + island * 14.0f);
        float detail = noise(x / 42, z / 42) * (1.5f + island * 5.0f)
                + noise(x / 14, z / 14) * 1.5f;
        return SEA - 18.0f + island * 25.0f + broad + detail;
    }

    /** {@inheritDoc} */
    @Override
    public void generateChunk(int chunkX, int chunkZ) {
        generate(chunkX, chunkZ);
    }

    /**
     * Generates a single chunk.
     * @param cx chunk X coordinate
     * @param cz chunk Z coordinate
     * @return the generated chunk
     */
    public VoxelChunk generate(int cx, int cz) {
        VoxelChunk chunk = new VoxelChunk(cx, cz);
        List<Tree> trees = trees(cx, cz);
        List<Plant> plants = plants(cx, cz);
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
                    boolean trunk = h < tree.height && square(dx - bend) + square(dz) < square(trunkRadius);
                    boolean branch = false;
                    if (!trunk && h > tree.height * .38f && h < tree.height * .92f) {
                        for (int b = 0; b < 4; b++) {
                            float branchHeight = tree.height * (.40f + b * .13f);
                            float t = (h - branchHeight) / 1.55f;
                            if (t < 0 || t > 1) continue;
                            float angle = tree.phase + b * 1.5708f;
                            float length = 1.6f + b * .42f;
                            float branchX = bend + (float) Math.cos(angle) * length * t;
                            float branchZ = (float) Math.sin(angle) * length * t;
                            branch |= square(dx - branchX) + square(dz - branchZ) < square(.27f);
                        }
                    }
                    if (trunk || branch) {
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
            for (Plant plant : plants) {
                float dx = wx - plant.x, dz = wz - plant.z;
                if (dx * dx + dz * dz > 2.1f) continue;
                int plantBase = VoxelGrid.cell(plant.base);
                if (plantBase < top || top < VoxelGrid.cell(SEA + 0.5f)) continue;
                int y = Math.max(top, plantBase);
                if (plant.kind == PlantKind.TALL_GRASS) {
                    for (int i = 0; i < 4 && y + i < VoxelGrid.HEIGHT; i++)
                        if (cells[y + i] == 0) cells[y + i] = BlockData.TALL_GRASS.getId();
                } else {
                    if (cells[y] == 0) cells[y] = BlockData.TALL_GRASS.getId();
                    int flowerY = y + 1;
                    byte flower = plant.kind == PlantKind.GHOSTFLOWER
                            ? BlockData.GHOSTFLOWER.getId() : BlockData.ROSE.getId();
                    if (flowerY < VoxelGrid.HEIGHT && cells[flowerY] == 0) cells[flowerY] = flower;
                    // Cross-shaped petals and a second lobe make a volumetric
                    // voxel analogue of the former billboard flower sprites.
                    if (plant.kind == PlantKind.ROSEBUSH) {
                        if (flowerY + 1 < VoxelGrid.HEIGHT && cells[flowerY + 1] == 0) cells[flowerY + 1] = flower;
                        if (dx * dx + dz * dz < 1.0f && flowerY < VoxelGrid.HEIGHT) cells[flowerY] = flower;
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
                result.add(new Tree(tx, tz, base, 8 + ((h >>> 20) & 255) / 255f * 5,
                        ((h >>> 28) & 255) / 40f, (h & 4) == 0));
            }
        return result;
    }

    /** Returns vegetation anchors in a one-cell border around this chunk. */
    private List<Plant> plants(int cx, int cz) {
        List<Plant> result = new ArrayList<>();
        int minX = cx * 16 - 3, maxX = cx * 16 + 19;
        int minZ = cz * 16 - 3, maxZ = cz * 16 + 19;
        for (int z = Math.floorDiv(minZ, 5); z <= Math.floorDiv(maxZ, 5); z++)
            for (int x = Math.floorDiv(minX, 5); x <= Math.floorDiv(maxX, 5); x++) {
                long h = hash(x * 17 + 11, z * 17 - 7);
                if ((h & 7) > 2) continue;
                float px = x * 5 + 1.0f + ((h >>> 8) & 255) / 255f * 3.0f;
                float pz = z * 5 + 1.0f + ((h >>> 16) & 255) / 255f * 3.0f;
                float base = height(px, pz);
                if (base <= SEA + 0.5f || px * px + pz * pz > 184f * 184f) continue;
                PlantKind kind = switch ((int) ((h >>> 24) & 3)) {
                    case 0 -> PlantKind.TALL_GRASS;
                    case 1 -> PlantKind.GHOSTFLOWER;
                    case 2 -> PlantKind.ROSE;
                    default -> PlantKind.ROSEBUSH;
                };
                result.add(new Plant(px, pz, base, kind));
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
    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Math.clamp((value - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private record Tree(float x, float z, float base, float height, float phase, boolean spruce) {}
    private record Plant(float x, float z, float base, PlantKind kind) {}
    private enum PlantKind { TALL_GRASS, GHOSTFLOWER, ROSE, ROSEBUSH }
}
