package org.kbeng.rpg.wrld;

import org.kbeng.rpg.data.BlockData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * IslandGenerator provides island generator capabilities within the wrld subsystem.
 * It maintains world simulation concerns including terrain, chunks, fluids, and authoritative spatial state.
 * The generator produces deterministic content from seeds, rules, and runtime configuration.
 * It implements Generator, providing a concrete strategy for this subsystem contract.
 */
public final class IslandGenerator implements Generator {
    public static final int OCEAN_DEPTH = 36;
    public static final int SEA_LEVEL = OCEAN_DEPTH + 1;

    private static final int OCEAN_FLOOR_Y = 1;
    private static final int VOID_SEAL_Y = OCEAN_FLOOR_Y - 1;
    private static final int CENTER_X = 0;
    private static final int CENTER_Z = 0;
    private static final float ISLAND_RADIUS = 24.0f;
    private static final float COAST_WIDTH = 2.0f;
    private static final float SEABED_TERRACE_WIDTH = 2.0f;
    private static final int ISLAND_HEIGHT = 8;

    private static final float LAKE_RADIUS = 4.0f;
    private static final float LAVA_RADIUS = 1.75f;
    private static final float LAKE_CHANCE = 0.50f;
    private static final float LAVA_CHANCE = 0.25f;
    private static final float NPC_SAFE_RADIUS = 12.0f;
    private static final float SPAWN_PLATFORM_RADIUS = 3.0f;
    private static final float SPAWN_PLATFORM_TRANSITION = 6.0f;
    private static final int PLANT_ATTEMPTS_PER_CHUNK = 24;
    private static final int TALL_GRASS_CLUSTER_ATTEMPTS = 24;
    private static final int TALL_GRASS_CLUSTER_SIZE = 8;
    private static final BlockData[] DECORATIVE_PLANTS = createDecorativePlants();

    private final FluidSimulation waterSimulation;
    private final long seed;
    private final Lake lake;
    private final LavaPool lavaPool;
    private final List<Tree> trees = new ArrayList<>();

    /**
     * Creates a generator with a random world seed.
     * @param waterSimulation the {@link FluidSimulation} argument; the fluid simulation
     *                        used for generated ocean and lakes
     */
    public IslandGenerator(FluidSimulation waterSimulation) {
        this(waterSimulation, new Random().nextLong());
    }

    /**
     * Creates a deterministic generator for the supplied seed.
     * @param waterSimulation the {@link FluidSimulation} argument; the fluid simulation
     *                        used for generated ocean and lakes
     * @param seed the {@code long} supplied as {@code seed}
     */
    public IslandGenerator(FluidSimulation waterSimulation, long seed) {
        this.waterSimulation = waterSimulation;
        this.seed = seed;

        Random random = new Random(seed);
        lake = random.nextFloat() < LAKE_CHANCE ? chooseLake(random) : null;
        lavaPool = random.nextFloat() < LAVA_CHANCE ? chooseLavaPool(random) : null;
        chooseTrees(random);
    }

    /**
     * {@inheritDoc}
     * Generates terrain and features owned by one chunk.
     * @param chunkX the {@code int} supplied as {@code chunkX}
     * @param chunkZ the {@code int} supplied as {@code chunkZ}
     */
    @Override
    public void generateChunk(int chunkX, int chunkZ) {
        Chunk chunk = World.wrld.getOrCreateChunk(chunkX, chunkZ);
        for (int localX = 0; localX < Chunk.SIZE_X; localX++) {
            for (int localZ = 0; localZ < Chunk.SIZE_Z; localZ++) {
                int worldX = chunkX * Chunk.SIZE_X + localX;
                int worldZ = chunkZ * Chunk.SIZE_Z + localZ;
                generateColumn(chunk, localX, localZ, worldX, worldZ);
            }
        }

        registerWaterSources(chunk, chunkX, chunkZ);
        registerLavaSources(chunkX, chunkZ);
        generateTreesInChunk(chunkX, chunkZ);
        long plantSeed = seed ^ ((long) chunkX * 341873128712L)
                ^ ((long) chunkZ * 132897987541L) ^ 0x5DEECE66DL;
        generatePlants(chunk, new Random(plantSeed));
    }

    /**
     * Generates a single vertical column of blocks within a chunk.
     * @param chunk the {@link Chunk} supplied as {@code chunk}
     * @param localX the {@code int} supplied as {@code localX}
     * @param localZ the {@code int} supplied as {@code localZ}
     * @param worldX the {@code int} supplied as {@code worldX}
     * @param worldZ the {@code int} supplied as {@code worldZ}
     */
    private void generateColumn(Chunk chunk, int localX, int localZ, int worldX, int worldZ) {
        chunk.setBlock(localX, VOID_SEAL_Y, localZ, BlockData.VOIDSEAL.getId());
        boolean island = isIsland(worldX, worldZ);
        boolean inLake = island && isInLake(worldX, worldZ);
        boolean inLava = island && isInLavaPool(worldX, worldZ);
        boolean lakeBank = island && !inLake && isLakeBank(worldX, worldZ);
        boolean lavaBank = island && !inLava && isLavaBank(worldX, worldZ);

        int topY = island ? terrainHeight(worldX, worldZ) : seabedHeight(worldX, worldZ);
        if (inLake) topY = lake.surfaceY - 2;
        if (inLava) topY = lavaPool.surfaceY - 1;
        if (lakeBank) topY = Math.max(topY, lake.surfaceY);
        if (lavaBank) topY = Math.max(topY, lavaPool.surfaceY);

        byte surfaceBlock = islandSurfaceBlock(worldX, worldZ,
                inLake || lakeBank, inLava || lavaBank);
        for (int y = OCEAN_FLOOR_Y; y <= topY; y++) {
            byte block;
            if (y == topY) {
                block = surfaceBlock;
            } else if (y >= topY - 3) {
                block = surfaceBlock == BlockData.SAND.getId()
                        ? BlockData.SAND.getId()
                        : BlockData.DIRT.getId();
            } else {
                block = BlockData.STONE.getId();
            }
            chunk.setBlock(localX, y, localZ, block);
        }

        if (inLake) {
            fillFluidColumn(chunk, localX, localZ, topY + 1, lake.surfaceY,
                    waterSimulation.getFluidType().getId());
        } else if (inLava) {
            chunk.setBlock(localX, lavaPool.surfaceY, localZ, BlockData.LAVA.getId());
            chunk.setFluidLevel(localX, lavaPool.surfaceY, localZ, (byte) 8);
        } else if (!island) {
            chunk.setBlock(localX, SEA_LEVEL, localZ, waterSimulation.getFluidType().getId());
            chunk.setFluidLevel(localX, SEA_LEVEL, localZ, (byte) 8);
        }
    }

    /**
     * Determines the surface block ID for island terrain based on proximity to fluids and coastlines.
     * @param x the {@code int} argument; the world x value
     * @param z the {@code int} argument; the world z value
     * @param inLake the {@code boolean} argument; whether the block is inside or bordering a lake
     * @param inLava the {@code boolean} argument; whether the block is inside or bordering a lava pool
     * @return {@code byte}; the block ID corresponding to the surface material
     */
    private byte islandSurfaceBlock(int x, int z, boolean inLake, boolean inLava) {
        if (inLake || inLava || isCoast(x, z)) return BlockData.SAND.getId();
        return BlockData.GRASS.getId();
    }

    /**
     * Calculates the island surface terrain height at a world position using distance attenuation and noise.
     * @param x the {@code int} argument; the world x value
     * @param z the {@code int} argument; the world z value
     * @return {@code int}; the calculated terrain height
     */
    private int terrainHeight(int x, int z) {
        float boundary = islandBoundary(x, z);
        float inland = Math.clamp(1.0f - distance(x, z, CENTER_X, CENTER_Z) / boundary,
                0.0f, 1.0f);
        float generatedHeight = SEA_LEVEL + ISLAND_HEIGHT * inland * inland
                + noise(x, z, 5) * inland;

        float spawnDistance = distance(x, z, CENTER_X, CENTER_Z);
        if (spawnDistance < SPAWN_PLATFORM_RADIUS + SPAWN_PLATFORM_TRANSITION) {
            float transition = Math.clamp((spawnDistance - SPAWN_PLATFORM_RADIUS)
                    / SPAWN_PLATFORM_TRANSITION, 0.0f, 1.0f);
            float smooth = transition * transition * (3.0f - 2.0f * transition);
            float spawnHeight = SEA_LEVEL + ISLAND_HEIGHT;
            generatedHeight = spawnHeight + (generatedHeight - spawnHeight) * smooth;
        }

        return Math.round(generatedHeight);
    }

    /**
     * Raises the seabed in broad terraces near the island and lets it descend
     * naturally to the configured ocean depth.
     * @param x the {@code int} argument; the world x value
     * @param z the {@code int} argument; the world z value
     * @return {@code int}; the seabed height at the position
     */
    private int seabedHeight(int x, int z) {
        float outside = Math.max(0.0f,
                distance(x, z, CENTER_X, CENTER_Z) - islandBoundary(x, z));
        int descent = 1 + (int) (outside / SEABED_TERRACE_WIDTH);
        return Math.max(OCEAN_FLOOR_Y, SEA_LEVEL - descent);
    }

    /**
     * Checks whether a horizontal position falls within the island boundary.
     * @param x the {@code int} argument; the world x value
     * @param z the {@code int} argument; the world z value
     * @return {@code true} if the position is on the island; otherwise {@code false}
     */
    private boolean isIsland(int x, int z) {
        return distance(x, z, CENTER_X, CENTER_Z) <= islandBoundary(x, z);
    }

    /**
     * Calculates the noisy radial boundary of the island at a given position.
     * @param x the {@code int} argument; the world x value
     * @param z the {@code int} argument; the world z value
     * @return {@code float}; the maximum island radius at this angle
     */
    private float islandBoundary(int x, int z) {
        return ISLAND_RADIUS + noise(x, z, 5) * 1.6f + noise(x, z, 11) * 1.2f;
    }

    /**
     * Checks whether a horizontal position belongs to the coastal transition zone.
     * @param x the {@code int} argument; the world x value
     * @param z the {@code int} argument; the world z value
     * @return {@code true} if the position is a coast block; otherwise {@code false}
     */
    private boolean isCoast(int x, int z) {
        float edgeDistance = islandBoundary(x, z) - distance(x, z, CENTER_X, CENTER_Z);
        return edgeDistance <= COAST_WIDTH || terrainHeight(x, z) <= SEA_LEVEL + 1;
    }

    /**
     * Selects a valid interior position and height for the primary lake.
     * @param random the {@link Random} supplied as {@code random}
     * @return the {@link Lake} representing the configured lake instance
     */
    private Lake chooseLake(Random random) {
        for (int attempt = 0; attempt < 100; attempt++) {
            int x = random.nextInt(41) - 20;
            int z = random.nextInt(41) - 20;
            float centerDistance = distance(x, z, CENTER_X, CENTER_Z);
            float lakeEdge = LAKE_RADIUS + 0.75f + 1.0f;
            if (!isIsland(x, z) || centerDistance < NPC_SAFE_RADIUS + lakeEdge
                    || centerDistance > ISLAND_RADIUS - 2.0f
                    || terrainHeight(x, z) <= SEA_LEVEL) continue;
            int surfaceY = Math.max(SEA_LEVEL + 1, terrainHeight(x, z) - 1);
            return new Lake(x, z, surfaceY);
        }
        return null;
    }

    /**
     * Attempts to select a valid position for a lava pool away from spawn and the lake.
     * @param random the {@link Random} supplied as {@code random}
     * @return the {@link LavaPool} instance, or {@code null} if no valid placement was found
     */
    private LavaPool chooseLavaPool(Random random) {
        for (int attempt = 0; attempt < 100; attempt++) {
            int x = random.nextInt(41) - 20;
            int z = random.nextInt(41) - 20;
            float centerDistance = distance(x, z, CENTER_X, CENTER_Z);
            float lavaEdge = LAVA_RADIUS + 0.3f + 1.0f;
            if (!isIsland(x, z) || centerDistance < NPC_SAFE_RADIUS + lavaEdge
                    || centerDistance > ISLAND_RADIUS - 2.0f
                    || terrainHeight(x, z) <= SEA_LEVEL
                    || (lake != null
                    && distance(x, z, lake.x, lake.z) < LAKE_RADIUS + LAVA_RADIUS + 3.0f)) {
                continue;
            }
            return new LavaPool(x, z, terrainHeight(x, z));
        }
        return null;
    }

    /**
     * Selects valid positions for tree placement across the island.
     * @param random the {@link Random} supplied as {@code random}
     */
    private void chooseTrees(Random random) {
        int requested = 4 + random.nextInt(3);
        for (int attempt = 0; trees.size() < requested && attempt < 500; attempt++) {
            int x = random.nextInt(27) - 13;
            int z = random.nextInt(27) - 13;
            if (!isIsland(x, z) || isCoast(x, z) || isInLake(x, z) || isInLavaPool(x, z)
                    || distance(x, z, CENTER_X, CENTER_Z) < 4.0f
                    || (lake != null && distance(x, z, lake.x, lake.z) < LAKE_RADIUS + 2.0f)
                    || nearTree(x, z)) continue;
            trees.add(new Tree(x, terrainHeight(x, z), z,
                    4 + random.nextInt(4), random.nextLong()));
        }
    }

    /**
     * Checks whether a position is too close to an existing tree.
     * @param x the {@code int} argument; the world x value
     * @param z the {@code int} argument; the world z value
     * @return {@code true} if another tree is within minimum clearance; otherwise {@code false}
     */
    private boolean nearTree(int x, int z) {
        for (Tree tree : trees) {
            if (distance(x, z, tree.x, tree.z) < 5.0f) return true;
        }
        return false;
    }

    /**
     * Checks whether a horizontal position falls within the organic lake boundary.
     * @param x the {@code int} argument; the world x value
     * @param z the {@code int} argument; the world z value
     * @return {@code true} if the position is inside the lake; otherwise {@code false}
     */
    private boolean isInLake(int x, int z) {
        return lake != null && distance(x, z, lake.x, lake.z)
                <= LAKE_RADIUS + noise(x, z, 3) * 0.75f;
    }

    /**
     * Checks whether a horizontal position falls within the lava pool boundary.
     * @param x the {@code int} argument; the world x value
     * @param z the {@code int} argument; the world z value
     * @return {@code true} if the position is inside the lava pool; otherwise {@code false}
     */
    private boolean isInLavaPool(int x, int z) {
        return lavaPool != null && distance(x, z, lavaPool.x, lavaPool.z)
                <= LAVA_RADIUS + noise(x, z, 2) * 0.3f;
    }

    /**
     * Checks whether a position immediately borders the lake.
     * @param x the {@code int} argument; the world x value
     * @param z the {@code int} argument; the world z value
     * @return {@code true} if adjacent to a lake block; otherwise {@code false}
     */
    private boolean isLakeBank(int x, int z) {
        if (isInLake(x, z)) return false;
        return isInLake(x + 1, z) || isInLake(x - 1, z)
                || isInLake(x, z + 1) || isInLake(x, z - 1);
    }

    /**
     * Checks whether a position immediately borders the lava pool.
     * @param x the {@code int} argument; the world x value
     * @param z the {@code int} argument; the world z value
     * @return {@code true} if adjacent to a lava block; otherwise {@code false}
     */
    private boolean isLavaBank(int x, int z) {
        if (lavaPool == null || isInLavaPool(x, z)) return false;
        return isInLavaPool(x + 1, z) || isInLavaPool(x - 1, z)
                || isInLavaPool(x, z + 1) || isInLavaPool(x, z - 1);
    }

    /**
     * Fills a vertical column within a chunk with fluid blocks and full fluid levels.
     * @param chunk the {@link Chunk} supplied as {@code chunk}
     * @param x the {@code int} argument; the local chunk x coordinate
     * @param z the {@code int} argument; the local chunk z coordinate
     * @param bottomY the {@code int} argument; the starting vertical level
     * @param topY the {@code int} argument; the ending vertical level
     * @param fluidId the {@code byte} argument; the block ID of the fluid
     */
    private void fillFluidColumn(Chunk chunk, int x, int z, int bottomY, int topY, byte fluidId) {
        for (int y = bottomY; y <= topY; y++) {
            chunk.setBlock(x, y, z, fluidId);
            chunk.setFluidLevel(x, y, z, (byte) 8);
        }
    }

    /**
     * Registers generated water blocks in a chunk as fluid simulation sources.
     * @param chunk the {@link Chunk} supplied as {@code chunk}
     * @param chunkX the {@code int} supplied as {@code chunkX}
     * @param chunkZ the {@code int} supplied as {@code chunkZ}
     */
    private void registerWaterSources(Chunk chunk, int chunkX, int chunkZ) {
        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                int worldX = chunkX * Chunk.SIZE_X + x;
                int worldZ = chunkZ * Chunk.SIZE_Z + z;
                if (isIsland(worldX, worldZ)) {
                    if (isInLake(worldX, worldZ)
                            && chunk.getBlock(x, lake.surfaceY, z) == waterSimulation.getFluidType().getId()) {
                        waterSimulation.addSource(worldX, lake.surfaceY, worldZ);
                    }
                    continue;
                }

                if (chunk.getBlock(x, SEA_LEVEL, z) == waterSimulation.getFluidType().getId()) {
                    waterSimulation.addSource(worldX, SEA_LEVEL, worldZ);
                    chunk.setGeneratedOceanWater(x, SEA_LEVEL, z, true);
                }
            }
        }
    }

    /**
     * Registers generated lava pool blocks in a chunk to the global lava simulation.
     * @param chunkX the {@code int} supplied as {@code chunkX}
     * @param chunkZ the {@code int} supplied as {@code chunkZ}
     */
    private void registerLavaSources(int chunkX, int chunkZ) {
        if (lavaPool == null) return;
        for (int x = (int) Math.floor(lavaPool.x - LAVA_RADIUS - 1);
             x <= (int) Math.ceil(lavaPool.x + LAVA_RADIUS + 1); x++) {
            for (int z = (int) Math.floor(lavaPool.z - LAVA_RADIUS - 1);
                 z <= (int) Math.ceil(lavaPool.z + LAVA_RADIUS + 1); z++) {
                if (!isInLavaPool(x, z)
                        || Math.floorDiv(x, Chunk.SIZE_X) != chunkX
                        || Math.floorDiv(z, Chunk.SIZE_Z) != chunkZ) continue;
                LavaSimulation.ls.addSource(x, lavaPool.surfaceY, z);
            }
        }
    }

    /**
     * Generates every selected tree owned by a chunk.
     * @param chunkX the {@code int} supplied as {@code chunkX}
     * @param chunkZ the {@code int} supplied as {@code chunkZ}
     */
    private void generateTreesInChunk(int chunkX, int chunkZ) {
        for (Tree tree : trees) {
            if (Math.floorDiv(tree.x, Chunk.SIZE_X) != chunkX
                    || Math.floorDiv(tree.z, Chunk.SIZE_Z) != chunkZ) continue;
            generateTree(tree);
        }
    }

    /**
     * Builds the trunk and canopy blocks for a single tree instance in the world.
     * @param tree the {@link Tree} argument; the tree descriptor to generate
     */
    private void generateTree(Tree tree) {
        Random random = new Random(tree.seed);
        for (int y = 1; y <= tree.height; y++) {
            World.wrld.setBlockTypeAt(tree.x, tree.surfaceY + y, tree.z, BlockData.OAK_LOG.getId());
        }

        int topY = tree.surfaceY + tree.height;
        for (int y = topY - 2; y <= topY + 1; y++) {
            int radius = y == topY + 1 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius
                            && random.nextFloat() < 0.4f) continue;
                    if (World.wrld.getBlockTypeAt(tree.x + dx, y, tree.z + dz) == BlockData.AIR.getId()) {
                        World.wrld.setBlockTypeAt(tree.x + dx, y, tree.z + dz,
                                BlockData.OAK_LEAVES.getId());
                    }
                }
            }
        }
    }

    /**
     * Generates decorative plants on exposed grass or dirt in one chunk.
     */
    private void generatePlants(Chunk chunk, Random random) {
        if (DECORATIVE_PLANTS.length == 0) return;
        for (int attempt = 0; attempt < PLANT_ATTEMPTS_PER_CHUNK; attempt++) {
            int x = random.nextInt(Chunk.SIZE_X);
            int z = random.nextInt(Chunk.SIZE_Z);
            int surfaceY = findPlantableSurface(chunk, x, z);
            if (surfaceY < 0) continue;

            BlockData plant = DECORATIVE_PLANTS[random.nextInt(DECORATIVE_PLANTS.length)];
            chunk.setBlock(x, surfaceY + 1, z, plant.getId());
            if (plant == BlockData.TALL_GRASS) {
                generateTallGrassCluster(chunk, x, surfaceY, z, random);
            }
        }
    }

    /**
     * Expands tall grass around its initial plant while staying inside the
     * chunk currently being generated.
     */
    private void generateTallGrassCluster(Chunk chunk, int centerX, int centerY,
                                          int centerZ, Random random) {
        int placed = 1;
        for (int attempt = 0; attempt < TALL_GRASS_CLUSTER_ATTEMPTS
                && placed < TALL_GRASS_CLUSTER_SIZE; attempt++) {
            int x = centerX + random.nextInt(5) - 2;
            int z = centerZ + random.nextInt(5) - 2;
            if (x < 0 || x >= Chunk.SIZE_X || z < 0 || z >= Chunk.SIZE_Z) continue;

            int surfaceY = findPlantableSurface(chunk, x, z);
            if (surfaceY < 0 || Math.abs(surfaceY - centerY) > 2) continue;
            chunk.setBlock(x, surfaceY + 1, z, BlockData.TALL_GRASS.getId());
            placed++;
        }
    }

    /** Returns the highest exposed grass or dirt block in a chunk column. */
    private int findPlantableSurface(Chunk chunk, int x, int z) {
        for (int y = Chunk.SIZE_Y - 2; y >= 0; y--) {
            byte block = chunk.getBlock(x, y, z);
            if (block == BlockData.AIR.getId()) continue;
            if ((block == BlockData.GRASS.getId() || block == BlockData.DIRT.getId())
                    && chunk.getBlock(x, y + 1, z) == BlockData.AIR.getId()) {
                return y;
            }
            return -1;
        }
        return -1;
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
     * Returns deterministic value noise for a world position.
     * @param x the {@code int} argument; the world x value
     * @param z the {@code int} argument; the world z value
     * @param scale the {@code int} argument; the noise cell scale
     * @return {@code float}; a noise value between {@code -1} and {@code 1}
     */
    private float noise(int x, int z, int scale) {
        long value = seed + (long) Math.floorDiv(x, scale) * 341873128712L
                + (long) Math.floorDiv(z, scale) * 132897987541L;
        value = (value ^ value >>> 30) * 0xbf58476d1ce4e5b9L;
        value = (value ^ value >>> 27) * 0x94d049bb133111ebL;
        value ^= value >>> 31;
        return ((value & 0xffffL) / 32767.5f) - 1.0f;
    }

    /**
     * Returns the horizontal distance between two positions.
     * @param x1 the {@code int} argument; the first x value
     * @param z1 the {@code int} argument; the first z value
     * @param x2 the {@code int} argument; the second x value
     * @param z2 the {@code int} argument; the second z value
     * @return {@code float}; the horizontal distance
     */
    private static float distance(int x1, int z1, int x2, int z2) {
        return (float) Math.hypot(x1 - x2, z1 - z2);
    }

    /**
     * Stores the position and surface height of a generated lake.
     */
    private record Lake(int x, int z, int surfaceY) {}

    /**
     * Stores the position and surface height of a generated lava pool.
     */
    private record LavaPool(int x, int z, int surfaceY) {}

    /**
     * Stores the position, dimensions, and seed of a generated tree.
     */
    private record Tree(int x, int surfaceY, int z, int height, long seed) {}
}
