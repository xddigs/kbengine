package org.kbeng.wrld;

import org.kbeng.data.BlockData;
import org.kbeng.data.BlockPos;
import org.kbeng.data.Crop;
import org.kbeng.data.FluidPos;
import org.kbeng.graphics.ParticleEngine;
import org.kbeng.graphics.ResourceManager;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines the shared source, flow and chunk-rebuild behavior used by fluid
 * simulations. Concrete simulations provide their block type, update speed and
 * source-renewal policy.
 */

@SuppressWarnings("all")
public abstract class FluidSimulation {
    protected static final byte MAX_LEVEL = 8;
    protected static final byte MIN_LEVEL = 1;
    private static final int MAX_UPDATES_PER_FRAME = 2048;

    private final BlockData fluidType;
    private final float stepTime;
    private final boolean renewableSources;
    private final Queue<FluidPos> queue = new ArrayDeque<>();
    private final Set<FluidPos> queued = new HashSet<>();
    private final Set<Long> changedChunks = new HashSet<>();
    private final Set<FluidPos> sources = new HashSet<>();
    private final Map<FluidPos, FluidSlope> slopes = new ConcurrentHashMap<>();
    private float timer;

    /**
     * Direction in which a fluid surface descends across one block.
     */
    public record FluidSlope(int dx, int dz) {
        public FluidSlope {
            if (Math.abs(dx) + Math.abs(dz) != 1) {
                throw new IllegalArgumentException("A fluid slope must point to one horizontal neighbour");
            }
        }
    }

    /**
     * Creates a new {@code FluidSimulation} instance.
     * @param fluidType the {@link BlockData} argument; the fluid block type
     * @param stepTime the {@code float} argument; the number of seconds between simulation steps
     * @param renewableSources the {@code boolean} argument; whether adjacent sources may create new sources
     */
    protected FluidSimulation(BlockData fluidType, float stepTime, boolean renewableSources) {
        if (fluidType == null || !fluidType.isFluid()) {
            throw new IllegalArgumentException("Fluid simulations require a fluid block type");
        }
        this.fluidType = fluidType;
        this.stepTime = stepTime;
        this.renewableSources = renewableSources;
    }

    /**
     * Returns the simulation responsible for a block type.
     * @param blockType the {@link BlockData} supplied as {@code blockType}
     * @return the {@link FluidSimulation} representing the matching simulation, or {@code null} for a non-fluid block
     */
    public static FluidSimulation forBlock(BlockData blockType) {
        if (blockType == BlockData.WATER) return WaterSimulation.ws;
        if (blockType == BlockData.LAVA) return LavaSimulation.ls;
        return null;
    }

    /**
     * Returns the downhill direction recorded for a fluid cell.
     * @param blockType the {@link BlockData} argument; the fluid type
     * @param x the {@code int} argument; the world x value
     * @param y the {@code int} argument; the world y value
     * @param z the {@code int} argument; the world z value
     * @return the {@link FluidSlope} result; its downhill direction, or {@code null} for a flat fluid cell
     */
    public static FluidSlope getSlope(BlockData blockType, int x, int y, int z) {
        FluidSimulation simulation = forBlock(blockType);
        if (simulation == null) return null;
        return simulation.slopes.get(new FluidPos(x, y, z));
    }

    /**
     * Updates every registered fluid simulation.
     * @param delta the {@code float} argument; the elapsed time in seconds
     */
    public static void updateAll(float delta) {
        WaterSimulation.ws.update(delta);
        LavaSimulation.ls.update(delta);
    }

    /**
     * Notifies every fluid simulation that a block was destroyed.
     * @param x the {@code int} argument; the block x value
     * @param y the {@code int} argument; the block y value
     * @param z the {@code int} argument; the block z value
     */
    public static void notifyBlockDestroyed(int x, int y, int z) {
        WaterSimulation.ws.onBlockDestroyed(x, y, z);
        LavaSimulation.ls.onBlockDestroyed(x, y, z);
    }

    /**
     * Notifies every fluid simulation that a block was placed.
     * @param x the {@code int} argument; the block x value
     * @param y the {@code int} argument; the block y value
     * @param z the {@code int} argument; the block z value
     */
    public static void notifyBlockPlaced(int x, int y, int z) {
        WaterSimulation.ws.onBlockPlaced(x, y, z);
        LavaSimulation.ls.onBlockPlaced(x, y, z);
    }

    /**
     * Returns the fluid block type managed by this simulation.
     * @return the {@link BlockData} representing the fluid block type
     */
    public final BlockData getFluidType() {
        return fluidType;
    }

    /**
     * Adds a full source block at a world position.
     * @param x the {@code int} argument; the source x value
     * @param y the {@code int} argument; the source y value
     * @param z the {@code int} argument; the source z value
     * @return {@code true} when the source was added; otherwise {@code false}
     */
    public final boolean addSource(int x, int y, int z) {
        if (y < 0 || y >= Chunk.SIZE_Y) return false;
        FluidPos pos = new FluidPos(x, y, z);
        byte block = World.wrld.getBlockTypeAt(x, y, z);
        if (block != BlockData.AIR.getId() && block != fluidType.getId()) return false;
        sources.add(pos);
        setFluid(pos, MAX_LEVEL);
        enqueue(pos);
        return true;
    }

    /**
     * Removes a fluid cell and recalculates its connected flow.
     * @param x the {@code int} argument; the fluid x value
     * @param y the {@code int} argument; the fluid y value
     * @param z the {@code int} argument; the fluid z value
     * @return {@code true} when fluid was removed; otherwise {@code false}
     */
    public final boolean removeFluid(int x, int y, int z) {
        FluidPos pos = new FluidPos(x, y, z);
        if (!isFluid(pos)) return false;
        sources.remove(pos);
        removeAndRebuild(pos);
        return true;
    }

    /**
     * Removes a fluid cell that is immediately being occupied by another block.
     * Unlike collecting fluid in a bucket, placement must not rebuild the whole
     * connected fluid component: an ocean is one component and rebuilding it on
     * the render thread can freeze or exhaust the game.
     *
     * @param x the world x value
     * @param y the world y value
     * @param z the world z value
     * @return {@code true} when a fluid cell was displaced
     */
    public final boolean displaceForBlockPlacement(int x, int y, int z) {
        FluidPos pos = new FluidPos(x, y, z);
        if (!isFluid(pos)) return false;

        sources.remove(pos);
        slopes.remove(pos);
        queue.remove(pos);
        queued.remove(pos);
        World.wrld.setFluidLevelAt(x, y, z, (byte) 0);
        World.wrld.setBlockTypeAt(x, y, z, BlockData.AIR.getId());
        mark(pos);
        enqueueNeighbours(pos);
        return true;
    }

    /**
     * Checks whether a position is a source owned by this simulation.
     * @param x the {@code int} argument; the source x value
     * @param y the {@code int} argument; the source y value
     * @param z the {@code int} argument; the source z value
     * @return {@code true} if the position is a source; otherwise {@code false}
     */
    public final boolean isSource(int x, int y, int z) {
        return sources.contains(new FluidPos(x, y, z));
    }

    /**
     * Advances the simulation according to its configured step time.
     * @param delta the {@code float} argument; the elapsed time in seconds
     */
    public final void update(float delta) {
        timer = Math.min(timer + delta, stepTime * 2.0f);
        int remainingBudget = MAX_UPDATES_PER_FRAME;
        while (timer >= stepTime && remainingBudget > 0) {
            timer -= stepTime;
            int count = Math.min(queue.size(), remainingBudget);
            while (count-- > 0) {
                FluidPos pos = queue.poll();
                if (pos == null) continue;
                queued.remove(pos);
                updateCell(pos);
                remainingBudget--;
            }
        }
        rebuildChangedChunks();
    }

    /**
     * Updates surrounding flow after a block is destroyed.
     * @param x the {@code int} argument; the block x value
     * @param y the {@code int} argument; the block y value
     * @param z the {@code int} argument; the block z value
     */
    public final void onBlockDestroyed(int x, int y, int z) {
        FluidPos pos = new FluidPos(x, y, z);
        slopes.remove(pos);
        mark(pos);
        enqueueNeighbours(pos);
    }

    /**
     * Updates surrounding flow after a block is placed.
     * @param x the {@code int} argument; the block x value
     * @param y the {@code int} argument; the block y value
     * @param z the {@code int} argument; the block z value
     */
    public final void onBlockPlaced(int x, int y, int z) {
        FluidPos pos = new FluidPos(x, y, z);
        sources.remove(pos);
        slopes.remove(pos);
        World.wrld.setFluidLevelAt(x, y, z, (byte) 0);
        mark(pos);
        enqueueNeighbours(pos);
    }

    /**
     * Updates one queued fluid cell.
     */
    private void updateCell(FluidPos pos) {
        if (!isFluid(pos)) return;
        byte level = levelAt(pos);
        if (level < MIN_LEVEL) return;
        renewSource(pos);
        if (sources.contains(pos)) level = MAX_LEVEL;
        refreshSlope(pos, level);

        FluidPos below = new FluidPos(pos.x(), pos.y() - 1, pos.z());
        if (solidify(pos, below)) return;
        if (solidify(pos)) return;

        if (canContainFluid(below) && levelAt(below) < MAX_LEVEL) {
            setFluid(below, MAX_LEVEL);
            enqueue(below);
            return;
        }

        if (level <= MIN_LEVEL) return;
        byte spreadLevel = (byte) (level - 1);
        spread(pos, new FluidPos(pos.x() + 1, pos.y(), pos.z()), spreadLevel);
        spread(pos, new FluidPos(pos.x() - 1, pos.y(), pos.z()), spreadLevel);
        spread(pos, new FluidPos(pos.x(), pos.y(), pos.z() + 1), spreadLevel);
        spread(pos, new FluidPos(pos.x(), pos.y(), pos.z() - 1), spreadLevel);
    }

    /**
     * Resolves a fluid collision in the downward flow direction. Falling lava
     * meeting water creates stone in the water cell. Water falling onto lava
     * cools the lava into obsidian when it is a source, or stone when flowing.
     */
    private boolean solidify(FluidPos pos, FluidPos below) {
        if (below.y() < 0) return false;
        byte belowBlock = World.wrld.getBlockTypeAt(below.x(), below.y(), below.z());

        if (fluidType == BlockData.LAVA && belowBlock == BlockData.WATER.getId()) {
            solidifyFluid(below, BlockData.STONE);
            return true;
        }

        if (fluidType == BlockData.WATER && belowBlock == BlockData.LAVA.getId()) {
            BlockData result = LavaSimulation.ls.isSource(below.x(), below.y(), below.z())
                    ? BlockData.OBSIDIAN : BlockData.STONE;
            solidifyFluid(below, result);
            return true;
        }

        return false;
    }

    /**
     * Resolves contact with the opposite fluid on the four horizontal sides.
     */
    private boolean solidify(FluidPos pos) {
        FluidPos[] neighbours = {
                new FluidPos(pos.x() + 1, pos.y(), pos.z()),
                new FluidPos(pos.x() - 1, pos.y(), pos.z()),
                new FluidPos(pos.x(), pos.y(), pos.z() + 1),
                new FluidPos(pos.x(), pos.y(), pos.z() - 1)
        };

        if (fluidType == BlockData.LAVA) {
            for (FluidPos neighbour : neighbours) {
                if (World.wrld.getBlockTypeAt(neighbour.x(), neighbour.y(), neighbour.z())
                        == BlockData.WATER.getId()) {
                    solidifyFluid(pos, sources.contains(pos)
                            ? BlockData.OBSIDIAN : BlockData.STONE);
                    return true;
                }
            }
            return false;
        }

        if (fluidType == BlockData.WATER) {
            boolean solidified = false;
            for (FluidPos neighbour : neighbours) {
                if (World.wrld.getBlockTypeAt(neighbour.x(), neighbour.y(), neighbour.z())
                        != BlockData.LAVA.getId()) continue;
                BlockData result = LavaSimulation.ls.isSource(
                        neighbour.x(), neighbour.y(), neighbour.z())
                        ? BlockData.OBSIDIAN : BlockData.STONE;
                solidifyFluid(neighbour, result);
                solidified = true;
            }
            return solidified;
        }

        return false;
    }

    /**
     * Replaces a fluid cell with a solid and invalidates both simulations.
     */
    private static void solidifyFluid(FluidPos pos, BlockData result) {
        byte blockId = World.wrld.getBlockTypeAt(pos.x(), pos.y(), pos.z());
        BlockData replacedFluid = BlockData.fromId(blockId);
        FluidSimulation owner = forBlock(replacedFluid);
        if (owner == null) return;

        owner.sources.remove(pos);
        owner.slopes.remove(pos);
        owner.queue.remove(pos);
        owner.queued.remove(pos);
        World.wrld.setFluidLevelAt(pos.x(), pos.y(), pos.z(), (byte) 0);
        World.wrld.setBlockTypeAt(pos.x(), pos.y(), pos.z(), result.getId());

        owner.markChangedArea(pos);
        FluidSimulation water = WaterSimulation.ws;
        FluidSimulation lava = LavaSimulation.ls;
        water.enqueueNeighbours(pos);
        lava.enqueueNeighbours(pos);
    }

    /**
     * Marks the changed cell and neighbouring chunks whose boundary faces may change.
     */
    private void markChangedArea(FluidPos pos) {
        mark(pos);
        mark(new FluidPos(pos.x() + 1, pos.y(), pos.z()));
        mark(new FluidPos(pos.x() - 1, pos.y(), pos.z()));
        mark(new FluidPos(pos.x(), pos.y(), pos.z() + 1));
        mark(new FluidPos(pos.x(), pos.y(), pos.z() - 1));
    }

    /**
     * Renews a supported source when the fluid permits infinite sources.
     */
    private void renewSource(FluidPos pos) {
        if (!renewableSources || sources.contains(pos) || !hasSourceSupport(pos)) return;
        int adjacent = 0;
        if (sources.contains(new FluidPos(pos.x() + 1, pos.y(), pos.z()))) adjacent++;
        if (sources.contains(new FluidPos(pos.x() - 1, pos.y(), pos.z()))) adjacent++;
        if (sources.contains(new FluidPos(pos.x(), pos.y(), pos.z() + 1))) adjacent++;
        if (sources.contains(new FluidPos(pos.x(), pos.y(), pos.z() - 1))) adjacent++;
        if (adjacent >= 2) {
            sources.add(pos);
            setFluid(pos, MAX_LEVEL);
            enqueueNeighbours(pos);
        }
    }

    /**
     * Returns whether a source has solid or source support below it.
     */
    private boolean hasSourceSupport(FluidPos pos) {
        if (pos.y() <= 0) return false;
        FluidPos below = new FluidPos(pos.x(), pos.y() - 1, pos.z());
        return sources.contains(below) || World.wrld.isBlockSolid(below.x(), below.y(), below.z());
    }

    /**
     * Spreads a fluid level into a neighbouring cell.
     */
    private void spread(FluidPos from, FluidPos pos, byte level) {
        if (level < MIN_LEVEL || !canContainFluid(pos) || levelAt(pos) >= level) return;
        FluidSlope slope = hasDeepDrop(pos)
                ? new FluidSlope(pos.x() - from.x(), pos.z() - from.z()) : null;
        setFluid(pos, level, slope);
        enqueue(pos);
    }

    /**
     * A free cell directly below means the surface drops by more than one block.
     */
    private boolean hasDeepDrop(FluidPos pos) {
        if (pos.y() <= 0) return false;
        FluidPos below = new FluidPos(pos.x(), pos.y() - 1, pos.z());
        byte blockId = World.wrld.getBlockTypeAt(below.x(), below.y(), below.z());
        if (blockId == BlockData.AIR.getId() || blockId == fluidType.getId()) return true;
        BlockData data = BlockData.fromId(blockId);
        return data != null && (data.isPlant() || World.wrld.getCropAt(
                below.x(), below.y(), below.z()) != null);
    }

    /**
     * Keeps a falling edge aligned after terrain is changed beneath existing fluid.
     */
    private void refreshSlope(FluidPos pos, byte level) {
        if (!hasDeepDrop(pos)) {
            setSlope(pos, null);
            return;
        }
        if (slopes.containsKey(pos)) return;

        FluidPos[] neighbours = {
                new FluidPos(pos.x() + 1, pos.y(), pos.z()),
                new FluidPos(pos.x() - 1, pos.y(), pos.z()),
                new FluidPos(pos.x(), pos.y(), pos.z() + 1),
                new FluidPos(pos.x(), pos.y(), pos.z() - 1)
        };
        FluidPos upstream = null;
        byte upstreamLevel = level;
        for (FluidPos neighbour : neighbours) {
            if (isFluid(neighbour) && levelAt(neighbour) > upstreamLevel) {
                upstream = neighbour;
                upstreamLevel = levelAt(neighbour);
            }
        }
        if (upstream != null) {
            setSlope(pos, new FluidSlope(pos.x() - upstream.x(), pos.z() - upstream.z()));
        }
    }

    /**
     * Places or updates a fluid cell.
     */
    private void setFluid(FluidPos pos, byte level) {
        setFluid(pos, level, null);
    }

    /**
     * Places or updates a fluid cell and its optional downhill surface.
     */
    private void setFluid(FluidPos pos, byte level, FluidSlope slope) {
        if (pos.y() < 0 || pos.y() >= Chunk.SIZE_Y || !canContainFluid(pos)) return;
        byte currentBlock = World.wrld.getBlockTypeAt(pos.x(), pos.y(), pos.z());
        if (currentBlock != BlockData.AIR.getId() && currentBlock != fluidType.getId()) {
            BlockData data = BlockData.fromId(currentBlock);
            if (data == null || !data.isPlant()) return;
            World.wrld.removeBlockAt(pos.x(), pos.y(), pos.z());
        }
        if (currentBlock == fluidType.getId() && levelAt(pos) == level) {
            setSlope(pos, slope);
            return;
        }
        World.wrld.setBlockTypeAt(pos.x(), pos.y(), pos.z(), fluidType.getId());
        World.wrld.setFluidLevelAt(pos.x(), pos.y(), pos.z(), level);
        setSlope(pos, slope);
        mark(pos);
    }

    /**
     * Updates slope metadata and invalidates its mesh when the shape changes.
     */
    private void setSlope(FluidPos pos, FluidSlope slope) {
        FluidSlope previous = slope == null ? slopes.remove(pos) : slopes.put(pos, slope);
        if ((previous == null && slope != null) || (previous != null && !previous.equals(slope))) {
            markChangedArea(pos);
        }
    }

    /**
     * Removes one cell belonging to this fluid.
     */
    private void removeFluidCell(FluidPos pos) {
        if (!isFluid(pos)) return;
        slopes.remove(pos);
        World.wrld.setFluidLevelAt(pos.x(), pos.y(), pos.z(), (byte) 0);
        World.wrld.setBlockTypeAt(pos.x(), pos.y(), pos.z(), BlockData.AIR.getId());
        mark(pos);
    }

    /**
     * Removes and reconstructs the connected fluid component.
     */
    private void removeAndRebuild(FluidPos removed) {
        Set<FluidPos> component = collect(removed);
        Set<FluidPos> componentSources = new HashSet<>();
        for (FluidPos pos : component) if (sources.contains(pos)) componentSources.add(pos);
        for (FluidPos pos : component) removeFluidCell(pos);
        queue.removeAll(component);
        queued.removeAll(component);
        for (FluidPos source : componentSources) setFluid(source, MAX_LEVEL);

        Queue<FluidPos> rebuildQueue = new ArrayDeque<>(componentSources);
        Set<FluidPos> rebuilt = new HashSet<>(componentSources);
        while (!rebuildQueue.isEmpty()) {
            FluidPos pos = rebuildQueue.poll();
            if (!isFluid(pos)) continue;
            byte level = levelAt(pos);
            FluidPos below = new FluidPos(pos.x(), pos.y() - 1, pos.z());
            if (component.contains(below) && canContainFluid(below) && levelAt(below) < MAX_LEVEL) {
                setFluid(below, MAX_LEVEL);
                if (rebuilt.add(below)) rebuildQueue.add(below);
            }
            if (level <= MIN_LEVEL) continue;
            byte spreadLevel = (byte) (level - 1);
            rebuild(new FluidPos(pos.x() + 1, pos.y(), pos.z()), spreadLevel, component, rebuilt, rebuildQueue);
            rebuild(new FluidPos(pos.x() - 1, pos.y(), pos.z()), spreadLevel, component, rebuilt, rebuildQueue);
            rebuild(new FluidPos(pos.x(), pos.y(), pos.z() + 1), spreadLevel, component, rebuilt, rebuildQueue);
            rebuild(new FluidPos(pos.x(), pos.y(), pos.z() - 1), spreadLevel, component, rebuilt, rebuildQueue);
        }
        for (FluidPos pos : component) {
            if (isFluid(pos)) enqueue(pos); else enqueueNeighbours(pos);
        }
    }

    /**
     * Rebuilds one cell inside a removed fluid component.
     */
    private void rebuild(FluidPos pos, byte level, Set<FluidPos> component,
                         Set<FluidPos> rebuilt, Queue<FluidPos> rebuildQueue) {
        if (level < MIN_LEVEL || !component.contains(pos) || !canContainFluid(pos) || levelAt(pos) >= level) return;
        setFluid(pos, level);
        if (rebuilt.add(pos)) rebuildQueue.add(pos);
    }

    /**
     * Collects the connected component belonging to this fluid.
     */
    private Set<FluidPos> collect(FluidPos start) {
        Set<FluidPos> component = new HashSet<>();
        Queue<FluidPos> searchQueue = new ArrayDeque<>();
        Set<FluidPos> visited = new HashSet<>();
        if (isFluid(start)) {
            visited.add(start);
            searchQueue.add(start);
        }
        while (!searchQueue.isEmpty()) {
            FluidPos pos = searchQueue.poll();
            component.add(pos);
            addFluidNeighbour(new FluidPos(pos.x() + 1, pos.y(), pos.z()), searchQueue, visited);
            addFluidNeighbour(new FluidPos(pos.x() - 1, pos.y(), pos.z()), searchQueue, visited);
            addFluidNeighbour(new FluidPos(pos.x(), pos.y() + 1, pos.z()), searchQueue, visited);
            addFluidNeighbour(new FluidPos(pos.x(), pos.y() - 1, pos.z()), searchQueue, visited);
            addFluidNeighbour(new FluidPos(pos.x(), pos.y(), pos.z() + 1), searchQueue, visited);
            addFluidNeighbour(new FluidPos(pos.x(), pos.y(), pos.z() - 1), searchQueue, visited);
        }
        return component;
    }

    /**
     * Adds a matching fluid neighbour to a component search.
     */
    private void addFluidNeighbour(FluidPos pos, Queue<FluidPos> searchQueue, Set<FluidPos> visited) {
        if (visited.add(pos) && isFluid(pos)) searchQueue.add(pos);
    }

    /**
     * Returns whether a cell can receive this fluid.
     */
    private boolean canContainFluid(FluidPos pos) {
        if (pos.y() < 0 || pos.y() >= Chunk.SIZE_Y) return false;
        byte blockId = World.wrld.getBlockTypeAt(pos.x(), pos.y(), pos.z());
        if (blockId == BlockData.AIR.getId() || blockId == fluidType.getId()) return true;
        BlockData data = BlockData.fromId(blockId);
        Crop crop = World.wrld.getCropAt(pos.x(), pos.y(), pos.z());
        if (data != null && data.isPlant()) {
            ParticleEngine.peng.spawnPlant(new BlockPos(data, pos.x(), pos.y(), pos.z()), data);
        } else if (crop != null) {
            int frame = crop.getStage().getFrameIndex();
            ParticleEngine.peng.spawnCrop(pos.x(), pos.y(), pos.z(),
                    ResourceManager.rem.getCropSpritesheets().get(crop), frame);
        }
        return data != null && (data.isPlant() || crop != null);
    }

    /**
     * Returns whether a position contains this simulation's fluid.
     */
    private boolean isFluid(FluidPos pos) {
        return World.wrld.getBlockTypeAt(pos.x(), pos.y(), pos.z()) == fluidType.getId();
    }

    /**
     * Returns the stored fluid level at a position.
     */
    private byte levelAt(FluidPos pos) {
        return World.wrld.getFluidLevelAt(pos.x(), pos.y(), pos.z());
    }

    /**
     * Adds a position to the pending update queue.
     */
    private void enqueue(FluidPos pos) {
        if (queued.add(pos)) queue.add(pos);
    }

    /**
     * Adds a position and all adjacent positions to the update queue.
     */
    private void enqueueNeighbours(FluidPos pos) {
        enqueue(pos);
        enqueue(new FluidPos(pos.x() + 1, pos.y(), pos.z()));
        enqueue(new FluidPos(pos.x() - 1, pos.y(), pos.z()));
        enqueue(new FluidPos(pos.x(), pos.y() + 1, pos.z()));
        enqueue(new FluidPos(pos.x(), pos.y() - 1, pos.z()));
        enqueue(new FluidPos(pos.x(), pos.y(), pos.z() + 1));
        enqueue(new FluidPos(pos.x(), pos.y(), pos.z() - 1));
    }

    /**
     * Marks a chunk for mesh rebuilding.
     */
    private void mark(FluidPos pos) {
        changedChunks.add(World.wrld.get2DKey(Math.floorDiv(pos.x(), Chunk.SIZE_X),
                Math.floorDiv(pos.z(), Chunk.SIZE_Z)));
    }

    /**
     * Rebuilds meshes changed by the most recent simulation steps.
     */
    private void rebuildChangedChunks() {
        if (changedChunks.isEmpty() || GameMaster.game == null) return;
        for (long key : changedChunks) {
            int chunkX = (int) (key >> 32);
            int chunkZ = (int) key;
            GameMaster.game.rebuildChunkMeshAt(chunkX * Chunk.SIZE_X, chunkZ * Chunk.SIZE_Z);
        }
        changedChunks.clear();
    }
}
