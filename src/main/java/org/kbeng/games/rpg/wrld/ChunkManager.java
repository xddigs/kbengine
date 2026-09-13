package org.kbeng.games.rpg.wrld;

import org.kbeng.engine.utils.Settings;
import org.kbeng.games.rpg.data.BlockData;
import org.kbeng.games.rpg.data.SoilPosition;
import org.kbeng.games.rpg.graphics.ChunkMeshBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ChunkManager provides chunk manager capabilities within the wrld subsystem.
 * It maintains world simulation concerns including terrain, chunks, fluids, and authoritative spatial state.
 * The manager coordinates lifecycle and ordering concerns across dependent runtime components.
 */
public class ChunkManager {
    private final org.kbeng.games.rpg.graphics.VoxelTerrain voxelTerrain =
            new org.kbeng.games.rpg.graphics.VoxelTerrain(World.wrld.voxels());

    /** Active packed-colour terrain renderer; legacy textured meshes remain archived. */
    public org.kbeng.games.rpg.graphics.VoxelTerrain getVoxelTerrain() { return voxelTerrain; }
    private static final float SOIL_GRASS_TIME = 10.0f;
    private static final int MAX_MESH_UPLOADS_PER_FRAME = 2;
    private final Generator generator;
    private final Map<Chunk, ChunkMeshBuilder.ChunkRenderMesh> chunkMeshes;
    private final Map<SoilPosition, Float> soilTimers;
    private final ExecutorService meshExecutor;
    private final ConcurrentLinkedDeque<MeshBuildResult> completedMeshes = new ConcurrentLinkedDeque<>();
    private final Set<Long> buildingChunks = ConcurrentHashMap.newKeySet();
    private final Set<Long> dirtyChunks = new HashSet<>();
    private final Map<Long, Long> meshVersions = new ConcurrentHashMap<>();
    private final AtomicLong meshBuildSequence = new AtomicLong();

    private int lastPlayerChunkX = Integer.MAX_VALUE;
    private int lastPlayerChunkZ = Integer.MAX_VALUE;

    /**
     * Creates a new {@code ChunkManager} instance.
     * @param fluidSimulation the {@link FluidSimulation} argument;
     *                        the fluid simulation used by the world generator
     */
    public ChunkManager(FluidSimulation fluidSimulation) {
        this.generator = World.wrld.voxels()::generate;
        this.chunkMeshes = new HashMap<>();
        this.soilTimers = new HashMap<>();
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
        this.meshExecutor = new ThreadPoolExecutor(threads, threads,
                0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<>());
    }

    /**
     * Updates the current state.
     * @param playerX the {@code float} supplied as {@code playerX}
     * @param playerZ the {@code float} supplied as {@code playerZ}
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void update(float playerX, float playerZ, float delta) {
        voxelTerrain.update(playerX, playerZ);
    }

    /** @deprecated Archived whole-block streaming and soil simulation. */
    @Deprecated
    private void updateLegacy(float playerX, float playerZ, float delta) {
        processCompletedMeshes();
        updateSoil(delta);

        int playerChunkX = Math.floorDiv((int) playerX, Chunk.SIZE_X);
        int playerChunkZ = Math.floorDiv((int) playerZ, Chunk.SIZE_Z);

        if (playerChunkX != lastPlayerChunkX || playerChunkZ != lastPlayerChunkZ) {
            updateLoadedChunks(playerChunkX, playerChunkZ);
            lastPlayerChunkX = playerChunkX;
            lastPlayerChunkZ = playerChunkZ;
        }
    }

    /**
     * Creates single chunk mesh from the supplied state and configuration.
     * @param chunkX the {@code int} supplied as {@code chunkX}
     * @param chunkZ the {@code int} supplied as {@code chunkZ}
     */
    public void buildSingleChunkMesh(int chunkX, int chunkZ) {
        voxelTerrain.build(chunkX, chunkZ);
    }

    /** @deprecated Archived textured 1-unit mesh upload. */
    @Deprecated
    private void buildLegacyChunkMesh(int chunkX, int chunkZ) {
        Chunk chunk = World.wrld.getChunks().get(World.wrld.get2DKey(chunkX, chunkZ));
        if (chunk == null) return;

        if (!chunkMeshes.containsKey(chunk)) {
            ChunkMeshBuilder.ChunkMeshData data = ChunkMeshBuilder.buildMesh(World.wrld, chunk);
            ChunkMeshBuilder.ChunkRenderMesh renderMesh = ChunkMeshBuilder.createMesh(data);
            if (renderMesh != null) {
                chunkMeshes.put(chunk, renderMesh);
            }
        }
    }

    /**
     * Updates the loaded chunks.
     * @param centerChunkX the {@code int} supplied as {@code centerChunkX}
     * @param centerChunkZ the {@code int} supplied as {@code centerChunkZ}
     */
    public void updateLoadedChunks(int centerChunkX, int centerChunkZ) {
        voxelTerrain.request(centerChunkX, centerChunkZ);
    }

    /** @deprecated Archived whole-block streaming implementation. */
    @Deprecated
    private void updateLegacyLoadedChunks(int centerChunkX, int centerChunkZ) {
        int r = Settings.getRenderDistance();
        int unloadDist = r + Settings.getUnloadMargin();
        int rSquared = r * r;

        chunkMeshes.entrySet().removeIf(entry -> {
            Chunk chunk = entry.getKey();
            int dx = Math.abs(chunk.getChunkX() - centerChunkX);
            int dz = Math.abs(chunk.getChunkZ() - centerChunkZ);

            if (dx > unloadDist || dz > unloadDist) {
                ChunkMeshBuilder.ChunkRenderMesh renderMesh = entry.getValue();
                if (renderMesh != null) {
                    renderMesh.dispose();
                }

                long key = World.wrld.get2DKey(chunk.getChunkX(), chunk.getChunkZ());
                World.wrld.getChunks().remove(key);
                buildingChunks.remove(key);
                meshVersions.remove(key);
                dirtyChunks.remove(key);
                cleanupSoilTimersForChunk(chunk);
                return true;
            }
            return false;
        });

        for (int cx = centerChunkX - r; cx <= centerChunkX + r; cx++) {
            for (int cz = centerChunkZ - r; cz <= centerChunkZ + r; cz++) {
                if ((cx - centerChunkX) * (cx - centerChunkX) + (cz - centerChunkZ) * (cz - centerChunkZ) > rSquared) {
                    continue;
                }

                long key = World.wrld.get2DKey(cx, cz);
                if (!World.wrld.getChunks().containsKey(key)) {
                    generator.generateChunk(cx, cz);
                    updateGrass(cx, cz);
                }
            }
        }

        for (int cx = centerChunkX - r; cx <= centerChunkX + r; cx++) {
            for (int cz = centerChunkZ - r; cz <= centerChunkZ + r; cz++) {
                if ((cx - centerChunkX) * (cx - centerChunkX) + (cz - centerChunkZ) * (cz - centerChunkZ) > rSquared) {
                    continue;
                }

                Chunk chunk = World.wrld.getChunks().get(World.wrld.get2DKey(cx, cz));
                if (chunk == null) continue;

                if (!chunkMeshes.containsKey(chunk)) {
                    queueMeshBuild(chunk);
                }
            }
        }
    }

    /**
     * Refreshes dependent runtime state for queue mesh build.
     * @param chunk the {@link Chunk} supplied as {@code chunk}
     */
    private void queueMeshBuild(Chunk chunk) {
        queueMeshBuild(chunk, false);
    }

    /** Queues player-visible rebuilds ahead of background chunk generation. */
    private void queueMeshBuild(Chunk chunk, boolean prioritized) {
        long key = World.wrld.get2DKey(chunk.getChunkX(), chunk.getChunkZ());

        if (meshExecutor.isShutdown()) {
            return;
        }

        if (!buildingChunks.add(key)) {
            return;
        }

        long version = meshVersions.getOrDefault(key, 0L);
        dirtyChunks.remove(key);
        meshExecutor.execute(new MeshBuildTask(chunk, key, version, prioritized,
                meshBuildSequence.getAndIncrement()));
    }

    /**
     * Processes completed meshes and applies the resulting state changes.
     */
    private void processCompletedMeshes() {
        MeshBuildResult result;
        int processed = 0;
        while (processed < MAX_MESH_UPLOADS_PER_FRAME
                && (result = completedMeshes.pollFirst()) != null) {
            processed++;
            Chunk chunk = result.chunk();
            long key = World.wrld.get2DKey(chunk.getChunkX(), chunk.getChunkZ());

            if (!World.wrld.getChunks().containsKey(key)) {
                continue;
            }

            if (result.version() != meshVersions.getOrDefault(key, 0L)) {
                if (dirtyChunks.contains(key)) queueMeshBuild(chunk, true);
                continue;
            }

            ChunkMeshBuilder.ChunkRenderMesh oldMesh = chunkMeshes.get(chunk);
            if (oldMesh != null) {
                oldMesh.dispose();
            }

            ChunkMeshBuilder.ChunkRenderMesh renderMesh = ChunkMeshBuilder.createMesh(result.data());
            if (renderMesh != null) {
                chunkMeshes.put(chunk, renderMesh);
            }
        }
    }

    /**
     * Rebuilds chunk mesh at from the authoritative runtime state.
     * @param worldX the {@code int} supplied as {@code worldX}
     * @param worldZ the {@code int} supplied as {@code worldZ}
     */
    public void rebuildChunkMeshAt(int worldX, int worldZ) {
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE_Z);

        updateGrassColumn(worldX, worldZ);
        rebuildSingleChunk(chunkX, chunkZ);

        int localX = Math.floorMod(worldX, Chunk.SIZE_X);
        int localZ = Math.floorMod(worldZ, Chunk.SIZE_Z);

        if (localX == 0) rebuildSingleChunk(chunkX - 1, chunkZ);
        if (localX == Chunk.SIZE_X - 1) rebuildSingleChunk(chunkX + 1, chunkZ);
        if (localZ == 0) rebuildSingleChunk(chunkX, chunkZ - 1);
        if (localZ == Chunk.SIZE_Z - 1) rebuildSingleChunk(chunkX, chunkZ + 1);
    }

    /**
     * Rebuilds the chunks around a breaking block on the render thread.
     * @param worldX the world x coordinate of the breaking block
     * @param worldZ the world z coordinate of the breaking block
     */
    public void rebuildBreakingChunkMeshAt(int worldX, int worldZ) {
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE_Z);
        rebuildSingleChunkImmediately(chunkX, chunkZ);

        int localX = Math.floorMod(worldX, Chunk.SIZE_X);
        int localZ = Math.floorMod(worldZ, Chunk.SIZE_Z);

        if (localX == 0) rebuildSingleChunkImmediately(chunkX - 1, chunkZ);
        if (localX == Chunk.SIZE_X - 1) rebuildSingleChunkImmediately(chunkX + 1, chunkZ);
        if (localZ == 0) rebuildSingleChunkImmediately(chunkX, chunkZ - 1);
        if (localZ == Chunk.SIZE_Z - 1) rebuildSingleChunkImmediately(chunkX, chunkZ + 1);
    }

    /**
     * Rebuilds single chunk from the authoritative runtime state.
     * @param cx the {@code int} supplied as {@code cx}
     * @param cz the {@code int} supplied as {@code cz}
     */
    private void rebuildSingleChunk(int cx, int cz) {
        long key = World.wrld.get2DKey(cx, cz);
        Chunk chunk = World.wrld.getChunks().get(key);
        if (chunk != null) {
            meshVersions.merge(key, 1L, Long::sum);
            dirtyChunks.add(key);
            queueMeshBuild(chunk, true);
        }
    }

    /**
     * Replaces one chunk mesh immediately and invalidates pending asynchronous builds.
     * @param cx the chunk x coordinate
     * @param cz the chunk z coordinate
     */
    private void rebuildSingleChunkImmediately(int cx, int cz) {
        long key = World.wrld.get2DKey(cx, cz);
        Chunk chunk = World.wrld.getChunks().get(key);
        if (chunk == null) return;

        meshVersions.merge(key, 1L, Long::sum);
        dirtyChunks.remove(key);
        ChunkMeshBuilder.ChunkRenderMesh oldMesh = chunkMeshes.get(chunk);
        ChunkMeshBuilder.ChunkRenderMesh renderMesh = ChunkMeshBuilder.createMesh(
                ChunkMeshBuilder.buildMesh(World.wrld, chunk));
        if (oldMesh != null) oldMesh.dispose();
        chunkMeshes.put(chunk, renderMesh);
    }

    /**
     * Updates the grass.
     * @param chunkX the {@code int} supplied as {@code chunkX}
     * @param chunkZ the {@code int} supplied as {@code chunkZ}
     */
    private void updateGrass(int chunkX, int chunkZ) {
        int startX = chunkX * Chunk.SIZE_X;
        int startZ = chunkZ * Chunk.SIZE_Z;

        for (int localX = 0; localX < Chunk.SIZE_X; localX++) {
            for (int localZ = 0; localZ < Chunk.SIZE_Z; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;

                for (int y = Chunk.SIZE_Y - 2; y >= 0; y--) {
                    byte block = World.wrld.getBlockTypeAt(worldX, y, worldZ);

                    if (isSoil(block)) {
                        if (isExposedToAir(worldX, y, worldZ)) {
                            startSoilTimer(worldX, y, worldZ);
                        }
                    }
                }
            }
        }
    }

    /** Updates soil exposure only in the column affected by a block change. */
    private void updateGrassColumn(int worldX, int worldZ) {
        for (int y = Chunk.SIZE_Y - 2; y >= 0; y--) {
            byte block = World.wrld.getBlockTypeAt(worldX, y, worldZ);
            boolean exposedToAir = isExposedToAir(worldX, y, worldZ);
            if (block == BlockData.GRASS.getId() && !exposedToAir) {
                World.wrld.setBlockTypeAt(worldX, y, worldZ, BlockData.DIRT.getId());
                soilTimers.remove(new SoilPosition(worldX, y, worldZ));
            } else if (isSoil(block) && exposedToAir) {
                startSoilTimer(worldX, y, worldZ);
            }
        }
    }

    /**
     * Updates the soil.
     * @param delta the {@code float} supplied as {@code delta}
     */
    private void updateSoil(float delta) {
        var iterator = soilTimers.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            SoilPosition position = entry.getKey();

            int x = position.x();
            int y = position.y();
            int z = position.z();

            byte block = World.wrld.getBlockTypeAt(x, y, z);

            if (!isSoil(block)) {
                iterator.remove();
                continue;
            }

            if (!isExposedToAir(x, y, z)) {
                iterator.remove();
                continue;
            }

            if (block == BlockData.TILLED_DIRT.getId() && hasWaterNearby(x, y, z)) {
                continue;
            }

            float remaining = entry.getValue() - delta;

            if (remaining <= 0.0f) {
                World.wrld.setBlockTypeAt(x, y, z, BlockData.GRASS.getId());
                rebuildChunkMeshAt(x, z);
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    /**
     * Refreshes dependent runtime state for start soil timer.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     */
    private void startSoilTimer(int x, int y, int z) {
        SoilPosition position = new SoilPosition(x, y, z);
        soilTimers.putIfAbsent(position, SOIL_GRASS_TIME);
    }

    /**
     * Checks whether the soil condition is met.
     * @param block the {@code byte} supplied as {@code block}
     * @return {@code true} if soil; otherwise {@code false}
     */
    private boolean isSoil(byte block) {
        return block == BlockData.DIRT.getId() || block == BlockData.TILLED_DIRT.getId();
    }

    /**
     * Checks whether the exposed to air condition is met.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code true} if exposed to air; otherwise {@code false}
     */
    private boolean isExposedToAir(int x, int y, int z) {
        return World.wrld.getBlockTypeAt(x, y + 1, z) == BlockData.AIR.getId();
    }

    /**
     * Checks whether the water nearby condition is met.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code true} if water nearby; otherwise {@code false}
     */
    private boolean hasWaterNearby(int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (World.wrld.getBlockTypeAt(x + dx, y, z + dz) == BlockData.WATER.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Releases the resources associated with soil timers for chunk.
     * @param chunk the {@link Chunk} supplied as {@code chunk}
     */
    private void cleanupSoilTimersForChunk(Chunk chunk) {
        int minX = chunk.getChunkX() * Chunk.SIZE_X;
        int maxX = minX + Chunk.SIZE_X;
        int minZ = chunk.getChunkZ() * Chunk.SIZE_Z;
        int maxZ = minZ + Chunk.SIZE_Z;

        soilTimers.keySet().removeIf(pos -> pos.x() >= minX && pos.x() < maxX && pos.z() >= minZ && pos.z() < maxZ);
    }

    /**
     * Returns the chunk meshes.
     * @return the {@link Map} representing the chunk meshes
     */
    public Map<Chunk, ChunkMeshBuilder.ChunkRenderMesh> getChunkMeshes() {
        return chunkMeshes;
    }

    /**
     * Returns the dirty chunks.
     * @return the {@link Set} representing the dirty chunks
     */
    public Set<Long> getDirtyChunks() {
        return dirtyChunks;
    }

    /**
     * Releases the resources associated with this object.
     */
    public void shutdown() {
        meshExecutor.shutdownNow();
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        voxelTerrain.close();
        meshExecutor.shutdownNow();
        soilTimers.clear();
        completedMeshes.clear();
        buildingChunks.clear();
        meshVersions.clear();
        dirtyChunks.clear();
        chunkMeshes.values().forEach(ChunkMeshBuilder.ChunkRenderMesh::dispose);
        chunkMeshes.clear();
    }

    /**
     * Returns the last player chunk x.
     * @return {@code int}; the last player chunk x
     */
    public int getLastPlayerChunkX() { return lastPlayerChunkX; }
    /**
     * Sets the last player chunk x.
     * @param x the {@code int} supplied as {@code x}
     */
    public void setLastPlayerChunkX(int x) { this.lastPlayerChunkX = x; }
    /**
     * Returns the last player chunk z.
     * @return {@code int}; the last player chunk z
     */
    public int getLastPlayerChunkZ() { return lastPlayerChunkZ; }
    /**
     * Sets the last player chunk z.
     * @param z the {@code int} supplied as {@code z}
     */
    public void setLastPlayerChunkZ(int z) { this.lastPlayerChunkZ = z; }
    /**
     * Returns the generator.
     * @return the {@link Generator} representing the generator
     */
    public Generator getGenerator() { return generator; }

    /**
     * Immutable value object containing mesh build result.
     */
    private record MeshBuildResult(Chunk chunk, ChunkMeshBuilder.ChunkMeshData data,
                                   long version) {}

    /** Prioritized CPU-side mesh build. GPU upload remains on the render thread. */
    private final class MeshBuildTask implements Runnable, Comparable<MeshBuildTask> {
        private final Chunk chunk;
        private final long key;
        private final long version;
        private final boolean prioritized;
        private final long sequence;

        private MeshBuildTask(Chunk chunk, long key, long version,
                              boolean prioritized, long sequence) {
            this.chunk = chunk;
            this.key = key;
            this.version = version;
            this.prioritized = prioritized;
            this.sequence = sequence;
        }

        @Override
        public void run() {
            ChunkMeshBuilder.ChunkMeshData data;
            try {
                data = ChunkMeshBuilder.buildMesh(World.wrld, chunk);
            } finally {
                buildingChunks.remove(key);
            }

            MeshBuildResult result = new MeshBuildResult(chunk, data, version);
            if (prioritized) completedMeshes.addFirst(result);
            else completedMeshes.addLast(result);
        }

        @Override
        public int compareTo(MeshBuildTask other) {
            int priority = Boolean.compare(other.prioritized, prioritized);
            return priority != 0 ? priority : Long.compare(sequence, other.sequence);
        }
    }
}
