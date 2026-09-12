package org.kbeng.wrld;

import org.kbeng.data.BlockData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * WorldGenerator provides world generator capabilities within the wrld subsystem.
 *
 * It maintains world simulation concerns including terrain, chunks, fluids, and authoritative spatial state.
 *
 * The generator produces deterministic content from seeds, rules, and runtime configuration.
 *
 * It implements Generator, providing a concrete strategy for this subsystem contract.
 */
public class WorldGenerator implements Generator {
    private static final int BASE_SURFACE_Y = 128;
    private static final int TOTAL_DEPTH = 128;
    private static final int TOPSOIL_DEPTH = 3;
    private static final int MIN_SURFACE_Y = 96;
    private static final int MAX_SURFACE_Y = 220;
    private static final int WATER_BASE_Y = BASE_SURFACE_Y - 1;
    private static final int MAX_WATER_CARVE = 8;
    private static final int TREE_CELL_SIZE = 6;
    private static final int PLANT_ATTEMPTS_PER_CHUNK = 20;
    private static final int TALL_GRASS_CLUSTER_ATTEMPTS = 24;
    private static final int TALL_GRASS_CLUSTER_SIZE = 8;

    private static final long TERRAIN_CONTINENT_SALT = 0x26A1_649D_B137_65E7L;
    private static final long TERRAIN_HILLS_SALT = 0x7C44_988F_6D38_18F1L;
    private static final long TERRAIN_RIDGE_SALT = 0x9E37_79B9_7F4A_7C15L;
    private static final long TERRAIN_PEAK_MASK_SALT = 0xC2B2_AE3D_27D4_EB4FL;
    private static final long RIVER_CHANNEL_SALT = 0x4CF5_AD43_2745_937FL;
    private static final long RIVER_WIDTH_SALT = 0x94D0_49BB_1331_11EBL;
    private static final long RIVER_LEVEL_SALT = 0xD6E8_FD95_98B2_C2F9L;
    private static final long LAKE_BASIN_SALT = 0x6A09_E667_F3BC_C909L;
    private static final long LAKE_DETAIL_SALT = 0xBB67_AE85_84CA_A73BL;
    private static final long LAKE_LEVEL_SALT = 0x3C6E_F372_FE94_F82BL;
    private static final long TREE_ANCHOR_SALT = 0xA54F_F53A_5F1D_36F1L;
    private static final long TREE_BIOME_SALT = 0x510E_527F_ADE6_82D1L;
    private static final long TREE_MOISTURE_SALT = 0x1F83_D9AB_FB41_BD6BL;
    private static final long TREE_VARIANT_SALT = 0x5BE0_CD19_137E_2179L;
    private static final long PLANT_CHUNK_SALT = 0xCBBB_9D5D_C105_9ED8L;

    private static final BlockData[] DECORATIVE_PLANTS = createDecorativePlants();

    private static FluidSimulation fluidSimulation;

    private final long seed;

    /**
     * Creates a new {@code WorldGenerator} instance with a random seed.
     * @param fluidSimulation the {@link FluidSimulation} argument; the fluid simulation used for generated lakes and rivers
     */
    public WorldGenerator(FluidSimulation fluidSimulation) {
        this(fluidSimulation, new Random().nextLong());
    }

    /**
     * Creates a new {@code WorldGenerator} instance.
     * @param fluidSimulation the {@link FluidSimulation} argument; the fluid simulation used for generated lakes and rivers
     * @param seed the {@code long} supplied as {@code seed}
     */
    public WorldGenerator(FluidSimulation fluidSimulation, long seed) {
        WorldGenerator.fluidSimulation = fluidSimulation;
        this.seed = seed;
    }

    /**
     * {@inheritDoc}
     * Generates one chunk of infinite terrain and biome features.
     * @param chunkX the {@code int} supplied as {@code chunkX}
     * @param chunkZ the {@code int} supplied as {@code chunkZ}
     */
    @Override
    public void generateChunk(int chunkX, int chunkZ) {
        Chunk chunk = World.wrld.getOrCreateChunk(chunkX, chunkZ);
        int[][] surfaceHeights = new int[Chunk.SIZE_X][Chunk.SIZE_Z];
        boolean[][] waterColumns = new boolean[Chunk.SIZE_X][Chunk.SIZE_Z];
        byte[][] surfaceBlocks = new byte[Chunk.SIZE_X][Chunk.SIZE_Z];

        for (int localX = 0; localX < Chunk.SIZE_X; localX++) {
            for (int localZ = 0; localZ < Chunk.SIZE_Z; localZ++) {
                int worldX = chunkX * Chunk.SIZE_X + localX;
                int worldZ = chunkZ * Chunk.SIZE_Z + localZ;

                int terrainY = terrainHeight(worldX, worldZ);
                TerrainColumn column = resolveTerrainColumn(worldX, worldZ, terrainY);

                surfaceHeights[localX][localZ] = column.surfaceY();
                waterColumns[localX][localZ] = column.waterSurfaceY() >= 0;
                surfaceBlocks[localX][localZ] = column.surfaceBlock();

                fillSolidColumn(chunk, localX, localZ, column.surfaceY(), column.surfaceBlock());
                if (column.waterSurfaceY() >= 0) {
                    fillWaterColumn(chunk, localX, localZ,
                            column.surfaceY() + 1, column.waterSurfaceY());
                    fluidSimulation.addSource(worldX, column.waterSurfaceY(), worldZ);
                }
            }
        }

        generateTrees(chunk, chunkX, chunkZ, surfaceHeights, waterColumns, surfaceBlocks);
        generatePlants(chunk, chunkX, chunkZ, surfaceHeights, waterColumns, surfaceBlocks);
    }

    /**
     * Resolves terrain and water shape for one world column.
     */
    private TerrainColumn resolveTerrainColumn(int worldX, int worldZ, int terrainY) {
        int surfaceY = terrainY;
        int waterSurfaceY = -1;
        byte surfaceBlock = BlockData.GRASS.getId();

        float riverStrength = riverStrength(worldX, worldZ, terrainY);
        if (riverStrength > 0.0f) {
            int riverLevel = Math.min(terrainY - 1,
                    WATER_BASE_Y + Math.round(sampleFbm(worldX, worldZ, 96.0f,
                            2, 0.5f, RIVER_LEVEL_SALT) * 2.0f));
            if (riverLevel >= 3) {
                int riverDepth = 2 + Math.round(riverStrength * (MAX_WATER_CARVE - 2));
                surfaceY = Math.max(2, riverLevel - riverDepth);
                waterSurfaceY = riverLevel;
                surfaceBlock = BlockData.SAND.getId();
            }
        } else {
            float lakeStrength = lakeStrength(worldX, worldZ, terrainY);
            if (lakeStrength > 0.0f) {
                int lakeLevel = Math.min(terrainY - 1,
                        WATER_BASE_Y + 1 + Math.round(sampleFbm(worldX, worldZ, 140.0f,
                                2, 0.5f, LAKE_LEVEL_SALT) * 2.0f));
                if (lakeLevel >= 3) {
                    int lakeDepth = 3 + Math.round(lakeStrength * 4.0f);
                    surfaceY = Math.max(2, lakeLevel - lakeDepth);
                    waterSurfaceY = lakeLevel;
                    surfaceBlock = BlockData.SAND.getId();
                }
            }
        }

        return new TerrainColumn(surfaceY, waterSurfaceY, surfaceBlock);
    }

    /**
     * Generates one column with flatworld-compatible layering.
     */
    private void fillSolidColumn(Chunk chunk, int localX, int localZ,
                                 int surfaceY, byte surfaceBlock) {
        int clampedSurfaceY = clamp(surfaceY, 1, Chunk.SIZE_Y - 1);
        int bottomY = Math.max(1, clampedSurfaceY - TOTAL_DEPTH + 1);

        for (int y = bottomY; y <= clampedSurfaceY; y++) {
            int depth = clampedSurfaceY - y + 1;
            byte blockId;
            if (y == bottomY) {
                blockId = BlockData.VOIDSEAL.getId();
            } else if (surfaceBlock == BlockData.SAND.getId()) {
                blockId = depth <= 4 ? BlockData.SAND.getId() : BlockData.STONE.getId();
            } else if (depth == 1) {
                blockId = BlockData.GRASS.getId();
            } else if (depth <= TOPSOIL_DEPTH) {
                blockId = BlockData.DIRT.getId();
            } else {
                blockId = BlockData.STONE.getId();
            }
            chunk.setBlock(localX, y, localZ, blockId);
        }
    }

    /**
     * Fills a water column with full fluid levels.
     */
    private void fillWaterColumn(Chunk chunk, int localX, int localZ, int fromY, int toY) {
        int startY = clamp(fromY, 1, Chunk.SIZE_Y - 1);
        int endY = clamp(toY, 1, Chunk.SIZE_Y - 1);
        if (startY > endY) return;

        byte waterId = fluidSimulation.getFluidType().getId();
        for (int y = startY; y <= endY; y++) {
            chunk.setBlock(localX, y, localZ, waterId);
            chunk.setFluidLevel(localX, y, localZ, (byte) 8);
        }
    }

    /**
     * Places deterministic trees for forest biomes.
     */
    private void generateTrees(Chunk chunk, int chunkX, int chunkZ,
                               int[][] surfaceHeights, boolean[][] waterColumns,
                               byte[][] surfaceBlocks) {
        for (int localX = 3; localX <= Chunk.SIZE_X - 4; localX++) {
            for (int localZ = 3; localZ <= Chunk.SIZE_Z - 4; localZ++) {
                if (waterColumns[localX][localZ]
                        || surfaceBlocks[localX][localZ] != BlockData.GRASS.getId()) {
                    continue;
                }

                int surfaceY = surfaceHeights[localX][localZ];
                if (surfaceY < WATER_BASE_Y - 2 || surfaceY >= Chunk.SIZE_Y - 18) continue;
                if (isNearWater(waterColumns, localX, localZ)) continue;

                int worldX = chunkX * Chunk.SIZE_X + localX;
                int worldZ = chunkZ * Chunk.SIZE_Z + localZ;
                if (!isForest(worldX, worldZ, surfaceY) || !isTreeAnchor(worldX, worldZ)) {
                    continue;
                }

                long treeSeed = hash(worldX, worldZ, TREE_VARIANT_SALT);
                boolean spruce = preferSpruce(worldX, worldZ, surfaceY);
                if (spruce) {
                    placeSpruceTree(chunk, localX, localZ, surfaceY, treeSeed);
                } else {
                    placeOakTree(chunk, localX, localZ, surfaceY, treeSeed);
                }
            }
        }
    }

    /**
     * Places decorative plants in grassland and forest clearings.
     */
    private void generatePlants(Chunk chunk, int chunkX, int chunkZ,
                                int[][] surfaceHeights, boolean[][] waterColumns,
                                byte[][] surfaceBlocks) {
        if (DECORATIVE_PLANTS.length == 0) return;
        Random random = new Random(hash(chunkX, chunkZ, PLANT_CHUNK_SALT));

        for (int attempt = 0; attempt < PLANT_ATTEMPTS_PER_CHUNK; attempt++) {
            int localX = random.nextInt(Chunk.SIZE_X);
            int localZ = random.nextInt(Chunk.SIZE_Z);
            if (waterColumns[localX][localZ]
                    || surfaceBlocks[localX][localZ] != BlockData.GRASS.getId()) {
                continue;
            }

            int surfaceY = surfaceHeights[localX][localZ];
            if (surfaceY < 1 || surfaceY >= Chunk.SIZE_Y - 2) continue;
            if (chunk.getBlock(localX, surfaceY + 1, localZ) != BlockData.AIR.getId()) continue;

            int worldX = chunkX * Chunk.SIZE_X + localX;
            int worldZ = chunkZ * Chunk.SIZE_Z + localZ;
            if (isNearWater(waterColumns, localX, localZ)) continue;

            float floraNoise = sampleFbm(worldX, worldZ, 88.0f, 3, 0.55f, TREE_MOISTURE_SALT);
            if (floraNoise < -0.15f) continue;

            BlockData plant = DECORATIVE_PLANTS[random.nextInt(DECORATIVE_PLANTS.length)];
            chunk.setBlock(localX, surfaceY + 1, localZ, plant.getId());
            if (plant == BlockData.TALL_GRASS) {
                generateTallGrassCluster(chunk, surfaceHeights, waterColumns,
                        localX, localZ, surfaceY, random);
            }
        }
    }

    /**
     * Expands tall grass around its seed while staying inside the chunk.
     */
    private void generateTallGrassCluster(Chunk chunk, int[][] surfaceHeights,
                                          boolean[][] waterColumns, int centerX,
                                          int centerZ, int centerSurfaceY, Random random) {
        int placed = 1;
        for (int attempt = 0; attempt < TALL_GRASS_CLUSTER_ATTEMPTS
                && placed < TALL_GRASS_CLUSTER_SIZE; attempt++) {
            int localX = centerX + random.nextInt(5) - 2;
            int localZ = centerZ + random.nextInt(5) - 2;
            if (localX < 0 || localX >= Chunk.SIZE_X || localZ < 0 || localZ >= Chunk.SIZE_Z) {
                continue;
            }
            if (waterColumns[localX][localZ]) continue;

            int surfaceY = surfaceHeights[localX][localZ];
            if (Math.abs(surfaceY - centerSurfaceY) > 2
                    || surfaceY < 1 || surfaceY >= Chunk.SIZE_Y - 2) {
                continue;
            }

            byte ground = chunk.getBlock(localX, surfaceY, localZ);
            if ((ground != BlockData.GRASS.getId() && ground != BlockData.DIRT.getId())
                    || chunk.getBlock(localX, surfaceY + 1, localZ) != BlockData.AIR.getId()) {
                continue;
            }

            chunk.setBlock(localX, surfaceY + 1, localZ, BlockData.TALL_GRASS.getId());
            placed++;
        }
    }

    /**
     * Places one oak tree.
     */
    private void placeOakTree(Chunk chunk, int localX, int localZ, int surfaceY, long treeSeed) {
        Random random = new Random(treeSeed);
        int trunkHeight = 4 + random.nextInt(4);
        int topY = surfaceY + trunkHeight;
        if (topY + 2 >= Chunk.SIZE_Y) return;

        for (int y = 1; y <= trunkHeight; y++) {
            chunk.setBlock(localX, surfaceY + y, localZ, BlockData.OAK_LOG.getId());
        }

        for (int y = topY - 2; y <= topY + 1; y++) {
            int radius = y == topY + 1 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius
                            && random.nextFloat() < 0.35f) {
                        continue;
                    }
                    placeLeafIfReplaceable(chunk, localX + dx, y, localZ + dz,
                            BlockData.OAK_LEAVES.getId());
                }
            }
        }
    }

    /**
     * Places one spruce tree.
     */
    private void placeSpruceTree(Chunk chunk, int localX, int localZ, int surfaceY, long treeSeed) {
        Random random = new Random(treeSeed);
        int trunkHeight = 9 + random.nextInt(4);
        int topY = surfaceY + trunkHeight;
        if (topY + 2 >= Chunk.SIZE_Y) return;

        for (int y = 1; y <= trunkHeight; y++) {
            chunk.setBlock(localX, surfaceY + y, localZ, BlockData.SPRUCE_LOG.getId());
        }

        for (int layer = 0; layer < trunkHeight - 1; layer++) {
            int y = topY - layer;
            int radius = layer < 2 ? 1 : Math.min(3, 1 + layer / 2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > radius + 1) continue;
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius
                            && random.nextFloat() < 0.30f) {
                        continue;
                    }
                    placeLeafIfReplaceable(chunk, localX + dx, y, localZ + dz,
                            BlockData.SPRUCE_LEAVES.getId());
                }
            }
        }
        placeLeafIfReplaceable(chunk, localX, topY + 1, localZ, BlockData.SPRUCE_LEAVES.getId());
    }

    /**
     * Places leaves when target is empty or a plant.
     */
    private void placeLeafIfReplaceable(Chunk chunk, int localX, int y, int localZ, byte leafId) {
        if (localX < 0 || localX >= Chunk.SIZE_X
                || localZ < 0 || localZ >= Chunk.SIZE_Z
                || y < 0 || y >= Chunk.SIZE_Y) {
            return;
        }

        byte existing = chunk.getBlock(localX, y, localZ);
        if (existing == BlockData.AIR.getId()) {
            chunk.setBlock(localX, y, localZ, leafId);
            return;
        }

        BlockData existingData = BlockData.fromId(existing);
        if (existingData != null && existingData.isPlant()) {
            chunk.setBlock(localX, y, localZ, leafId);
        }
    }

    /**
     * Returns whether a local position is adjacent to water generated in this chunk.
     */
    private boolean isNearWater(boolean[][] waterColumns, int localX, int localZ) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = localX + dx;
                int z = localZ + dz;
                if (x < 0 || x >= Chunk.SIZE_X || z < 0 || z >= Chunk.SIZE_Z) continue;
                if (waterColumns[x][z]) return true;
            }
        }
        return false;
    }

    /**
     * Returns deterministic terrain height for a world column.
     */
    private int terrainHeight(int x, int z) {
        float continental = sampleFbm(x, z, 460.0f, 4, 0.55f, TERRAIN_CONTINENT_SALT);
        float hills = sampleFbm(x, z, 120.0f, 4, 0.52f, TERRAIN_HILLS_SALT);
        float ridges = ridge(sampleFbm(x, z, 230.0f, 5, 0.56f, TERRAIN_RIDGE_SALT));
        float mountainMask = toUnit(sampleFbm(x, z, 880.0f, 3, 0.58f, TERRAIN_PEAK_MASK_SALT));

        int baseVariation = Math.round(continental * 14.0f + hills * 10.0f);
        float mountainStrength = (float) Math.pow(Math.max(0.0f, ridges - 0.34f), 1.65f);
        int mountainHeight = Math.round(mountainStrength * 72.0f * (0.35f + mountainMask));

        return clamp(BASE_SURFACE_Y + baseVariation + mountainHeight,
                MIN_SURFACE_Y, MAX_SURFACE_Y);
    }

    /**
     * Returns a river intensity value in [0, 1].
     */
    private float riverStrength(int x, int z, int terrainY) {
        float channel = Math.abs(sampleFbm(x, z, 210.0f, 5, 0.58f, RIVER_CHANNEL_SALT));
        float widthNoise = toUnit(sampleFbm(x, z, 90.0f, 3, 0.5f, RIVER_WIDTH_SALT));
        float width = 0.015f + widthNoise * 0.032f;

        if (terrainY > BASE_SURFACE_Y + 52) {
            width *= 0.55f;
        }
        if (channel >= width) {
            return 0.0f;
        }
        return clamp01((width - channel) / width);
    }

    /**
     * Returns a lake intensity value in [0, 1].
     */
    private float lakeStrength(int x, int z, int terrainY) {
        if (terrainY > BASE_SURFACE_Y + 34) {
            return 0.0f;
        }

        float basin = sampleFbm(x, z, 350.0f, 4, 0.60f, LAKE_BASIN_SALT);
        float detail = sampleFbm(x, z, 72.0f, 3, 0.5f, LAKE_DETAIL_SALT);
        if (basin <= 0.42f || detail <= -0.18f) {
            return 0.0f;
        }

        float basinScore = clamp01((basin - 0.42f) * 2.25f);
        float detailScore = clamp01((detail + 0.18f) * 1.5f);
        return clamp01((basinScore * 0.7f) + (detailScore * 0.3f));
    }

    /**
     * Returns whether this column belongs to a forest biome.
     */
    private boolean isForest(int worldX, int worldZ, int surfaceY) {
        float biome = sampleFbm(worldX, worldZ, 290.0f, 4, 0.56f, TREE_BIOME_SALT);
        float moisture = sampleFbm(worldX, worldZ, 170.0f, 3, 0.52f, TREE_MOISTURE_SALT);
        float altitude = clamp01((surfaceY - BASE_SURFACE_Y) / 55.0f);
        float score = biome * 0.65f + moisture * 0.35f + altitude * 0.08f;
        return score > 0.08f;
    }

    /**
     * Returns whether this world position is the unique tree anchor in its cell.
     */
    private boolean isTreeAnchor(int worldX, int worldZ) {
        int cellX = Math.floorDiv(worldX, TREE_CELL_SIZE);
        int cellZ = Math.floorDiv(worldZ, TREE_CELL_SIZE);

        long h = hash(cellX, cellZ, TREE_ANCHOR_SALT);
        int offsetX = 1 + Math.floorMod((int) h, TREE_CELL_SIZE - 2);
        int offsetZ = 1 + Math.floorMod((int) (h >>> 32), TREE_CELL_SIZE - 2);

        int anchorX = cellX * TREE_CELL_SIZE + offsetX;
        int anchorZ = cellZ * TREE_CELL_SIZE + offsetZ;
        if (worldX != anchorX || worldZ != anchorZ) {
            return false;
        }

        return toUnit(mix(h ^ 0x9E37_79B9_7F4A_7C15L)) > 0.28f;
    }

    /**
     * Returns whether a tree should use the spruce variant.
     */
    private boolean preferSpruce(int worldX, int worldZ, int surfaceY) {
        float alpine = clamp01((surfaceY - (BASE_SURFACE_Y + 20)) / 35.0f);
        float biomeNoise = toUnit(sampleFbm(worldX, worldZ, 84.0f, 3, 0.55f, TREE_VARIANT_SALT));
        return biomeNoise + alpine * 0.55f > 0.72f;
    }

    /**
     * Generates the complete tree selected by its trunk block type.
     * @param worldX the {@code int} supplied as {@code worldX}
     * @param surfaceY the {@code int} supplied as {@code surfaceY}
     * @param worldZ the {@code int} supplied as {@code worldZ}
     * @param random the {@link Random} supplied as {@code random}
     * @param logType the trunk block type selecting the tree variant
     */
    public static void generateTree(int worldX, int surfaceY, int worldZ, Random random,
                                    BlockData logType) {
        switch (Objects.requireNonNull(logType)) {
            case OAK_LOG -> {
                int trunkHeight = 4 + random.nextInt(5);
                for (int y = 1; y <= trunkHeight; y++) {
                    World.wrld.setBlockTypeAt(
                            worldX, surfaceY + y, worldZ, BlockData.OAK_LOG.getId());
                }

                int topY = surfaceY + trunkHeight;
                for (int y = topY - 2; y <= topY + 1; y++) {
                    int radius = y == topY + 1 ? 1 : 2;
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (Math.abs(dx) == radius && Math.abs(dz) == radius
                                    && random.nextDouble() < 0.4) continue;
                            if (World.wrld.getBlockTypeAt(worldX + dx, y, worldZ + dz)
                                    == BlockData.AIR.getId()) {
                                World.wrld.setBlockTypeAt(worldX + dx, y, worldZ + dz,
                                        BlockData.OAK_LEAVES.getId());
                            }
                        }
                    }
                }
            }
            case SPRUCE_LOG -> {
                int trunkHeight = 12 + random.nextInt(4);
                for (int y = 1; y <= trunkHeight; y++) {
                    World.wrld.setBlockTypeAt(
                            worldX, surfaceY + y, worldZ, BlockData.SPRUCE_LOG.getId());
                }

                int topY = surfaceY + trunkHeight;
                for (int layer = 0; layer < trunkHeight - 1; layer++) {
                    int y = topY - layer;
                    int radius = layer < 2 ? 1 : Math.min(3, 1 + layer / 2);
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (Math.abs(dx) + Math.abs(dz) > radius + 1) continue;
                            if (Math.abs(dx) == radius && Math.abs(dz) == radius
                                    && random.nextDouble() < 0.35) continue;
                            if (World.wrld.getBlockTypeAt(worldX + dx, y, worldZ + dz)
                                    == BlockData.AIR.getId()) {
                                World.wrld.setBlockTypeAt(worldX + dx, y, worldZ + dz,
                                        BlockData.SPRUCE_LEAVES.getId());
                            }
                        }
                    }
                }
                World.wrld.setBlockTypeAt(
                        worldX, topY + 1, worldZ, BlockData.SPRUCE_LEAVES.getId());
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported tree log type: " + logType);
        }

        GameMaster.game
                .rebuildChunkMeshAt(worldX, worldZ);
    }

    /**
     * Fractal value noise in range [-1, 1].
     */
    private float sampleFbm(int x, int z, float baseScale, int octaves,
                            float persistence, long salt) {
        float sum = 0.0f;
        float amplitude = 1.0f;
        float amplitudeSum = 0.0f;
        float scale = baseScale;
        for (int i = 0; i < octaves; i++) {
            sum += sampleValueNoise(x, z, scale, salt + i * 0x9E37_79B9_7F4A_7C15L)
                    * amplitude;
            amplitudeSum += amplitude;
            amplitude *= persistence;
            scale *= 0.5f;
        }
        return amplitudeSum == 0.0f ? 0.0f : sum / amplitudeSum;
    }

    /**
     * Bilinearly-interpolated deterministic value noise in range [-1, 1].
     */
    private float sampleValueNoise(int x, int z, float scale, long salt) {
        float nx = x / scale;
        float nz = z / scale;

        int x0 = (int) Math.floor(nx);
        int z0 = (int) Math.floor(nz);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        float tx = smoothStep(nx - x0);
        float tz = smoothStep(nz - z0);

        float v00 = toSigned(hash(x0, z0, salt));
        float v10 = toSigned(hash(x1, z0, salt));
        float v01 = toSigned(hash(x0, z1, salt));
        float v11 = toSigned(hash(x1, z1, salt));

        float a = lerp(v00, v10, tx);
        float b = lerp(v01, v11, tx);
        return lerp(a, b, tz);
    }

    /**
     * Hashes two coordinates and a salt into deterministic pseudo-random bits.
     */
    private long hash(int x, int z, long salt) {
        long value = seed ^ salt;
        value += (long) x * 0x632B_E59B_D9B4_E019L;
        value += (long) z * 0x8CB9_2BA7_2F3D_8DD7L;
        return mix(value);
    }

    /**
     * SplitMix64 mixer.
     */
    private long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58_476D_1CE4_E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D0_49BB_1331_11EBL;
        return value ^ (value >>> 31);
    }

    private static float toUnit(float signed) {
        return (signed + 1.0f) * 0.5f;
    }

    private static float toUnit(long bits) {
        return (bits >>> 40) / (float) 0xFFFFFFL;
    }

    private static float toSigned(long bits) {
        return toUnit(bits) * 2.0f - 1.0f;
    }

    private static float ridge(float value) {
        return 1.0f - Math.abs(value);
    }

    private static float smoothStep(float value) {
        float t = clamp01(value);
        return t * t * (3.0f - 2.0f * t);
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    /** Excludes bonsais, which are crafted/placeable blocks rather than wild plants. */
    private static BlockData[] createDecorativePlants() {
        List<BlockData> plants = new ArrayList<>();
        for (BlockData plant : BlockData.allPlants()) {
            if (plant != BlockData.OAK_BONSAI && plant != BlockData.SPRUCE_BONSAI) {
                plants.add(plant);
            }
        }
        return plants.toArray(new BlockData[0]);
    }

    /**
     * Immutable terrain profile for one world column.
     */
    private record TerrainColumn(int surfaceY, int waterSurfaceY, byte surfaceBlock) {}
}
