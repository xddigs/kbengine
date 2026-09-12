package org.kbeng.service;

import org.kbeng.data.BlockData;
import org.kbeng.data.View;
import org.kbeng.entity.Player;
import org.kbeng.item.iBlock;
import org.kbeng.utils.Settings;
import org.kbeng.wrld.Chunk;
import org.kbeng.wrld.World;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * ViewService provides view service capabilities within the service subsystem.
 * It provides shared runtime services, registries, and policy logic consumed by orchestrators and feature modules.
 * The service acts as a shared policy and state access point for other runtime modules.
 * It implements Service<View>, providing a concrete strategy for this subsystem contract.
 */
public final class ViewService implements Service<View> {
    private static final int MIN_ROOM_HEIGHT = 2;
    private static final int MAX_ROOM_HEIGHT = 8;
    private static final float MIN_WALL_COVERAGE = 0.90f;
    private static final float MIN_ROOF_COVERAGE = 0.80f;
    private static final float VIEW_BOUNDS_PADDING = 0.06f;

    private View view = View.EXTERIOR;
    private final Vector4f bounds = new Vector4f();
    private final Vector3f playerPosition = new Vector3f();
    private float floorY;
    private float ceilingY;

    /**
     * Re-evaluates the view volume around the player's current voxel.
     * @param world the {@link World} argument; the world to search for the player.
     * @param player the {@link Player} argument; the player to classify.
     */
    public void update(World world, Player player) {
        if (world == null || player == null) {
            view = View.EXTERIOR;
            return;
        }

        int playerX = (int) Math.floor(player.getPosition().x);
        int playerY = (int) Math.floor(player.getPosition().y + 0.05f);
        int playerZ = (int) Math.floor(player.getPosition().z);
        playerPosition.set(player.getPosition());
        floorY = player.getPosition().y;

        int overheadY = findOverhead(world, playerX, playerY, playerZ);
        Room room = overheadY < 0 ? null : findRoom(world, playerX, playerY, playerZ);
        if (room != null) {
            view = View.INTERIOR;
            bounds.set(room.minX - VIEW_BOUNDS_PADDING, room.minZ - VIEW_BOUNDS_PADDING,
                    room.maxX + 1.0f + VIEW_BOUNDS_PADDING, room.maxZ + 1.0f + VIEW_BOUNDS_PADDING);
            ceilingY = room.ceilingY;
            return;
        }

        if (overheadY >= 0
                && !isVegetationCover(world, playerX, overheadY, playerZ)
                && hasUndergroundWalls(world, playerX, playerY, playerZ)
                && hasSolidOverburden(world, playerX, overheadY, playerZ)) {
            view = View.UNDERGROUND;
            float radius = Settings.getUndergroundViewRadius();
            float paddedRadius = radius + VIEW_BOUNDS_PADDING;
            bounds.set(player.getPosition().x - paddedRadius, player.getPosition().z - paddedRadius,
                    player.getPosition().x + paddedRadius, player.getPosition().z + paddedRadius);
            ceilingY = Math.min(overheadY, player.getPosition().y + Settings.getUndergroundCutHeight());
            return;
        }

        view = View.EXTERIOR;
        ceilingY = Chunk.SIZE_Y;
    }

    /**
     * Finds the room that the player is in.
     * @param world the {@link World} argument; the world to search
     * @param playerX the {@code int} argument; the player's x position
     * @param playerY the {@code int} argument; the player's y position
     * @param playerZ the {@code int} argument; the player's z position
     * @return the {@link Room} representing the room, or {@code null} if none
     */
    private Room findRoom(World world, int playerX, int playerY, int playerZ) {
        int radius = Settings.getInteriorDetectionRadius();
        int minX = findBoundary(world, playerX, playerY, playerZ, -1, 0, radius);
        int maxX = findBoundary(world, playerX, playerY, playerZ, 1, 0, radius);
        int minZ = findBoundary(world, playerX, playerY, playerZ, 0, -1, radius);
        int maxZ = findBoundary(world, playerX, playerY, playerZ, 0, 1, radius);
        if (minX == Integer.MIN_VALUE || maxX == Integer.MIN_VALUE
                || minZ == Integer.MIN_VALUE || maxZ == Integer.MIN_VALUE) return null;

        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        if (!wallIsClosed(world, minX, playerY, minZ, maxZ, true)
                || !wallIsClosed(world, maxX, playerY, minZ, maxZ, true)
                || !wallIsClosed(world, minZ, playerY, minX, maxX, false)
                || !wallIsClosed(world, maxZ, playerY, minX, maxX, false)) return null;

        int ceiling = findOverhead(world, playerX, playerY, playerZ);
        if (ceiling < playerY + MIN_ROOM_HEIGHT) return null;
        int roofColumns = 0;
        int columns = Math.max(1, (width - 2) * (depth - 2));
        for (int x = minX + 1; x < maxX; x++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                if (hasRoofColumn(world, x, playerY, z, ceiling)) roofColumns++;
            }
        }
        if (roofColumns < Math.ceil(columns * MIN_ROOF_COVERAGE)) return null;
        return new Room(minX, maxX, minZ, maxZ, ceiling);
    }

    /**
     * Finds the first solid boundary of the given radius from the given voxel.
     * @param world the {@link World} argument; the world to search
     * @param x the {@code int} argument; the voxel to search from
     * @param y the {@code int} argument; the voxel to search from
     * @param z the {@code int} argument; the voxel to search from
     * @param stepX the {@code int} argument; the direction to search in
     * @param stepZ the {@code int} argument; the direction to search in
     * @param radius the {@code int} argument; the radius of the search
     * @return the {@code int} value of the boundary voxel, or {@code Integer.MIN_VALUE} if none
     */
    private int findBoundary(World world, int x, int y, int z,
                             int stepX, int stepZ, int radius) {
        for (int distance = 1; distance <= radius; distance++) {
            int testX = x + stepX * distance;
            int testZ = z + stepZ * distance;
            if (isWall(world, testX, y, testZ)) return (stepX != 0 ? testX : testZ);
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Returns true if the given wall is closed.
     * @param world the {@link World} argument; the world to search
     * @param fixed the {@code int} argument; the fixed voxel to search from
     * @param y the {@code int} argument; the voxel to search from
     * @param start the {@code int} argument; the voxel to search from
     * @param end the {@code int} argument; the voxel to search from
     * @param fixedX the {@code boolean} argument; whether the fixed voxel is the x or z axis
     * @return {@code true} if the wall is closed; otherwise {@code false}
     */
    private boolean wallIsClosed(World world, int fixed, int y, int start, int end,
                                 boolean fixedX) {
        int covered = 0;
        int length = end - start + 1;
        for (int value = start; value <= end; value++) {
            int x = fixedX ? fixed : value;
            int z = fixedX ? value : fixed;
            if (isWall(world, x, y, z)) covered++;
        }
        return covered >= Math.ceil(length * MIN_WALL_COVERAGE);
    }

    /**
     * Returns true if the given voxel is a solid wall.
     * @param world the {@link World} argument; the world to search
     * @param x the {@code int} argument; the voxel to search from
     * @param y the {@code int} argument; the voxel to search from
     * @param z the {@code int} argument; the voxel to search from
     * @return {@code true} if the voxel is a solid wall; otherwise {@code false}
     */
    private boolean isWall(World world, int x, int y, int z) {
        return isDoor(world, x, y, z)
                || world.isBlockSolid(x, y, z)
                || world.isBlockSolid(x, y + 1, z);
    }

    /**
     * Returns true if the given voxel is a door.
     * @param world the {@link World} argument; the world to search
     * @param x the {@code int} argument; the voxel to search from
     * @param y the {@code int} argument; the voxel to search from
     * @param z the {@code int} argument; the voxel to search from
     * @return {@code true} if the voxel is a door; otherwise {@code false}
     */
    private boolean isDoor(World world, int x, int y, int z) {
        iBlock interactive = world.getInteractiveBlockAt(x, y, z);
        if (interactive != null && interactive.getType().isDoor()) return true;
        BlockData data = BlockData.fromId(world.getBlockTypeAt(x, y, z));
        return data != null && data.isDoor();
    }

    /**
     * Finds the highest solid voxel above the player's y position.
     * @param world the {@link World} argument; the world to search
     * @param x the {@code int} argument; the voxel to search from
     * @param playerY the {@code int} argument; the player's y position
     * @param z the {@code int} argument; the voxel to search from
     * @return the {@code int} value of the highest solid voxel, or {@code -1} if none
     */
    private int findOverhead(World world, int x, int playerY, int z) {
        for (int y = playerY + MIN_ROOM_HEIGHT;
             y <= Math.min(Chunk.SIZE_Y - 1, playerY + MAX_ROOM_HEIGHT); y++) {
            if (world.isBlockSolid(x, y, z)) return y;
        }
        return -1;
    }

    /**
     * Returns true if the given voxel is a solid column of roof.
     * @param world the {@link World} argument; the world to search
     * @param x the {@code int} argument; the voxel to search from
     * @param playerY the {@code int} argument; the player's y position
     * @param z the {@code int} argument; the voxel to search from
     * @param expectedY the {@code int} argument; the expected ceiling of the room
     * @return {@code true} if the voxel is a solid column of roof; otherwise {@code false}
     */
    private boolean hasRoofColumn(World world, int x, int playerY, int z, int expectedY) {
        int minY = Math.max(playerY + MIN_ROOM_HEIGHT, expectedY - 1);
        int maxY = Math.min(Chunk.SIZE_Y - 1, expectedY + 1);
        for (int y = minY; y <= maxY; y++) {
            if (world.isBlockSolid(x, y, z)) return true;
        }
        return false;
    }

    /**
     * Returns true if the given voxel is a solid column of overburden.
     * @param world the {@link World} argument; the world to search
     * @param x the {@code int} argument; the voxel to search from
     * @param ceiling the {@code int} argument; the ceiling of the room
     * @param z the {@code int} argument; the voxel to search from
     * @return {@code true} if the voxel is a solid column of overburden; otherwise {@code false}
     */
    private boolean hasSolidOverburden(World world, int x, int ceiling, int z) {
        for (int y = ceiling + 1; y < Chunk.SIZE_Y; y++) {
            if (world.isBlockSolid(x, y, z)) return true;
        }
        return false;
    }

    /**
     * Trees are solid for voxel collision purposes, but their canopy must not
     * turn the exterior view into an underground view.
     */
    private boolean isVegetationCover(World world, int x, int ceiling, int z) {
        BlockData cover = BlockData.fromId(world.getBlockTypeAt(x, ceiling, z));
        return cover == BlockData.OAK_LEAVES || cover == BlockData.SPRUCE_LEAVES
                || cover == BlockData.OAK_LOG || cover == BlockData.SPRUCE_LOG;
    }

    /** Requires lateral enclosure so an isolated tree or overhang stays exterior. */
    private boolean hasUndergroundWalls(World world, int x, int y, int z) {
        int walls = 0;
        int radius = Settings.getUndergroundViewRadius() >= 10.0f ? 3 : 2;
        if (findBoundary(world, x, y, z, -1, 0, radius) != Integer.MIN_VALUE) walls++;
        if (findBoundary(world, x, y, z, 1, 0, radius) != Integer.MIN_VALUE) walls++;
        if (findBoundary(world, x, y, z, 0, -1, radius) != Integer.MIN_VALUE) walls++;
        if (findBoundary(world, x, y, z, 0, 1, radius) != Integer.MIN_VALUE) walls++;
        return walls >= 2;
    }

    public View getView() {
        return view;
    }

    public Vector4f getBounds() {
        return new Vector4f(bounds);
    }

    public float getFloorY() {
        return floorY;
    }

    public float getCeilingY() {
        return ceilingY;
    }

    /** True when an emissive or interactive world object belongs to the visible volume. */
    public boolean isVisible(Vector3f position) {
        return isVisible(position, null);
    }

    /**
     * Returns whether a cell belongs to the visible, interactable volume.
     * This mirrors the volumetric fog shape used by the world shaders.
     */
    public boolean isVisible(Vector3f position, Vector3f cameraPosition) {
        if (view == View.EXTERIOR || position == null) return true;
        if (position.y >= ceilingY) return false;
        if (view == View.INTERIOR) {
            if (position.x < bounds.x || position.x > bounds.z
                    || position.z < bounds.y || position.z > bounds.w) return false;
            return true;
        }
        float offsetX = position.x - playerPosition.x;
        float offsetZ = position.z - playerPosition.z;
        float radius = Settings.getUndergroundViewRadius();
        return offsetX * offsetX + offsetZ * offsetZ <= radius * radius;
    }

    /** Describes the rectangular bounds and ceiling of a visible room. */
    private record Room(int minX, int maxX, int minZ, int maxZ, int ceilingY) {}
}
