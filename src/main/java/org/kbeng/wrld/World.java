package org.kbeng.wrld;

import org.kbeng.data.BlockData;
import org.kbeng.data.BlockPos;
import org.kbeng.data.BlockShape;
import org.kbeng.data.Crop;
import org.kbeng.data.Singleton;
import org.kbeng.item.Block;
import org.kbeng.item.iBlock;
import org.kbeng.pathfinding.GridPos;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

/**
 * World provides world capabilities within the wrld subsystem.
 * It maintains world simulation concerns including terrain, chunks, fluids, and authoritative spatial state.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@SuppressWarnings("all")
@Singleton
public class World {
    public static final World wrld = new World();
    private final Map<Long, Block> blocks = new HashMap<>();
    private final Map<Long, iBlock> interactiveBlocks = new HashMap<>();
    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final Map<Long, BlockPos> torches = new HashMap<>();
    private final Map<Long, BlockPos> lavaCells = new HashMap<>();

    /**
     * Creates a new private {@code World} instance.
     */
    private World() {}

    /**
     * Returns get2 dkey.
     * @param x the {@code int} supplied as {@code x}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code long}; the get2 dkey result
     */
    public long get2DKey(int x, int z) {
        return (((long) x) << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * Returns the or create chunk.
     * @param chunkX the {@code int} supplied as {@code chunkX}
     * @param chunkZ the {@code int} supplied as {@code chunkZ}
     * @return the {@link Chunk} representing the or create chunk
     */
    public Chunk getOrCreateChunk(int chunkX, int chunkZ) {
        long key = get2DKey(chunkX, chunkZ);

        return chunks.computeIfAbsent(key, k -> new Chunk(chunkX, chunkZ));
    }

    /**
     * Checks whether the chunk loaded at condition is met.
     * @param x the {@code int} supplied as {@code x}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code true} if chunk loaded at; otherwise {@code false}
     */
    public boolean isChunkLoadedAt(int x, int z) {
        int chunkX = Math.floorDiv(x, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(z, Chunk.SIZE_Z);
        return chunks.containsKey(get2DKey(chunkX, chunkZ));
    }

    /**
     * Returns the chunks.
     * @return the {@link Map} representing the chunks
     */
    public Map<Long, Chunk> getChunks() {
        return chunks;
    }

    /**
     * Returns the block at.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return the {@link Block} representing the block at
     */
    public Block getBlockAt(int x, int y, int z) {
        long key = getBlockKey(x, y, z);
        Block registeredBlock = blocks.get(key);

        if (registeredBlock != null) {
            return registeredBlock;
        }

        byte blockId = getBlockTypeAt(x, y, z);
        if (blockId == 0) {
            return null;
        }

        BlockData data = BlockData.fromId(blockId);
        if (data == null) {
            return null;
        }

        return new Block(data, x, y, z);
    }

    /**
     * Returns the block at.
     * @param pos the {@link BlockPos} supplied as {@code pos}
     * @return the {@link Block} representing the block at
     */
    public Block getBlockAt(BlockPos pos) {
        return getBlockAt(pos.x(), pos.y(), pos.z());
    }

    /** Returns the placed shape state, falling back to the block type default. */
    public BlockShape getBlockShapeAt(int x, int y, int z) {
        BlockData data = BlockData.fromId(getBlockTypeAt(x, y, z));
        if (data != null && data.isFence()) {
            int connections = 0;
            if (canFenceConnectTo(x, y, z - 1)) connections |= 1;
            if (canFenceConnectTo(x, y, z + 1)) connections |= 2;
            if (canFenceConnectTo(x - 1, y, z)) connections |= 4;
            if (canFenceConnectTo(x + 1, y, z)) connections |= 8;
            return BlockShape.fence(connections);
        }
        Block registeredBlock = blocks.get(getBlockKey(x, y, z));
        if (registeredBlock != null) {
            BlockShape shape = registeredBlock.getShape();
            if (data.isStaircase()) {
                int connections = 0;
                if (isStaircaseAt(x, y, z - 1)) connections |= 1;
                if (isStaircaseAt(x, y, z + 1)) connections |= 2;
                if (isStaircaseAt(x - 1, y, z)) connections |= 4;
                if (isStaircaseAt(x + 1, y, z)) connections |= 8;
                return BlockShape.staircaseConnected(shape, connections);
            }
            return shape;
        }
        return data == null ? BlockShape.FULL_CUBE : data.getShape();
    }

    private boolean isStaircaseAt(int x, int y, int z) {
        BlockData neighbor = BlockData.fromId(getBlockTypeAt(x, y, z));
        return neighbor != null && neighbor.isStaircase();
    }

    private boolean canFenceConnectTo(int x, int y, int z) {
        if (getInteractiveBlockAt(x, y, z) != null) return true;
        BlockData neighbor = BlockData.fromId(getBlockTypeAt(x, y, z));
        return neighbor != null && neighbor.isSolid();
    }

    /**
     * Adds the block.
     * @param block the {@link Block} supplied as {@code block}
     */
    public void addBlock(Block block) {
        if (block == null) return;
        blocks.put(getBlockKey(block.getX(), block.getY(), block.getZ()), block);
    }

    /**
     * Removes the block.
     * @param block the {@link Block} supplied as {@code block}
     */
    public void removeBlock(Block block) {
        if (block == null) return;
        long key = getBlockKey(block.getX(), block.getY(), block.getZ());
        if (blocks.get(key) == block) {
            blocks.remove(key);
        }
    }

    /**
     * Adds an interactive block to the world.
     * @param block the {@link iBlock} argument; the interactive block
     */
    public void addInteractiveBlock(iBlock block) {
        if (block == null) return;
        for (int offsetY = 0; offsetY < block.getType().getHeight(); offsetY++) {
            interactiveBlocks.put(getBlockKey(
                    block.getX(), block.getY() + offsetY, block.getZ()), block);
        }
    }

    /**
     * Returns the interactive block at a world position.
     * @param x the {@code int} argument; the x coordinate
     * @param y the {@code int} argument; the y coordinate
     * @param z the {@code int} argument; the z coordinate
     * @return the {@link iBlock} representing the interactive block, or {@code null} when the position is empty
     */
    public iBlock getInteractiveBlockAt(int x, int y, int z) {
        return interactiveBlocks.get(getBlockKey(x, y, z));
    }

    /**
     * Removes and returns the interactive block at a world position.
     * @param x the {@code int} argument; the x coordinate
     * @param y the {@code int} argument; the y coordinate
     * @param z the {@code int} argument; the z coordinate
     * @return the {@link iBlock} representing the removed block, or {@code null} when none was present
     */
    public iBlock removeInteractiveBlockAt(int x, int y, int z) {
        iBlock block = getInteractiveBlockAt(x, y, z);
        if (block == null) return null;

        for (int offsetY = 0; offsetY < block.getType().getHeight(); offsetY++) {
            long key = getBlockKey(block.getX(), block.getY() + offsetY, block.getZ());
            if (interactiveBlocks.get(key) == block) {
                interactiveBlocks.remove(key);
            }
        }
        return block;
    }

    /**
     * Visits every placed interactive block.
     * @param consumer the {@link Consumer} argument; the block consumer
     */
    public void forEachInteractiveBlock(Consumer<iBlock> consumer) {
        new HashSet<>(interactiveBlocks.values()).forEach(consumer);
    }

    /**
     * Tests an AABB against every door model using its current animated angle.
     */
    public boolean intersectsDoor(float minX, float minY, float minZ,
                                  float maxX, float maxY, float maxZ) {
        for (iBlock block : new HashSet<>(interactiveBlocks.values())) {
            if (block.intersects(minX, minY, minZ, maxX, maxY, maxZ)) return true;
        }
        return false;
    }

    /**
     * Finds the closest animated door model intersected by a world-space ray.
     */
    public DoorHit raycastDoor(Vector3f origin, Vector3f direction) {
        iBlock closest = null;
        float closestDistance = Float.POSITIVE_INFINITY;
        for (iBlock block : new HashSet<>(interactiveBlocks.values())) {
            float distance = block.rayIntersection(origin, direction);
            if (distance < closestDistance) {
                closest = block;
                closestDistance = distance;
            }
        }
        return closest == null ? null : new DoorHit(closest, closestDistance);
    }

    /** Closest precise door intersection along a ray. */
    public record DoorHit(iBlock block, float distance) { }

    /**
     * Adds the crop.
     * @param crop the {@link Crop} supplied as {@code crop}
     */
    public void addCrop(Crop crop) {
        addBlock(crop);
    }

    /**
     * Removes the crop.
     * @param crop the {@link Crop} supplied as {@code crop}
     */
    public void removeCrop(Crop crop) {
        removeBlock(crop);
    }

    /**
     * Returns the crop at.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return the {@link Crop} representing the crop at
     */
    public Crop getCropAt(int x, int y, int z) {
        Block block = blocks.get(getBlockKey(x, y, z));
        if (block instanceof Crop crop) {
            return crop;
        }
        return null;
    }

    /**
     * Returns the blocks.
     * @return the {@link Map} representing the blocks
     */
    public Map<Long, Block> getBlocks() {
        return blocks;
    }

    /**
     * Processes each applicable element for for each.
     * @param consumer the {@link Consumer} supplied as {@code consumer}
     */
    public void forEach(Consumer<Block> consumer) {
        for (Block block : blocks.values()) {
            consumer.accept(block);
        }
    }

    /** Visits every placed torch without scanning all loaded voxel cells. */
    public void forEachTorch(Consumer<BlockPos> consumer) {
        torches.values().forEach(consumer);
    }

    /** Visits every lava cell that can act as an artificial light source. */
    public void forEachLava(Consumer<BlockPos> consumer) {
        lavaCells.values().forEach(consumer);
    }

    /**
     * Returns the chunk block type at.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code int}; the chunk block type at
     */
    public int getChunkBlockTypeAt(int x, int y, int z) {
        int chunkX = Math.floorDiv(x, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(z, Chunk.SIZE_Z);

        Chunk chunk = getOrCreateChunk(chunkX, chunkZ);

        int localX = Math.floorMod(x, Chunk.SIZE_X);
        int localZ = Math.floorMod(z, Chunk.SIZE_Z);

        return chunk.getBlock(localX, y, localZ);
    }

    /**
     * Returns the block type at.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code byte}; the block type at
     */
    public byte getBlockTypeAt(int x, int y, int z) {
        if (y < 0 || y >= Chunk.SIZE_Y) return 0;
        int chunkX = Math.floorDiv(x, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(z, Chunk.SIZE_Z);

        Chunk chunk = chunks.get(get2DKey(chunkX, chunkZ));
        if (chunk == null) return 0;
        int localX = Math.floorMod(x, Chunk.SIZE_X);
        int localZ = Math.floorMod(z, Chunk.SIZE_Z);

        return chunk.getBlock(localX, y, localZ);
    }

    /**
     * Returns the block type at.
     * @param pos the {@link BlockPos} supplied as {@code pos}
     * @return {@code byte}; the block type at
     */
    public byte getBlockTypeAt(BlockPos pos) {
        return getBlockTypeAt(pos.x(), pos.y(), pos.z());
    }

    /**
     * Removes the block at.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     */
    public void removeBlockAt(int x, int y, int z) {
        blocks.remove(getBlockKey(x, y, z));
    }

    /**
     * Sets the block type at.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @param blockId the {@code byte} supplied as {@code blockId}
     */
    public void setBlockTypeAt(int x, int y, int z, byte blockId) {
        if (y < 0 || y >= Chunk.SIZE_Y) {
            return;
        }

        int chunkX = Math.floorDiv(x, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(z, Chunk.SIZE_Z);
        Chunk chunk = getOrCreateChunk(chunkX, chunkZ);
        int localX = Math.floorMod(x, Chunk.SIZE_X);
        int localZ = Math.floorMod(z, Chunk.SIZE_Z);

        chunk.setBlock(localX, y, localZ, blockId);

        long blockKey = getBlockKey(x, y, z);
        if (blockId == BlockData.TORCH.getId()) {
            torches.put(blockKey, new BlockPos(BlockData.TORCH, x, y, z));
        } else {
            torches.remove(blockKey);
        }

        if (blockId == BlockData.LAVA.getId()) {
            lavaCells.put(blockKey, new BlockPos(BlockData.LAVA, x, y, z));
        } else {
            lavaCells.remove(blockKey);
        }

        if (blockId == BlockData.AIR.getId()) {
            removeBlockAt(x, y, z);
        }
    }

    /**
     * Sets the block type at.
     * @param pos the {@link BlockPos} supplied as {@code pos}
     * @param blockId the {@code byte} supplied as {@code blockId}
     */
    public void setBlockTypeAt(BlockPos pos, byte blockId) {
        setBlockTypeAt(pos.x(), pos.y(), pos.z(), blockId);
    }

    /**
     * Returns the water level at.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code byte}; the water level at
     */
    public byte getWaterLevelAt(int x, int y, int z) {
        if (y < 0 || y >= Chunk.SIZE_Y) {
            return 0;
        }

        int chunkX = Math.floorDiv(x, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(z, Chunk.SIZE_Z);

        Chunk chunk = chunks.get(get2DKey(chunkX, chunkZ));

        if (chunk == null) {
            return 0;
        }

        int localX = Math.floorMod(x, Chunk.SIZE_X);
        int localZ = Math.floorMod(z, Chunk.SIZE_Z);

        return chunk.getWaterLevel(localX, y, localZ);
    }

    /**
     * Returns the water level at.
     * @param pos the {@link BlockPos} supplied as {@code pos}
     * @return {@code byte}; the water level at
     */
    public byte getWaterLevelAt(BlockPos pos) {
        return getWaterLevelAt(pos.x(), pos.y(), pos.z());
    }

    /**
     * Returns the fluid level at a world position.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code byte}; the stored fluid level
     */
    public byte getFluidLevelAt(int x, int y, int z) {
        return getWaterLevelAt(x, y, z);
    }

    /**
     * Returns the fluid level at a world position.
     * @param pos the {@link BlockPos} argument; the position value
     * @return {@code byte}; the stored fluid level
     */
    public byte getFluidLevelAt(BlockPos pos) {
        return getFluidLevelAt(pos.x(), pos.y(), pos.z());
    }

    /** Returns whether a position contains ocean water generated with the terrain. */
    public boolean isGeneratedOceanWaterAt(int x, int y, int z) {
        if (y < 0 || y >= Chunk.SIZE_Y) return false;
        int chunkX = Math.floorDiv(x, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(z, Chunk.SIZE_Z);
        Chunk chunk = chunks.get(get2DKey(chunkX, chunkZ));
        return chunk != null && chunk.isGeneratedOceanWater(
                Math.floorMod(x, Chunk.SIZE_X), y, Math.floorMod(z, Chunk.SIZE_Z));
    }

    /**
     * Sets the water level at.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @param waterLevel the {@code byte} supplied as {@code waterLevel}
     */
    public void setWaterLevelAt(int x, int y, int z, byte waterLevel) {
        if (y < 0 || y >= Chunk.SIZE_Y) {
            return;
        }

        int chunkX = Math.floorDiv(x, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(z, Chunk.SIZE_Z);

        Chunk chunk = getOrCreateChunk(chunkX, chunkZ);

        int localX = Math.floorMod(x, Chunk.SIZE_X);
        int localZ = Math.floorMod(z, Chunk.SIZE_Z);

        chunk.setWaterLevel(localX, y, localZ, waterLevel);
    }

    /**
     * Sets the water level at.
     * @param pos the {@link BlockPos} supplied as {@code pos}
     * @param waterLevel the {@code byte} supplied as {@code waterLevel}
     */
    public void setWaterLevelAt(BlockPos pos, byte waterLevel) {
        setWaterLevelAt(pos.x(), pos.y(), pos.z(), waterLevel);
    }

    /**
     * Sets the fluid level at a world position.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @param fluidLevel the {@code byte} supplied as {@code fluidLevel}
     */
    public void setFluidLevelAt(int x, int y, int z, byte fluidLevel) {
        setWaterLevelAt(x, y, z, fluidLevel);
    }

    /**
     * Sets the fluid level at a world position.
     * @param pos the {@link BlockPos} argument; the position value
     * @param fluidLevel the {@code byte} supplied as {@code fluidLevel}
     */
    public void setFluidLevelAt(BlockPos pos, byte fluidLevel) {
        setFluidLevelAt(pos.x(), pos.y(), pos.z(), fluidLevel);
    }

    /**
     * Returns the block key.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code long}; the block key
     */
    public long getBlockKey(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) |
                (((long) z & 0x3FFFFFFL) << 12) |
                ((long) y & 0xFFFL);
    }

    /**
     * Returns the block data.
     * @param blockId the {@code byte} supplied as {@code blockId}
     * @return the {@link BlockData} representing the block data
     */
    private BlockData getBlockData(byte blockId) {
        for (BlockData data : BlockData.values()) {
            if (data.getId() == blockId) {
                return data;
            }
        }

        return null;
    }

    /**
     * Checks whether the block solid condition is met.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code true} if block solid; otherwise {@code false}
     */
    public boolean isBlockSolid(int x, int y, int z) {
        iBlock interactiveBlock = getInteractiveBlockAt(x, y, z);
        if (interactiveBlock != null) return interactiveBlock.isSolid();
        byte blockId = getBlockTypeAt(x, y, z);
        BlockData block = BlockData.fromId(blockId);
        if (block == null) return false;
        if (block.isFluid()) return false;
        return block.isSolid();
    }

    /**
     * Returns whether a cell contains a solid full-cube collider. Doors are
     * excluded because their model-shaped collision is evaluated separately.
     */
    public boolean isFullCubeSolid(int x, int y, int z) {
        iBlock interactiveBlock = getInteractiveBlockAt(x, y, z);
        if (interactiveBlock != null) {
            return !interactiveBlock.getType().isDoor();
        }
        BlockData block = BlockData.fromId(getBlockTypeAt(x, y, z));
        return block != null && block.isSolid() && getBlockShapeAt(x, y, z).isFullCube();
    }

    /** Tests an AABB against non-full voxel block shapes. */
    public boolean intersectsShapedBlocks(float minX, float minY, float minZ,
                                          float maxX, float maxY, float maxZ) {
        int blockMinX = (int) Math.floor(minX);
        int blockMaxX = (int) Math.floor(maxX);
        int blockMinY = Math.max(0, (int) Math.floor(minY));
        int blockMaxY = Math.min(Chunk.SIZE_Y - 1, (int) Math.floor(maxY));
        int blockMinZ = (int) Math.floor(minZ);
        int blockMaxZ = (int) Math.floor(maxZ);

        for (int x = blockMinX; x <= blockMaxX; x++) {
            for (int y = blockMinY; y <= blockMaxY; y++) {
                for (int z = blockMinZ; z <= blockMaxZ; z++) {
                    BlockData block = BlockData.fromId(getBlockTypeAt(x, y, z));
                    BlockShape shape = getBlockShapeAt(x, y, z);
                    if (block == null || !block.isSolid() || shape.isFullCube()) continue;
                    if (shape.intersects(x, y, z,
                            minX, minY, minZ, maxX, maxY, maxZ)) return true;
                }
            }
        }
        return false;
    }

    /** Returns the world-space collision surface under a point in one cell. */
    public float getBlockSurfaceY(int x, int y, int z, float worldX, float worldZ) {
        iBlock interactiveBlock = getInteractiveBlockAt(x, y, z);
        if (interactiveBlock != null && interactiveBlock.isSolid()) return y + 1.0f;
        BlockData block = BlockData.fromId(getBlockTypeAt(x, y, z));
        if (block == null || !block.isSolid()) return Float.NEGATIVE_INFINITY;
        float localTop = getBlockShapeAt(x, y, z).getTopAt(worldX - x, worldZ - z);
        return localTop > 0.0f ? y + localTop : Float.NEGATIVE_INFINITY;
    }

    /**
     * Returns the highest y.
     * @param spawnX the {@code float} supplied as {@code spawnX}
     * @param spawnZ the {@code float} supplied as {@code spawnZ}
     * @return the {@link GridPos} representing the highest y
     */
    public GridPos getHighestY(float spawnX, float spawnZ) {
        int blockX = (int) Math.floor(spawnX);
        int blockZ = (int) Math.floor(spawnZ);

        for (int y = Chunk.SIZE_Y - 1; y >= 0; y--) {
            byte blockId = getBlockTypeAt(blockX, y, blockZ);
            if (blockId != 0) {
                return new GridPos(blockX, y, blockZ);
            }
        }
        return new GridPos(blockX, 0, blockZ);
    }

    /**
     * Updates or derives runtime state for for each plant according to the supplied arguments.
     * @param consumer the {@link Consumer} supplied as {@code consumer}
     */
    public void forEachPlant(Consumer<PlantInstance> consumer) {
        for (Chunk chunk : chunks.values()) {
            int[] plantIndices = chunk.getPlantIndices();

            for (int index : plantIndices) {
                int localX = Chunk.indexToX(index);
                int localY = Chunk.indexToY(index);
                int localZ = Chunk.indexToZ(index);
                int worldX = chunk.getChunkX() * Chunk.SIZE_X + localX;
                int worldZ = chunk.getChunkZ() * Chunk.SIZE_Z + localZ;

                byte blockId = chunk.getBlock(localX, localY, localZ);
                BlockData data = BlockData.fromId(blockId);
                if (data == null || !data.isPlant()) {
                    continue;
                }

                consumer.accept(new PlantInstance(chunk, worldX, localY, worldZ, data));
            }
        }
    }

    /**
     * Immutable value object containing plant instance.
     */
    public record PlantInstance(Chunk chunk, int x, int y, int z, BlockData data) {}
}
