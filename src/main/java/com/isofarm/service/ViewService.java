package com.isofarm.service;

import com.isofarm.data.BlockData;
import com.isofarm.data.View;
import com.isofarm.entity.Player;
import com.isofarm.item.iBlock;
import com.isofarm.utils.Settings;
import com.isofarm.wrld.Chunk;
import com.isofarm.wrld.World;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Classifies the space occupied by the player and exposes the clipping volume
 * used by the isometric fog-of-war shaders.
 */
@SuppressWarnings("all")
public final class ViewService implements Service<View> {
    private static final int MIN_ROOM_HEIGHT = 2;
    private static final int MAX_ROOM_HEIGHT = 8;
    private static final float MIN_WALL_COVERAGE = 0.90f;
    private static final float MIN_ROOF_COVERAGE = 0.80f;

    private View view = View.EXTERIOR;
    private final Vector4f bounds = new Vector4f();
    private final Vector3f playerPosition = new Vector3f();
    private float floorY;
    private float ceilingY;

    /** Re-evaluates the view volume around the player's current voxel. */
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
            bounds.set(room.minX, room.minZ, room.maxX + 1.0f, room.maxZ + 1.0f);
            ceilingY = room.ceilingY;
            return;
        }

        if (overheadY >= 0
                && !isVegetationCover(world, playerX, overheadY, playerZ)
                && hasUndergroundWalls(world, playerX, playerY, playerZ)
                && hasSolidOverburden(world, playerX, overheadY, playerZ)) {
            view = View.UNDERGROUND;
            float radius = Settings.getUndergroundViewRadius();
            bounds.set(player.getPosition().x - radius, player.getPosition().z - radius,
                    player.getPosition().x + radius, player.getPosition().z + radius);
            ceilingY = Math.min(overheadY, player.getPosition().y + Settings.getUndergroundCutHeight());
            return;
        }

        view = View.EXTERIOR;
        ceilingY = Chunk.SIZE_Y;
    }

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

    private int findBoundary(World world, int x, int y, int z,
                             int stepX, int stepZ, int radius) {
        for (int distance = 1; distance <= radius; distance++) {
            int testX = x + stepX * distance;
            int testZ = z + stepZ * distance;
            if (isWall(world, testX, y, testZ)) return (stepX != 0 ? testX : testZ);
        }
        return Integer.MIN_VALUE;
    }

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

    private boolean isWall(World world, int x, int y, int z) {
        return isDoor(world, x, y, z)
                || world.isBlockSolid(x, y, z)
                || world.isBlockSolid(x, y + 1, z);
    }

    private boolean isDoor(World world, int x, int y, int z) {
        iBlock interactive = world.getInteractiveBlockAt(x, y, z);
        if (interactive != null && interactive.getType().isDoor()) return true;
        BlockData data = BlockData.fromId(world.getBlockTypeAt(x, y, z));
        return data != null && data.isDoor();
    }

    private int findOverhead(World world, int x, int playerY, int z) {
        for (int y = playerY + MIN_ROOM_HEIGHT;
             y <= Math.min(Chunk.SIZE_Y - 1, playerY + MAX_ROOM_HEIGHT); y++) {
            if (world.isBlockSolid(x, y, z)) return y;
        }
        return -1;
    }

    private boolean hasRoofColumn(World world, int x, int playerY, int z, int expectedY) {
        int minY = Math.max(playerY + MIN_ROOM_HEIGHT, expectedY - 1);
        int maxY = Math.min(Chunk.SIZE_Y - 1, expectedY + 1);
        for (int y = minY; y <= maxY; y++) {
            if (world.isBlockSolid(x, y, z)) return true;
        }
        return false;
    }

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
     * This mirrors the directional cutaway rules used by the world shaders.
     */
    public boolean isVisible(Vector3f position, Vector3f cameraPosition) {
        if (view == View.EXTERIOR || position == null) return true;
        if (position.y >= ceilingY) return false;
        if (view == View.INTERIOR) {
            if (position.x < bounds.x || position.x > bounds.z
                    || position.z < bounds.y || position.z > bounds.w) return false;
            return cameraPosition == null || !isFrontCutaway(position, cameraPosition);
        }
        float offsetX = position.x - playerPosition.x;
        float offsetZ = position.z - playerPosition.z;
        float radius = Settings.getUndergroundViewRadius();
        if (offsetX * offsetX + offsetZ * offsetZ > radius * radius) return false;
        return cameraPosition == null || !isUndergroundCutaway(
                position, cameraPosition, offsetX, offsetZ, radius);
    }

    private boolean isFrontCutaway(Vector3f position, Vector3f cameraPosition) {
        float cameraOffsetX = cameraPosition.x - playerPosition.x;
        float cameraOffsetZ = cameraPosition.z - playerPosition.z;
        float length = (float) Math.sqrt(cameraOffsetX * cameraOffsetX
                + cameraOffsetZ * cameraOffsetZ);
        float toCameraX = length > 0.001f ? cameraOffsetX / length : 0.7071f;
        float toCameraZ = length > 0.001f ? cameraOffsetZ / length : 0.7071f;
        boolean frontWall = (toCameraX > 0.15f && position.x > bounds.z - 1.01f)
                || (toCameraX < -0.15f && position.x < bounds.x + 1.01f)
                || (toCameraZ > 0.15f && position.z > bounds.w - 1.01f)
                || (toCameraZ < -0.15f && position.z < bounds.y + 1.01f);
        return frontWall && position.y > floorY + 0.08f;
    }

    private boolean isUndergroundCutaway(Vector3f position, Vector3f cameraPosition,
                                         float offsetX, float offsetZ, float radius) {
        float cameraOffsetX = cameraPosition.x - playerPosition.x;
        float cameraOffsetZ = cameraPosition.z - playerPosition.z;
        float length = (float) Math.sqrt(cameraOffsetX * cameraOffsetX
                + cameraOffsetZ * cameraOffsetZ);
        float toCameraX = length > 0.001f ? cameraOffsetX / length : 0.7071f;
        float toCameraZ = length > 0.001f ? cameraOffsetZ / length : 0.7071f;
        float frontDepth = offsetX * toCameraX + offsetZ * toCameraZ;
        float sideDistance = Math.abs(offsetX * -toCameraZ + offsetZ * toCameraX);
        return frontDepth > 0.20f && sideDistance < Math.max(1.5f, radius * 0.32f)
                && position.y > floorY + 0.08f;
    }

    private record Room(int minX, int maxX, int minZ, int maxZ, int ceilingY) {}
}
