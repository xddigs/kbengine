package org.kbeng.games.rpg.graphics;

import org.kbeng.engine.graphics.Mesh;
import org.kbeng.engine.graphics.TextureAtlas;

import org.kbeng.games.rpg.data.BlockData;
import org.kbeng.games.rpg.data.BlockPos;
import org.kbeng.games.rpg.data.BlockShape;
import org.kbeng.engine.utils.K;
import org.kbeng.games.rpg.wrld.Chunk;
import org.kbeng.games.rpg.wrld.FluidSimulation;
import org.kbeng.games.rpg.wrld.World;

import java.util.Arrays;

/**
 * ChunkMeshBuilder provides chunk mesh builder capabilities within the graphics subsystem.
 * It contributes to OpenGL resource ownership, render-state setup, or frame-pipeline execution.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public class ChunkMeshBuilder {
    private static final float PIXEL = 1.0f / K.World.DEFAULT_TEXTURE_SCALE;
    private static final float TILLED_HEIGHT = 1.0f - PIXEL;
    private static final BlockData[] BLOCK_LUT = new BlockData[256];
    private static final int MAX_POSITION_FLOATS = Chunk.SIZE_X * Chunk.SIZE_Y * Chunk.SIZE_Z * 72;
    private static final int MAX_NORMAL_FLOATS = MAX_POSITION_FLOATS;
    private static final int MAX_UV_FLOATS = Chunk.SIZE_X * Chunk.SIZE_Y * Chunk.SIZE_Z * 48;
    private static final int MAX_INDICES = Chunk.SIZE_X * Chunk.SIZE_Y * Chunk.SIZE_Z * 36;
    private static final ThreadLocal<float[]> POS_BUFFER = ThreadLocal.withInitial(() -> new float[MAX_POSITION_FLOATS]);
    private static final ThreadLocal<float[]> NORMAL_BUFFER = ThreadLocal.withInitial(() -> new float[MAX_NORMAL_FLOATS]);
    private static final ThreadLocal<float[]> UV_BUFFER = ThreadLocal.withInitial(() -> new float[MAX_UV_FLOATS]);
    private static final ThreadLocal<int[]> INDEX_BUFFER = ThreadLocal.withInitial(() -> new int[MAX_INDICES]);
    private static final ThreadLocal<float[]> WATER_POS_BUFFER = ThreadLocal.withInitial(() -> new float[MAX_POSITION_FLOATS]);
    private static final ThreadLocal<float[]> WATER_NORMAL_BUFFER = ThreadLocal.withInitial(() -> new float[MAX_NORMAL_FLOATS]);
    private static final ThreadLocal<float[]> WATER_UV_BUFFER = ThreadLocal.withInitial(() -> new float[MAX_UV_FLOATS]);
    private static final ThreadLocal<int[]> WATER_INDEX_BUFFER = ThreadLocal.withInitial(() -> new int[MAX_INDICES]);
    private static final ThreadLocal<boolean[]> OCEAN_MERGED_BUFFER = ThreadLocal.withInitial(
            () -> new boolean[Chunk.SIZE_X * Chunk.SIZE_Y * Chunk.SIZE_Z]);
    private static volatile BlockPos breakingBlock;

    static {
        for (BlockData data : BlockData.values()) {
            BLOCK_LUT[data.getId() & 0xFF] = data;
        }
    }

    /**
     * Immutable value object containing raw mesh.
     */
    public record RawMeshData(float[] positions, float[] normals, float[] uv, int[] indices) {}
    /**
     * Immutable value object containing chunk mesh.
     */
    public record ChunkMeshData(RawMeshData solidData, RawMeshData waterData) {}

    /** Tracks write offsets while assembling transparent water geometry. */
    private record WaterMeshCursor(int position, int normal, int uv, int element, int vertices) {}
    /** Tracks write offsets while assembling solid block geometry. */
    private record MeshCursor(int position, int normal, int uv, int element, int vertices) {}

    /**
     * Immutable value object containing chunk render mesh.
     */
    public record ChunkRenderMesh(Mesh solidMesh, Mesh waterMesh) {
        /**
         * Releases the resources associated with this object.
         */
        public void dispose() {
            if (solidMesh != null) solidMesh.dispose();
            if (waterMesh != null) waterMesh.dispose();
        }
    }

    /**
     * Creates mesh from the supplied state and configuration.
     * @param world the {@link World} supplied as {@code world}
     * @param chunk the {@link Chunk} supplied as {@code chunk}
     * @return the {@link ChunkMeshData} representing the build mesh result
     */
    public static ChunkMeshData buildMesh(World world, Chunk chunk) {
        int posIdx = 0, normIdx = 0, uvIdx = 0, elemIdx = 0, vertexCount = 0;
        int wPosIdx = 0, wNormIdx = 0, wUvIdx = 0, wElemIdx = 0, wVertexCount = 0;

        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();

        int extraShapeBoxes = countExtraShapeBoxes(world, chunk);
        float[] posBuf = ensureFloatCapacity(POS_BUFFER,
                MAX_POSITION_FLOATS + extraShapeBoxes * 72);
        float[] normBuf = ensureFloatCapacity(NORMAL_BUFFER,
                MAX_NORMAL_FLOATS + extraShapeBoxes * 72);
        float[] uvBuf = ensureFloatCapacity(UV_BUFFER,
                MAX_UV_FLOATS + extraShapeBoxes * 48);
        int[] idxBuf = ensureIntCapacity(INDEX_BUFFER,
                MAX_INDICES + extraShapeBoxes * 36);

        float[] wPosBuf = WATER_POS_BUFFER.get();
        float[] wNormBuf = WATER_NORMAL_BUFFER.get();
        float[] wUvBuf = WATER_UV_BUFFER.get();
        int[] wIdxBuf = WATER_INDEX_BUFFER.get();

        WaterMeshCursor oceanCursor = addGreedyOceanSurface(
                world, chunk, wPosBuf, wNormBuf, wUvBuf, wIdxBuf);
        wPosIdx = oceanCursor.position();
        wNormIdx = oceanCursor.normal();
        wUvIdx = oceanCursor.uv();
        wElemIdx = oceanCursor.element();
        wVertexCount = oceanCursor.vertices();

        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int y = 0; y < Chunk.SIZE_Y; y++) {
                for (int z = 0; z < Chunk.SIZE_Z; z++) {
                    byte blockId = chunk.getBlock(x, y, z);
                    if (blockId == 0) continue;

                    BlockData data = BLOCK_LUT[blockId & 0xFF];
                    if (data == null || data.isPlant() || data.isTorch()) continue;

                    int worldX = chunkX * Chunk.SIZE_X + x;
                    int worldZ = chunkZ * Chunk.SIZE_Z + z;
                    BlockShape shape = world.getBlockShapeAt(worldX, y, worldZ);
                    if (data.hasCustomShape()) {
                        MeshCursor cursor = addBlockShape(data, shape, x, y, z,
                                posBuf, normBuf, uvBuf, idxBuf,
                                posIdx, normIdx, uvIdx, elemIdx, vertexCount);
                        posIdx = cursor.position();
                        normIdx = cursor.normal();
                        uvIdx = cursor.uv();
                        elemIdx = cursor.element();
                        vertexCount = cursor.vertices();
                        continue;
                    }

                    float bottomY = y;
                    boolean isWater = data.isFluid();
                    float topY;
                    if (data.isFluid()) {
                        byte level = chunk.getFluidLevel(x, y, z);
                        int waterLevel = (level <= 0) ? 8 : level;
                        boolean hasWaterAbove = y < Chunk.SIZE_Y - 1
                                && chunk.getBlock(x, y + 1, z) == blockId;
                        topY = hasWaterAbove ? (y + 1.0f) : (y + (waterLevel / 8.0f) * TILLED_HEIGHT);
                    } else {
                        topY = y + 1.0f;
                    }

                    float fluidY00 = topY;
                    float fluidY10 = topY;
                    float fluidY11 = topY;
                    float fluidY01 = topY;
                    FluidSimulation.FluidSlope fluidSlope = isWater
                            ? FluidSimulation.getSlope(data, worldX, y, worldZ) : null;
                    if (fluidSlope != null) {
                        if (fluidSlope.dx() > 0) {
                            fluidY10 = bottomY;
                            fluidY11 = bottomY;
                        } else if (fluidSlope.dx() < 0) {
                            fluidY00 = bottomY;
                            fluidY01 = bottomY;
                        } else if (fluidSlope.dz() > 0) {
                            fluidY01 = bottomY;
                            fluidY11 = bottomY;
                        } else {
                            fluidY00 = bottomY;
                            fluidY10 = bottomY;
                        }
                    }

                    boolean renderTopFace = isWater
                            ? !chunk.isGeneratedOceanWater(x, y, z)
                                && shouldRenderWaterTop(world, worldX, y, worldZ, data)
                            : (shouldRenderFace(world, worldX, y + 1, worldZ, data) || getBlockBottomY(world, worldX, y + 1, worldZ) > topY);

                    if (renderTopFace) {
                        TextureAtlas.TextureRegion region = data.getTopRegion();
                        if (region != null) {
                            if (isWater) {
                                float y00 = fluidSlope == null
                                        ? getWaterCornerHeight(world, worldX, y, worldZ, data) : fluidY00;
                                float y10 = fluidSlope == null
                                        ? getWaterCornerHeight(world, worldX + 1, y, worldZ, data) : fluidY10;
                                float y11 = fluidSlope == null
                                        ? getWaterCornerHeight(world, worldX + 1, y, worldZ + 1, data) : fluidY11;
                                float y01 = fluidSlope == null
                                        ? getWaterCornerHeight(world, worldX, y, worldZ + 1, data) : fluidY01;

                                wPosIdx = addQuadPos(wPosBuf, wPosIdx, x, y01, z + 1, x + 1, y11, z + 1, x + 1, y10, z, x, y00, z);

                                wUvIdx = addQuadUV(wUvBuf, wUvIdx, region.uvMin().x, region.uvMax().y, region.uvMax().x, region.uvMax().y, region.uvMax().x, region.uvMin().y, region.uvMin().x, region.uvMin().y);
                                if (fluidSlope == null) {
                                    wNormIdx = addQuadNorm(wNormBuf, wNormIdx, 0, 1, 0);
                                } else {
                                    float rise = topY - bottomY;
                                    float length = (float) Math.sqrt(1.0f + rise * rise);
                                    wNormIdx = addQuadNorm(wNormBuf, wNormIdx,
                                            fluidSlope.dx() * rise / length, 1.0f / length,
                                            fluidSlope.dz() * rise / length);
                                }
                                wElemIdx = addQuadIndices(wIdxBuf, wElemIdx, wVertexCount);
                                wVertexCount += 4;
                            } else {
                                posIdx = addQuadPos(posBuf, posIdx, x, topY, z + 1, x + 1, topY, z + 1, x + 1, topY, z, x, topY, z);
                                uvIdx = addQuadUV(uvBuf, uvIdx, region.uvMin().x, region.uvMax().y, region.uvMax().x, region.uvMax().y, region.uvMax().x, region.uvMin().y, region.uvMin().x, region.uvMin().y);
                                normIdx = addQuadNorm(normBuf, normIdx, 0, 1, 0);
                                elemIdx = addQuadIndices(idxBuf, elemIdx, vertexCount);
                                vertexCount += 4;
                            }
                        }
                    }

                    if (y > 0 && !isWater) {
                        boolean renderFace = shouldRenderFace(world, worldX, y - 1, worldZ, data);
                        float belowTopY = getBlockTopY(world, worldX, y - 1, worldZ);
                        if (renderFace || (belowTopY < bottomY && belowTopY > 0)) {
                            TextureAtlas.TextureRegion region = data.getBottomRegion();
                            if (region != null) {
                                posIdx = addQuadPos(posBuf, posIdx, x, bottomY, z, x + 1, bottomY, z, x + 1, bottomY, z + 1, x, bottomY, z + 1);
                                uvIdx = addQuadUV(uvBuf, uvIdx, region.uvMin().x, region.uvMin().y, region.uvMax().x, region.uvMin().y, region.uvMax().x, region.uvMax().y, region.uvMin().x, region.uvMax().y);
                                normIdx = addQuadNorm(normBuf, normIdx, 0, -1, 0);
                                elemIdx = addQuadIndices(idxBuf, elemIdx, vertexCount);
                                vertexCount += 4;
                            }
                        }
                    }

                    if (shouldRenderFace(world, worldX, y, worldZ + 1, data) || isPartialSideExposure(world, worldX, y, worldZ + 1, data)) {
                        float expBottom = getSideBottomY(world, worldX, y, worldZ + 1, bottomY, data);
                        float uvB = calculateSideUvBottom(expBottom, bottomY, topY);
                        if (isWater) {
                            int next = addSideQuadDirect(wPosBuf, wNormBuf, wUvBuf, wIdxBuf, wPosIdx, wNormIdx, wUvIdx, wElemIdx, wVertexCount, x, x + 1, expBottom, fluidY01, fluidY11, z + 1, z + 1, 0, 0, 1, data, uvB, 1.0f);
                            if (next != wVertexCount) { wVertexCount = next; wPosIdx += 12; wNormIdx += 12; wUvIdx += 8; wElemIdx += 6; }
                        } else {
                            int next = addSideQuadDirect(posBuf, normBuf, uvBuf, idxBuf, posIdx, normIdx, uvIdx, elemIdx, vertexCount, x, x + 1, expBottom, topY, topY, z + 1, z + 1, 0, 0, 1, data, uvB, 1.0f);
                            if (next != vertexCount) { vertexCount = next; posIdx += 12; normIdx += 12; uvIdx += 8; elemIdx += 6; }
                        }
                    }

                    if (shouldRenderFace(world, worldX, y, worldZ - 1, data) || isPartialSideExposure(world, worldX, y, worldZ - 1, data)) {
                        float expBottom = getSideBottomY(world, worldX, y, worldZ - 1, bottomY, data);
                        float uvB = calculateSideUvBottom(expBottom, bottomY, topY);
                        if (isWater) {
                            int next = addSideQuadDirect(wPosBuf, wNormBuf, wUvBuf, wIdxBuf, wPosIdx, wNormIdx, wUvIdx, wElemIdx, wVertexCount, x + 1, x, expBottom, fluidY10, fluidY00, z, z, 0, 0, -1, data, uvB, 1.0f);
                            if (next != wVertexCount) { wVertexCount = next; wPosIdx += 12; wNormIdx += 12; wUvIdx += 8; wElemIdx += 6; }
                        } else {
                            int next = addSideQuadDirect(posBuf, normBuf, uvBuf, idxBuf, posIdx, normIdx, uvIdx, elemIdx, vertexCount, x + 1, x, expBottom, topY, topY, z, z, 0, 0, -1, data, uvB, 1.0f);
                            if (next != vertexCount) { vertexCount = next; posIdx += 12; normIdx += 12; uvIdx += 8; elemIdx += 6; }
                        }
                    }

                    if (shouldRenderFace(world, worldX + 1, y, worldZ, data) || isPartialSideExposure(world, worldX + 1, y, worldZ, data)) {
                        float expBottom = getSideBottomY(world, worldX + 1, y, worldZ, bottomY, data);
                        float uvB = calculateSideUvBottom(expBottom, bottomY, topY);
                        if (isWater) {
                            int next = addSideQuadDirect(wPosBuf, wNormBuf, wUvBuf, wIdxBuf, wPosIdx, wNormIdx, wUvIdx, wElemIdx, wVertexCount, x + 1, x + 1, expBottom, fluidY11, fluidY10, z + 1, z, 1, 0, 0, data, uvB, 1.0f);
                            if (next != wVertexCount) { wVertexCount = next; wPosIdx += 12; wNormIdx += 12; wUvIdx += 8; wElemIdx += 6; }
                        } else {
                            int next = addSideQuadDirect(posBuf, normBuf, uvBuf, idxBuf, posIdx, normIdx, uvIdx, elemIdx, vertexCount, x + 1, x + 1, expBottom, topY, topY, z + 1, z, 1, 0, 0, data, uvB, 1.0f);
                            if (next != vertexCount) { vertexCount = next; posIdx += 12; normIdx += 12; uvIdx += 8; elemIdx += 6; }
                        }
                    }

                    if (shouldRenderFace(world, worldX - 1, y, worldZ, data) || isPartialSideExposure(world, worldX - 1, y, worldZ, data)) {
                        float expBottom = getSideBottomY(world, worldX - 1, y, worldZ, bottomY, data);
                        float uvB = calculateSideUvBottom(expBottom, bottomY, topY);
                        if (isWater) {
                            int next = addSideQuadDirect(wPosBuf, wNormBuf, wUvBuf, wIdxBuf, wPosIdx, wNormIdx, wUvIdx, wElemIdx, wVertexCount, x, x, expBottom, fluidY00, fluidY01, z, z + 1, -1, 0, 0, data, uvB, 1.0f);
                            if (next != wVertexCount) { wVertexCount = next; wPosIdx += 12; wNormIdx += 12; wUvIdx += 8; wElemIdx += 6; }
                        } else {
                            int next = addSideQuadDirect(posBuf, normBuf, uvBuf, idxBuf, posIdx, normIdx, uvIdx, elemIdx, vertexCount, x, x, expBottom, topY, topY, z, z + 1, -1, 0, 0, data, uvB, 1.0f);
                            if (next != vertexCount) { vertexCount = next; posIdx += 12; normIdx += 12; uvIdx += 8; elemIdx += 6; }
                        }
                    }
                }
            }
        }

        RawMeshData solidData = buildRawData(posBuf, posIdx, normBuf, normIdx, uvBuf, uvIdx, idxBuf, elemIdx);
        RawMeshData waterData = buildRawData(wPosBuf, wPosIdx, wNormBuf, wNormIdx, wUvBuf, wUvIdx, wIdxBuf, wElemIdx);
        return new ChunkMeshData(solidData, waterData);
    }

    private static int countExtraShapeBoxes(World world, Chunk chunk) {
        int extraBoxes = 0;
        int chunkWorldX = chunk.getChunkX() * Chunk.SIZE_X;
        int chunkWorldZ = chunk.getChunkZ() * Chunk.SIZE_Z;
        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int y = 0; y < Chunk.SIZE_Y; y++) {
                for (int z = 0; z < Chunk.SIZE_Z; z++) {
                    BlockData data = BLOCK_LUT[chunk.getBlock(x, y, z) & 0xFF];
                    if (data != null && !data.getShape().isFullCube()) {
                        BlockShape shape = world.getBlockShapeAt(
                                chunkWorldX + x, y, chunkWorldZ + z);
                        extraBoxes += shape.getBoxCount() - 1;
                    }
                }
            }
        }
        return extraBoxes;
    }

    private static float[] ensureFloatCapacity(ThreadLocal<float[]> storage, int capacity) {
        float[] buffer = storage.get();
        if (buffer.length >= capacity) return buffer;
        buffer = new float[capacity];
        storage.set(buffer);
        return buffer;
    }

    private static int[] ensureIntCapacity(ThreadLocal<int[]> storage, int capacity) {
        int[] buffer = storage.get();
        if (buffer.length >= capacity) return buffer;
        buffer = new int[capacity];
        storage.set(buffer);
        return buffer;
    }

    /**
     * Merges each contiguous rectangle of generated ocean surface into one quad.
     * Simulated fluids, lakes and player-placed water deliberately remain unmerged.
     */
    private static WaterMeshCursor addGreedyOceanSurface(World world, Chunk chunk,
                                                          float[] positions, float[] normals,
                                                          float[] uv, int[] indices) {
        boolean[] merged = OCEAN_MERGED_BUFFER.get();
        Arrays.fill(merged, false);

        int positionIndex = 0;
        int normalIndex = 0;
        int uvIndex = 0;
        int elementIndex = 0;
        int vertexCount = 0;
        int chunkWorldX = chunk.getChunkX() * Chunk.SIZE_X;
        int chunkWorldZ = chunk.getChunkZ() * Chunk.SIZE_Z;
        TextureAtlas.TextureRegion region = BlockData.WATER.getTopRegion();
        if (region == null) {
            return new WaterMeshCursor(0, 0, 0, 0, 0);
        }

        for (int y = 0; y < Chunk.SIZE_Y; y++) {
            if (!chunk.hasGeneratedOceanWaterAtY(y)) continue;
            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                for (int x = 0; x < Chunk.SIZE_X; x++) {
                    int index = blockIndex(x, y, z);
                    if (merged[index] || !isMergeableOceanSurface(
                            world, chunk, chunkWorldX, chunkWorldZ, x, y, z)) {
                        continue;
                    }

                    int width = 1;
                    while (x + width < Chunk.SIZE_X
                            && !merged[blockIndex(x + width, y, z)]
                            && isMergeableOceanSurface(world, chunk, chunkWorldX, chunkWorldZ,
                                    x + width, y, z)) {
                        width++;
                    }

                    int depth = 1;
                    boolean canExpand = true;
                    while (z + depth < Chunk.SIZE_Z && canExpand) {
                        for (int dx = 0; dx < width; dx++) {
                            int candidate = blockIndex(x + dx, y, z + depth);
                            if (merged[candidate] || !isMergeableOceanSurface(
                                    world, chunk, chunkWorldX, chunkWorldZ,
                                    x + dx, y, z + depth)) {
                                canExpand = false;
                                break;
                            }
                        }
                        if (canExpand) depth++;
                    }

                    for (int dz = 0; dz < depth; dz++) {
                        for (int dx = 0; dx < width; dx++) {
                            merged[blockIndex(x + dx, y, z + dz)] = true;
                        }
                    }

                    float surfaceY = y + TILLED_HEIGHT;
                    positionIndex = addQuadPos(positions, positionIndex,
                            x, surfaceY, z + depth,
                            x + width, surfaceY, z + depth,
                            x + width, surfaceY, z,
                            x, surfaceY, z);
                    uvIndex = addQuadUV(uv, uvIndex,
                            region.uvMin().x, region.uvMax().y,
                            region.uvMax().x, region.uvMax().y,
                            region.uvMax().x, region.uvMin().y,
                            region.uvMin().x, region.uvMin().y);
                    normalIndex = addQuadNorm(normals, normalIndex, 0, 1, 0);
                    elementIndex = addQuadIndices(indices, elementIndex, vertexCount);
                    vertexCount += 4;
                }
            }
        }

        return new WaterMeshCursor(positionIndex, normalIndex, uvIndex, elementIndex, vertexCount);
    }

    private static boolean isMergeableOceanSurface(World world, Chunk chunk,
                                                     int chunkWorldX, int chunkWorldZ,
                                                     int x, int y, int z) {
        return chunk.isGeneratedOceanWater(x, y, z)
                && chunk.getBlock(x, y, z) == BlockData.WATER.getId()
                && shouldRenderWaterTop(world, chunkWorldX + x, y, chunkWorldZ + z,
                        BlockData.WATER);
    }

    private static int blockIndex(int x, int y, int z) {
        return (y * Chunk.SIZE_Z + z) * Chunk.SIZE_X + x;
    }

    /** Adds every cuboid composing a non-full voxel shape to the chunk mesh. */
    private static MeshCursor addBlockShape(BlockData data, BlockShape shape,
                                            int x, int y, int z,
                                            float[] positions, float[] normals,
                                            float[] uv, int[] indices,
                                            int positionIndex, int normalIndex,
                                            int uvIndex, int elementIndex, int vertexCount) {
        for (BlockShape.Box box : shape.getBoxes()) {
            float minX = x + box.minX();
            float minY = y + box.minY();
            float minZ = z + box.minZ();
            float maxX = x + box.maxX();
            float maxY = y + box.maxY();
            float maxZ = z + box.maxZ();

            TextureAtlas.TextureRegion top = data.getTopRegion();
            TextureAtlas.TextureRegion bottom = data.getBottomRegion();
            TextureAtlas.TextureRegion side = data.getSideRegion();

            if (top != null) {
                positionIndex = addQuadPos(positions, positionIndex,
                        minX, maxY, maxZ, maxX, maxY, maxZ,
                        maxX, maxY, minZ, minX, maxY, minZ);
                uvIndex = addQuadUV(uv, uvIndex,
                        atlasU(top, box.minX()), atlasV(top, box.maxZ()),
                        atlasU(top, box.maxX()), atlasV(top, box.maxZ()),
                        atlasU(top, box.maxX()), atlasV(top, box.minZ()),
                        atlasU(top, box.minX()), atlasV(top, box.minZ()));
                normalIndex = addQuadNorm(normals, normalIndex, 0, 1, 0);
                elementIndex = addQuadIndices(indices, elementIndex, vertexCount);
                vertexCount += 4;
            }
            if (bottom != null) {
                positionIndex = addQuadPos(positions, positionIndex,
                        minX, minY, minZ, maxX, minY, minZ,
                        maxX, minY, maxZ, minX, minY, maxZ);
                uvIndex = addQuadUV(uv, uvIndex,
                        atlasU(bottom, box.minX()), atlasV(bottom, box.minZ()),
                        atlasU(bottom, box.maxX()), atlasV(bottom, box.minZ()),
                        atlasU(bottom, box.maxX()), atlasV(bottom, box.maxZ()),
                        atlasU(bottom, box.minX()), atlasV(bottom, box.maxZ()));
                normalIndex = addQuadNorm(normals, normalIndex, 0, -1, 0);
                elementIndex = addQuadIndices(indices, elementIndex, vertexCount);
                vertexCount += 4;
            }
            if (side == null) continue;

            positionIndex = addQuadPos(positions, positionIndex,
                    minX, minY, maxZ, maxX, minY, maxZ,
                    maxX, maxY, maxZ, minX, maxY, maxZ);
            uvIndex = addQuadUV(uv, uvIndex,
                    atlasU(side, box.minX()), atlasSideV(side, box.minY()),
                    atlasU(side, box.maxX()), atlasSideV(side, box.minY()),
                    atlasU(side, box.maxX()), atlasSideV(side, box.maxY()),
                    atlasU(side, box.minX()), atlasSideV(side, box.maxY()));
            normalIndex = addQuadNorm(normals, normalIndex, 0, 0, 1);
            elementIndex = addQuadIndices(indices, elementIndex, vertexCount);
            vertexCount += 4;

            positionIndex = addQuadPos(positions, positionIndex,
                    maxX, minY, minZ, minX, minY, minZ,
                    minX, maxY, minZ, maxX, maxY, minZ);
            uvIndex = addQuadUV(uv, uvIndex,
                    atlasU(side, box.maxX()), atlasSideV(side, box.minY()),
                    atlasU(side, box.minX()), atlasSideV(side, box.minY()),
                    atlasU(side, box.minX()), atlasSideV(side, box.maxY()),
                    atlasU(side, box.maxX()), atlasSideV(side, box.maxY()));
            normalIndex = addQuadNorm(normals, normalIndex, 0, 0, -1);
            elementIndex = addQuadIndices(indices, elementIndex, vertexCount);
            vertexCount += 4;

            positionIndex = addQuadPos(positions, positionIndex,
                    maxX, minY, maxZ, maxX, minY, minZ,
                    maxX, maxY, minZ, maxX, maxY, maxZ);
            uvIndex = addQuadUV(uv, uvIndex,
                    atlasU(side, 1.0f - box.maxZ()), atlasSideV(side, box.minY()),
                    atlasU(side, 1.0f - box.minZ()), atlasSideV(side, box.minY()),
                    atlasU(side, 1.0f - box.minZ()), atlasSideV(side, box.maxY()),
                    atlasU(side, 1.0f - box.maxZ()), atlasSideV(side, box.maxY()));
            normalIndex = addQuadNorm(normals, normalIndex, 1, 0, 0);
            elementIndex = addQuadIndices(indices, elementIndex, vertexCount);
            vertexCount += 4;

            positionIndex = addQuadPos(positions, positionIndex,
                    minX, minY, minZ, minX, minY, maxZ,
                    minX, maxY, maxZ, minX, maxY, minZ);
            uvIndex = addQuadUV(uv, uvIndex,
                    atlasU(side, box.minZ()), atlasSideV(side, box.minY()),
                    atlasU(side, box.maxZ()), atlasSideV(side, box.minY()),
                    atlasU(side, box.maxZ()), atlasSideV(side, box.maxY()),
                    atlasU(side, box.minZ()), atlasSideV(side, box.maxY()));
            normalIndex = addQuadNorm(normals, normalIndex, -1, 0, 0);
            elementIndex = addQuadIndices(indices, elementIndex, vertexCount);
            vertexCount += 4;
        }
        return new MeshCursor(positionIndex, normalIndex, uvIndex,
                elementIndex, vertexCount);
    }

    private static float atlasU(TextureAtlas.TextureRegion region, float local) {
        return region.uvMin().x + (region.uvMax().x - region.uvMin().x) * local;
    }

    private static float atlasV(TextureAtlas.TextureRegion region, float local) {
        return region.uvMin().y + (region.uvMax().y - region.uvMin().y) * local;
    }

    private static float atlasSideV(TextureAtlas.TextureRegion region, float localY) {
        return region.uvMax().y - (region.uvMax().y - region.uvMin().y) * localY;
    }

    /**
     * Creates raw data from the supplied state and configuration.
     * @param pBuf an array of {@code float} values supplied as {@code pBuf}
     * @param pIdx the {@code int} supplied as {@code pIdx}
     * @param nBuf an array of {@code float} values supplied as {@code nBuf}
     * @param nIdx the {@code int} supplied as {@code nIdx}
     * @param uBuf an array of {@code float} values supplied as {@code uBuf}
     * @param uIdx the {@code int} supplied as {@code uIdx}
     * @param iBuf an array of {@code int} values supplied as {@code iBuf}
     * @param iIdx the {@code int} supplied as {@code iIdx}
     * @return the {@link RawMeshData} representing the build raw data result
     */
    private static RawMeshData buildRawData(float[] pBuf, int pIdx, float[] nBuf, int nIdx, float[] uBuf, int uIdx, int[] iBuf, int iIdx) {
        if (iIdx == 0) return null;
        float[] pos = new float[pIdx]; System.arraycopy(pBuf, 0, pos, 0, pIdx);
        float[] norm = new float[nIdx]; System.arraycopy(nBuf, 0, norm, 0, nIdx);
        float[] uv = new float[uIdx]; System.arraycopy(uBuf, 0, uv, 0, uIdx);
        int[] idx = new int[iIdx]; System.arraycopy(iBuf, 0, idx, 0, iIdx);
        return new RawMeshData(pos, norm, uv, idx);
    }

    /**
     * Creates and returns the mesh.
     * @param data the {@link ChunkMeshData} supplied as {@code data}
     * @return the {@link ChunkRenderMesh} representing the created mesh
     */
    public static ChunkRenderMesh createMesh(ChunkMeshData data) {
        Mesh solid = data.solidData() != null ? new Mesh(data.solidData().positions(), data.solidData().normals(), data.solidData().uv(), data.solidData().indices()) : null;
        Mesh water = data.waterData() != null ? new Mesh(data.waterData().positions(), data.waterData().normals(), data.waterData().uv(), data.waterData().indices()) : null;
        return new ChunkRenderMesh(solid, water);
    }

    /**
     * Returns the block top y.
     * @param data the {@link BlockData} supplied as {@code data}
     * @param y the {@code float} supplied as {@code y}
     * @return {@code float}; the block top y
     */
    private static float getBlockTopY(BlockData data, float y) {
        if (data == BlockData.TILLED_DIRT || data.isFluid()) return y + TILLED_HEIGHT;
        return y + data.getShape().getTop();
    }
    /**
     * Returns the block bottom y.
     * @param world the {@link World} supplied as {@code world}
     * @param worldX the {@code int} supplied as {@code worldX}
     * @param worldY the {@code int} supplied as {@code worldY}
     * @param worldZ the {@code int} supplied as {@code worldZ}
     * @return {@code float}; the block bottom y
     */
    private static float getBlockBottomY(World world, int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.SIZE_Y || !world.isChunkLoadedAt(worldX, worldZ)) return 0.0f;
        byte blockId = world.getBlockTypeAt(worldX, worldY, worldZ);
        if (blockId == 0) return 0.0f;
        BlockData data = BLOCK_LUT[blockId & 0xFF];
        return data == null ? 0.0f : worldY;
    }

    /**
     * Returns the block top y.
     * @param world the {@link World} supplied as {@code world}
     * @param worldX the {@code int} supplied as {@code worldX}
     * @param worldY the {@code int} supplied as {@code worldY}
     * @param worldZ the {@code int} supplied as {@code worldZ}
     * @return {@code float}; the block top y
     */
    private static float getBlockTopY(World world, int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.SIZE_Y || !world.isChunkLoadedAt(worldX, worldZ)) return 0.0f;
        byte blockId = world.getBlockTypeAt(worldX, worldY, worldZ);
        if (blockId == 0) return 0.0f;
        BlockData data = BLOCK_LUT[blockId & 0xFF];
        return data == null ? 0.0f : getBlockTopY(data, worldY);
    }

    /**
     * Determines whether render face is satisfied by the current state.
     * @param world the {@link World} supplied as {@code world}
     * @param worldX the {@code int} supplied as {@code worldX}
     * @param worldY the {@code int} supplied as {@code worldY}
     * @param worldZ the {@code int} supplied as {@code worldZ}
     * @param currentBlock the {@link BlockData} supplied as {@code currentBlock}
     * @return {@code boolean}; the should render face result
     */
    private static boolean shouldRenderFace(World world, int worldX, int worldY, int worldZ, BlockData currentBlock) {
        if (worldY < 0) return false;
        if (worldY >= Chunk.SIZE_Y) return true;
        if (!world.isChunkLoadedAt(worldX, worldZ)) return false;
        if (isBreakingBlock(worldX, worldY, worldZ)) return true;

        byte neighborId = world.getBlockTypeAt(worldX, worldY, worldZ);
        if (neighborId == 0) return true;

        BlockData neighborData = BLOCK_LUT[neighborId & 0xFF];
        if (neighborData == null) return true;

        if (currentBlock.isFluid()) {
            return (!neighborData.isSolid() || neighborData.isFullCube())
                    && neighborData != currentBlock;
        }

        if (neighborData.isFluid()) return true;
        if (neighborData.isFullCube()) return true;
        return neighborData.isTransparent() && neighborData != currentBlock;
    }

    /**
     * Marks the block whose neighbours must render their exposed faces.
     * @param position the position of the breaking block
     */
    public static void setBreakingBlock(BlockPos position) {
        breakingBlock = position;
    }

    /**
     * Clears the temporary breaking-block render state.
     */
    public static void clearBreakingBlock() {
        breakingBlock = null;
    }

    /**
     * Checks whether the supplied position is the temporarily hidden breaking block.
     * @param x the world x coordinate
     * @param y the world y coordinate
     * @param z the world z coordinate
     * @return {@code true} when the position is the breaking block
     */
    private static boolean isBreakingBlock(int x, int y, int z) {
        BlockPos position = breakingBlock;
        return position != null && position.x() == x && position.y() == y && position.z() == z;
    }

    /**
     * Determines whether render water top is satisfied by the current state.
     * @param world the {@link World} supplied as {@code world}
     * @param worldX the {@code int} supplied as {@code worldX}
     * @param y the {@code int} supplied as {@code y}
     * @param worldZ the {@code int} supplied as {@code worldZ}
     * @param currentFluid the {@link BlockData} argument; the fluid being rendered
     * @return {@code boolean}; the should render water top result
     */
    private static boolean shouldRenderWaterTop(World world, int worldX, int y, int worldZ,
                                                BlockData currentFluid) {
        if (y >= Chunk.SIZE_Y - 1) return true;
        byte aboveId = world.getBlockTypeAt(worldX, y + 1, worldZ);
        if (aboveId == 0) return true;
        BlockData above = BLOCK_LUT[aboveId & 0xFF];
        if (above == null) return true;
        if (above == currentFluid) return false;
        return !above.isSolid();
    }

    /**
     * Returns the water corner height.
     * @param world the {@link World} supplied as {@code world}
     * @param wx the {@code int} supplied as {@code wx}
     * @param wy the {@code int} supplied as {@code wy}
     * @param wz the {@code int} supplied as {@code wz}
     * @param currentFluid the {@link BlockData} argument; the fluid being rendered
     * @return {@code float}; the water corner height
     */
    private static float getWaterCornerHeight(World world, int wx, int wy, int wz,
                                              BlockData currentFluid) {
        float totalHeight = 0;
        int count = 0;
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                int nx = wx + dx;
                int nz = wz + dz;
                if (world.isChunkLoadedAt(nx, nz)) {
                    byte blockId = world.getBlockTypeAt(nx, wy, nz);
                    BlockData bData = BLOCK_LUT[blockId & 0xFF];
                    if (bData == currentFluid) {
                        byte lvl = world.getFluidLevelAt(nx, wy, nz);
                        float level = (lvl <= 0) ? 8 : lvl;
                        totalHeight += wy + (level / 8.0f) * TILLED_HEIGHT;
                        count++;
                    } else if (bData != null && bData.isSolid()) {
                        totalHeight += wy + 1.0f;
                        count++;
                    }
                }
            }
        }
        return count > 0 ? (totalHeight / count) : (wy + TILLED_HEIGHT);
    }

    /**
     * Checks whether the partial side exposure condition is met.
     * @param world the {@link World} supplied as {@code world}
     * @param worldX the {@code int} supplied as {@code worldX}
     * @param worldY the {@code int} supplied as {@code worldY}
     * @param worldZ the {@code int} supplied as {@code worldZ}
     * @param currentBlock the {@link BlockData} supplied as {@code currentBlock}
     * @return {@code true} if partial side exposure; otherwise {@code false}
     */
    private static boolean isPartialSideExposure(World world, int worldX, int worldY, int worldZ, BlockData currentBlock) {
        if (currentBlock.isFluid() || !world.isChunkLoadedAt(worldX, worldZ) || worldY < 0 || worldY >= Chunk.SIZE_Y) return false;
        byte neighborId = world.getBlockTypeAt(worldX, worldY, worldZ);
        if (neighborId == 0) return false;
        BlockData neighborData = BLOCK_LUT[neighborId & 0xFF];
        return neighborData == BlockData.TILLED_DIRT && currentBlock != BlockData.TILLED_DIRT;
    }

    /**
     * Returns the side bottom y.
     * @param world the {@link World} supplied as {@code world}
     * @param worldX the {@code int} supplied as {@code worldX}
     * @param worldY the {@code int} supplied as {@code worldY}
     * @param worldZ the {@code int} supplied as {@code worldZ}
     * @param currentBottomY the {@code float} supplied as {@code currentBottomY}
     * @param currentBlock the {@link BlockData} supplied as {@code currentBlock}
     * @return {@code float}; the side bottom y
     */
    private static float getSideBottomY(World world, int worldX, int worldY, int worldZ, float currentBottomY, BlockData currentBlock) {
        if (!world.isChunkLoadedAt(worldX, worldZ) || worldY < 0 || worldY >= Chunk.SIZE_Y) return currentBottomY;
        byte neighborId = world.getBlockTypeAt(worldX, worldY, worldZ);
        if (neighborId == 0) return currentBottomY;
        BlockData neighborData = BLOCK_LUT[neighborId & 0xFF];
        if (neighborData == BlockData.TILLED_DIRT && currentBlock != BlockData.TILLED_DIRT) return currentBottomY + TILLED_HEIGHT;
        return currentBottomY;
    }

    /**
     * Calculates and returns the side uv bottom.
     * @param expBottom the {@code float} supplied as {@code expBottom}
     * @param bottomY the {@code float} supplied as {@code bottomY}
     * @param topY the {@code float} supplied as {@code topY}
     * @return {@code float}; the calculate side uv bottom result
     */
    private static float calculateSideUvBottom(float expBottom, float bottomY, float topY) {
        if (topY <= bottomY) return 0.0f;
        float exposedHeight = topY - expBottom;
        if (exposedHeight <= 0.0f) return 1.0f;
        float totalHeight = topY - bottomY;
        return Math.clamp(1.0f - (exposedHeight / totalHeight), 0.0f, 1.0f);
    }

    /**
     * Adds the quad pos.
     * @param buf an array of {@code float} values supplied as {@code buf}
     * @param idx the {@code int} supplied as {@code idx}
     * @param x1 the {@code float} supplied as {@code x1}
     * @param y1 the {@code float} supplied as {@code y1}
     * @param z1 the {@code float} supplied as {@code z1}
     * @param x2 the {@code float} supplied as {@code x2}
     * @param y2 the {@code float} supplied as {@code y2}
     * @param z2 the {@code float} supplied as {@code z2}
     * @param x3 the {@code float} supplied as {@code x3}
     * @param y3 the {@code float} supplied as {@code y3}
     * @param z3 the {@code float} supplied as {@code z3}
     * @param x4 the {@code float} supplied as {@code x4}
     * @param y4 the {@code float} supplied as {@code y4}
     * @param z4 the {@code float} supplied as {@code z4}
     * @return {@code int}; the addEnemy quad pos result
     */
    private static int addQuadPos(float[] buf, int idx, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
        buf[idx] = x1; buf[idx + 1] = y1; buf[idx + 2] = z1;
        buf[idx + 3] = x2; buf[idx + 4] = y2; buf[idx + 5] = z2;
        buf[idx + 6] = x3; buf[idx + 7] = y3; buf[idx + 8] = z3;
        buf[idx + 9] = x4; buf[idx + 10] = y4; buf[idx + 11] = z4;
        return idx + 12;
    }

    /**
     * Adds the quad uv.
     * @param buf an array of {@code float} values supplied as {@code buf}
     * @param idx the {@code int} supplied as {@code idx}
     * @param u1 the {@code float} supplied as {@code u1}
     * @param v1 the {@code float} supplied as {@code v1}
     * @param u2 the {@code float} supplied as {@code u2}
     * @param v2 the {@code float} supplied as {@code v2}
     * @param u3 the {@code float} supplied as {@code u3}
     * @param v3 the {@code float} supplied as {@code v3}
     * @param u4 the {@code float} supplied as {@code u4}
     * @param v4 the {@code float} supplied as {@code v4}
     * @return {@code int}; the addEnemy quad uv result
     */
    private static int addQuadUV(float[] buf, int idx, float u1, float v1, float u2, float v2, float u3, float v3, float u4, float v4) {
        buf[idx] = u1; buf[idx + 1] = v1;
        buf[idx + 2] = u2; buf[idx + 3] = v2;
        buf[idx + 4] = u3; buf[idx + 5] = v3;
        buf[idx + 6] = u4; buf[idx + 7] = v4;
        return idx + 8;
    }

    /**
     * Adds the quad norm.
     * @param buf an array of {@code float} values supplied as {@code buf}
     * @param idx the {@code int} supplied as {@code idx}
     * @param nx the {@code float} supplied as {@code nx}
     * @param ny the {@code float} supplied as {@code ny}
     * @param nz the {@code float} supplied as {@code nz}
     * @return {@code int}; the addEnemy quad norm result
     */
    private static int addQuadNorm(float[] buf, int idx, float nx, float ny, float nz) {
        for (int i = 0; i < 4; i++) { buf[idx++] = nx; buf[idx++] = ny; buf[idx++] = nz; }
        return idx;
    }

    /**
     * Adds the quad indices.
     * @param buf an array of {@code int} values supplied as {@code buf}
     * @param idx the {@code int} supplied as {@code idx}
     * @param vertexCount the {@code int} supplied as {@code vertexCount}
     * @return {@code int}; the addEnemy quad indices result
     */
    private static int addQuadIndices(int[] buf, int idx, int vertexCount) {
        buf[idx] = vertexCount; buf[idx + 1] = vertexCount + 1; buf[idx + 2] = vertexCount + 2;
        buf[idx + 3] = vertexCount + 2; buf[idx + 4] = vertexCount + 3; buf[idx + 5] = vertexCount;
        return idx + 6;
    }

    /**
     * Adds the side quad direct.
     * @param pos an array of {@code float} values supplied as {@code pos}
     * @param norm an array of {@code float} values supplied as {@code norm}
     * @param uv an array of {@code float} values supplied as {@code uv}
     * @param idx an array of {@code int} values supplied as {@code idx}
     * @param posI the {@code int} supplied as {@code posI}
     * @param normI the {@code int} supplied as {@code normI}
     * @param uvI the {@code int} supplied as {@code uvI}
     * @param elemI the {@code int} supplied as {@code elemI}
     * @param vertexCount the {@code int} supplied as {@code vertexCount}
     * @param x1 the {@code float} supplied as {@code x1}
     * @param x2 the {@code float} supplied as {@code x2}
     * @param y1 the {@code float} supplied as {@code y1}
     * @param topY1 the {@code float} argument; the top y at the first endpoint
     * @param topY2 the {@code float} argument; the top y at the second endpoint
     * @param z1 the {@code float} supplied as {@code z1}
     * @param z2 the {@code float} supplied as {@code z2}
     * @param nx the {@code float} supplied as {@code nx}
     * @param ny the {@code float} supplied as {@code ny}
     * @param nz the {@code float} supplied as {@code nz}
     * @param data the {@link BlockData} supplied as {@code data}
     * @param uvB the {@code float} supplied as {@code uvB}
     * @param uvT the {@code float} supplied as {@code uvT}
     * @return {@code int}; the addEnemy side quad direct result
     */
    private static int addSideQuadDirect(float[] pos, float[] norm, float[] uv, int[] idx,
                                         int posI, int normI, int uvI, int elemI, int vertexCount, float x1,
                                         float x2, float y1, float topY1, float topY2, float z1, float z2, float nx, float ny,
                                         float nz, BlockData data, float uvB, float uvT) {
        TextureAtlas.TextureRegion region = data.getSideRegion();
        if (region == null) return vertexCount;
        addQuadPos(pos, posI, x1, y1, z1, x2, y1, z2,
                x2, topY2, z2, x1, topY1, z1);
        float u1 = region.uvMin().x; float u2 = region.uvMax().x;
        float heightUV = region.uvMax().y - region.uvMin().y;
        float v1 = region.uvMin().y + heightUV * (1.0f - uvB);
        float v2 = region.uvMin().y + heightUV * (1.0f - uvT);
        addQuadUV(uv, uvI, u1, v1, u2, v1, u2, v2, u1, v2);
        addQuadNorm(norm, normI, nx, ny, nz);
        addQuadIndices(idx, elemI, vertexCount);
        return vertexCount + 4;
    }
}
