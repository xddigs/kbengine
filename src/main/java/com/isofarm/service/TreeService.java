package com.isofarm.service;

import com.isofarm.data.*;
import com.isofarm.entity.WorldItem;
import com.isofarm.item.*;
import com.isofarm.ui.GameUIService;
import com.isofarm.utils.HoveredCell;
import com.isofarm.utils.Settings;
import com.isofarm.wrld.Chunk;
import com.isofarm.wrld.GameMaster;
import com.isofarm.wrld.World;
import com.isofarm.wrld.WorldGenerator;
import org.joml.Vector3f;

import java.util.*;

/**
 * Encapsulates the state and operations required by tree service within the game runtime.
 */
@Singleton
public class TreeService {
    public static final TreeService ts = new TreeService();
    private static final int LEAF_DECAY_CHECK_DISTANCE = 4;
    private static final int RANDOM_TICKS_PER_CHUNK = 3;
    private static final int SAPLING_GROWTH_TICKS = 600;
    private final List<TreeSapling> saplings = new ArrayList<>();
    private final Random random = new Random();

    /**
     * Creates a new {@code TreeService} instance.
     */
    private TreeService() {}

    /**
     * Applies the world or inventory action represented by chop.
     * @param gamemaster the {@link GameMaster} supplied as {@code gamemaster}
     * @param axe the {@link Axe} supplied as {@code axe}
     * @return the {@link List} representing the chop result
     */
    public static List<BlockPos> chop(GameMaster gamemaster, Axe axe) {
        List<BlockPos> choppedBlocks = new ArrayList<>();
        BlockPos cell = HoveredCell.get(gamemaster);
        if (cell == null) return choppedBlocks;

        World world = gamemaster.getWorld();
        byte startId = world.getBlockTypeAt(cell.x(), cell.y(), cell.z());
        BlockData startBlock = BlockData.fromId(startId);

        if (!BlockData.OAK_LOG.equals(startBlock)) {
            return choppedBlocks;
        }

        Queue<BlockPos> toProcess = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<ChunkPos> affectedChunks = new HashSet<>();

        BlockPos origin = new BlockPos(startBlock, cell.x(), cell.y(), cell.z());
        toProcess.add(origin);
        visited.add(origin);

        int blocksBroken = 0;
        int maxBlocks = 150;

        while (!toProcess.isEmpty() && blocksBroken < maxBlocks) {
            BlockPos current = toProcess.poll();
            byte currentId = World.wrld.getBlockTypeAt(current.x(), current.y(), current.z());
            BlockData currentBlock = BlockData.fromId(currentId);
            boolean isLog = BlockData.OAK_LOG.equals(currentBlock);

            if (isLog) {
                World.wrld.setBlockTypeAt(current.x(), current.y(), current.z(), BlockData.AIR.getId());
                choppedBlocks.add(current);
                blocksBroken++;
                axe.use();
                affectedChunks.add(new ChunkPos(current.x(), current.z()));
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;

                            BlockPos neighbor = new BlockPos(currentBlock,
                                    current.x() + dx,
                                    current.y() + dy,
                                    current.z() + dz);

                            if (!visited.contains(neighbor)) {
                                visited.add(neighbor);
                                toProcess.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        if (blocksBroken > 0) {
            GameUIService.ui.logAction(cell);
            SoundService.fx.playBreakSound(startBlock.getSoundGroup()
            );

            for (ChunkPos chunk : affectedChunks) {
                gamemaster.rebuildChunkMeshAt(chunk.x(), chunk.z());
            }
            return choppedBlocks;
        }
        return choppedBlocks;
    }

    /**
     * Applies the world or inventory action represented by plant.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @param saplingBlock the {@link BlockData} supplied as {@code saplingBlock}
     */
    public void plant(int x, int y, int z, BlockData saplingBlock) {
        World.wrld.setBlockTypeAt(x, y, z, saplingBlock.getId());
        // TreeSapling counts from zero, so the target must be relative to the
        // current world tick rather than the absolute tick counter.
        saplings.add(new TreeSapling(x, y, z, saplingBlock,
                (int) Settings.getTicks() + SAPLING_GROWTH_TICKS));
    }

    /**
     * Updates the current state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     */
    public void update(GameMaster gameMaster) {
        for (int i = saplings.size() - 1; i >= 0; i--) {
            TreeSapling sapling = saplings.get(i);
            if (sapling.tick()) {
                growTree(sapling);
                saplings.remove(i);
            }
        }

        saplings.removeIf(t -> World.wrld.getBlockTypeAt(
                t.getX(), t.getY(), t.getZ()) == BlockData.AIR.getId());
        updateLeaves(gameMaster);
    }

    /**
     * Applies the world or inventory action represented by grow tree.
     * @param sapling the {@link TreeSapling} supplied as {@code sapling}
     */
    @SuppressWarnings("DuplicateBranchesInSwitch")
    private void growTree(TreeSapling sapling) {
        int x = sapling.getX();
        int y = sapling.getY();
        int z = sapling.getZ();

        World.wrld.setBlockTypeAt(x, y, z, BlockData.AIR.getId());
        BlockData logType = switch (sapling.getTreeType()) {
            case SPRUCE_BONSAI -> BlockData.SPRUCE_LOG;
            case OAK_BONSAI -> BlockData.OAK_LOG;
            default -> BlockData.OAK_LOG;
        };
        WorldGenerator.generateTree(x, z, random, logType);
        GameMaster.game.rebuildChunkMeshAt(x, z);
    }

    /**
     * Updates the leaves.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     */
    private void updateLeaves(GameMaster gameMaster) {
        for (Chunk chunk : World.wrld.getChunks().values()) {
            int chunkStartX = chunk.getChunkX() * Chunk.SIZE_X;
            int chunkStartZ = chunk.getChunkZ() * Chunk.SIZE_Z;

            for (int i = 0; i < RANDOM_TICKS_PER_CHUNK; i++) {
                int localX = random.nextInt(Chunk.SIZE_X);
                int localY = random.nextInt(Chunk.SIZE_Y);
                int localZ = random.nextInt(Chunk.SIZE_Z);

                byte blockId = chunk.getBlock(localX, localY, localZ);

                if (blockId == BlockData.OAK_LEAVES.getId()) {
                    int worldX = chunkStartX + localX;
                    int worldZ = chunkStartZ + localZ;

                    if (!isConnectedToLog(worldX, localY, worldZ)) {
                        World.wrld.setBlockTypeAt(worldX, localY, worldZ, BlockData.AIR.getId());
                        Item item = createLeafDrop(BlockData.OAK_LEAVES.getRandomDrop());
                        if (item != null) {
                            WorldItem worldItem = new WorldItem(item, 1,
                                    new Vector3f(worldX + 0.5f, localY + 0.5f,
                                            worldZ + 0.5f));
                            gameMaster.addEntity(worldItem);
                        }
                        gameMaster.rebuildChunkMeshAt(worldX, worldZ);
                    }
                }
            }
        }
    }

    /**
     * Converts a configured leaf drop into a concrete inventory item.
     */
    private Item createLeafDrop(Object drop) {
        return switch (drop) {
            case MaterialID materialID -> new Material(Tier.NONE, materialID);
            case BlockData blockData -> new Block(blockData);
            case Item item -> item;
            case null, default -> null;
        };
    }

    /**
     * Checks whether the connected to log condition is met.
     * @param startX the {@code int} supplied as {@code startX}
     * @param startY the {@code int} supplied as {@code startY}
     * @param startZ the {@code int} supplied as {@code startZ}
     * @return {@code true} if connected to log; otherwise {@code false}
     */
    private boolean isConnectedToLog(int startX, int startY, int startZ) {
        Queue<BlockNode> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();

        queue.add(new BlockNode(startX, startY, startZ, 0));
        visited.add(World.wrld.getBlockKey(startX, startY, startZ));

        while (!queue.isEmpty()) {
            BlockNode current = queue.poll();
            if (current.distance() > LEAF_DECAY_CHECK_DISTANCE) {
                continue;
            }

            byte currentId = World.wrld.getBlockTypeAt(current.x(), current.y(), current.z());
            if (currentId == BlockData.OAK_LOG.getId()) {
                return true;
            }

            if (currentId == BlockData.OAK_LEAVES.getId() || current.distance() == 0) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;

                            int nx = current.x() + dx;
                            int ny = current.y() + dy;
                            int nz = current.z() + dz;

                            long key = World.wrld.getBlockKey(nx, ny, nz);
                            if (!visited.contains(key)) {
                                visited.add(key);
                                queue.add(new BlockNode(nx, ny, nz, current.distance() + 1));
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
