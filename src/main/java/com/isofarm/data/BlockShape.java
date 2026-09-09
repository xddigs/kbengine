package com.isofarm.data;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometry and collision bounds for voxel blocks that are not full cubes.
 * Defines Axis-Aligned Bounding Boxes (AABB) for slabs, staircases, and fence
 * combinations, alongside utilities for collision detection, height evaluation, and raycasting.
 */
@SuppressWarnings("all")
@DataClass
public enum BlockShape {
    FULL_CUBE(new Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)),
    HORIZONTAL_SLAB(new Box(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f)),
    VERTICAL_SLAB_WEST(new Box(0.0f, 0.0f, 0.0f, 0.5f, 1.0f, 1.0f)),
    VERTICAL_SLAB_EAST(new Box(0.5f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)),
    VERTICAL_SLAB_NORTH(new Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.5f)),
    VERTICAL_SLAB_SOUTH(new Box(0.0f, 0.0f, 0.5f, 1.0f, 1.0f, 1.0f)),

    STAIRCASE_WEST(new Box(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f), new Box(0.5f, 0.5f, 0.0f, 1.0f, 1.0f, 1.0f)),
    STAIRCASE_EAST(new Box(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f), new Box(0.0f, 0.5f, 0.0f, 0.5f, 1.0f, 1.0f)),
    STAIRCASE_NORTH(new Box(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f), new Box(0.0f, 0.5f, 0.5f, 1.0f, 1.0f, 1.0f)),
    STAIRCASE_SOUTH(new Box(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f), new Box(0.0f, 0.5f, 0.0f, 1.0f, 1.0f, 0.5f)),
    STAIRCASE_CORNER_NW(new Box(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f), new Box(0.5f, 0.5f, 0.5f, 1.0f, 1.0f, 1.0f), new Box(0.0f, 0.5f, 0.0f, 0.5f, 1.0f, 0.5f)),
    STAIRCASE_CORNER_SW(new Box(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f), new Box(0.0f, 0.5f, 0.5f, 0.5f, 1.0f, 1.0f), new Box(0.5f, 0.5f, 0.0f, 1.0f, 1.0f, 0.5f)),
    STAIRCASE_CORNER_NE(new Box(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f), new Box(0.0f, 0.5f, 0.5f, 0.5f, 1.0f, 1.0f), new Box(0.5f, 0.5f, 0.5f, 1.0f, 1.0f, 1.0f)),
    STAIRCASE_CORNER_SE(new Box(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f), new Box(0.0f, 0.5f, 0.0f, 0.5f, 1.0f, 0.5f), new Box(0.5f, 0.5f, 0.0f, 1.0f, 1.0f, 0.5f)),

    TORCH_FLOOR(new Box(0.4f, 0.0f, 0.4f, 0.6f, 0.8f, 0.6f)),
    TORCH_WEST(new Box(0.0f, 0.0f, 0.4f, 0.16f, 0.8f, 0.6f)),
    TORCH_EAST(new Box(0.84f, 0.0f, 0.4f, 1.0f, 0.8f, 0.6f)),
    TORCH_NORTH(new Box(0.4f, 0.0f, 0.0f, 0.6f, 0.8f, 0.16f)),
    TORCH_SOUTH(new Box(0.4f, 0.0f, 0.84f, 0.6f, 0.8f, 1.0f)),

    FENCE_NONE(0),
    FENCE_N(1), FENCE_S(2), FENCE_NS(3),
    FENCE_W(4), FENCE_NW(5), FENCE_SW(6), FENCE_NSW(7),
    FENCE_E(8), FENCE_NE(9), FENCE_SE(10), FENCE_NSE(11),
    FENCE_WE(12), FENCE_NWE(13), FENCE_SWE(14), FENCE_NSWE(15);

    private static final float EPSILON = 0.00001f;
    private final Box[] boxes;

    /**
     * Constructs a block shape defined by one or more bounding boxes.
     * @param boxes The bounding boxes that make up this shape.
     */
    BlockShape(Box... boxes) {
        this.boxes = boxes;
    }

    /**
     * Constructs a fence shape from a connection bitmask.
     * @param fenceMask A 4-bit integer mask indicating connections (1:N, 2:S, 4:W, 8:E).
     */
    BlockShape(int fenceMask) {
        this.boxes = createFenceBoxes(fenceMask);
    }

    /**
     * Generates the required AABB bounding boxes for a fence shape based on its connection mask.
     * @param mask Connection bitmask.
     * @return Array of {@link Box} components forming the fence model.
     */
    private static Box[] createFenceBoxes(int mask) {
        List<Box> result = new ArrayList<>();
        result.add(new Box(0.375f, 0.0f, 0.375f, 0.625f, 1.0f, 0.625f));
        if ((mask & 1) != 0) addFenceRails(result, 0.4375f, 0.0f, 0.5625f, 0.5f);
        if ((mask & 2) != 0) addFenceRails(result, 0.4375f, 0.5f, 0.5625f, 1.0f);
        if ((mask & 4) != 0) addFenceRails(result, 0.0f, 0.4375f, 0.5f, 0.5625f);
        if ((mask & 8) != 0) addFenceRails(result, 0.5f, 0.4375f, 1.0f, 0.5625f);
        return result.toArray(new Box[0]);
    }

    /**
     * Helper method to attach upper and lower horizontal fence rails in a specific direction.
     *
     * @param boxes Target list to append rail boxes to.
     * @param minX  Minimum X bound.
     * @param minZ  Minimum Z bound.
     * @param maxX  Maximum X bound.
     * @param maxZ  Maximum Z bound.
     */
    private static void addFenceRails(List<Box> boxes, float minX, float minZ,
                                      float maxX, float maxZ) {
        boxes.add(new Box(minX, 0.375f, minZ, maxX, 0.5625f, maxZ));
        boxes.add(new Box(minX, 0.6875f, minZ, maxX, 0.875f, maxZ));
    }

    /**
     * Returns a defensive copy of the bounding boxes that compose this shape.
     * @return Array of {@link Box} instances.
     */
    public Box[] getBoxes() {
        return boxes.clone();
    }

    /**
     * Gets the total number of bounding boxes composing this shape.
     * @return Number of bounding boxes.
     */
    public int getBoxCount() {
        return boxes.length;
    }

    /**
     * Checks if this shape represents a standard 1x1x1 full cube.
     * @return {@code true} if it is a full cube; {@code false} otherwise.
     */
    public boolean isFullCube() {
        return this == FULL_CUBE;
    }

    /**
     * Checks if this shape is a vertically-oriented slab.
     * @return {@code true} if it matches any vertical slab variant.
     */
    public boolean isVerticalSlab() {
        return this == VERTICAL_SLAB_WEST || this == VERTICAL_SLAB_EAST
                || this == VERTICAL_SLAB_NORTH || this == VERTICAL_SLAB_SOUTH;
    }

    /**
     * Checks if this shape belongs to any fence variant.
     * @return {@code true} if this shape is a fence shape.
     */
    public boolean isFence() {
        return ordinal() >= FENCE_NONE.ordinal();
    }

    /**
     * Checks if this shape is a staircase variant.
     * @return {@code true} if it is a staircase.
     */
    public boolean isStaircase() {
        return this == STAIRCASE_WEST || this == STAIRCASE_EAST
                || this == STAIRCASE_NORTH || this == STAIRCASE_SOUTH
                || this == STAIRCASE_CORNER_NW || this == STAIRCASE_CORNER_SW
                || this == STAIRCASE_CORNER_NE || this == STAIRCASE_CORNER_SE;
    }

    /** Returns whether this shape describes a floor- or wall-mounted torch. */
    public boolean isTorch() {
        return this == TORCH_FLOOR || this == TORCH_WEST || this == TORCH_EAST
                || this == TORCH_NORTH || this == TORCH_SOUTH;
    }

    /** Resolves the torch shape attached to the face represented by a hit normal. */
    public static BlockShape torchFacing(int normalX, int normalY, int normalZ) {
        if (normalY > 0) return TORCH_FLOOR;
        if (normalX > 0) return TORCH_WEST;
        if (normalX < 0) return TORCH_EAST;
        if (normalZ > 0) return TORCH_NORTH;
        if (normalZ < 0) return TORCH_SOUTH;
        return TORCH_FLOOR;
    }

    /**
     * Resolves the appropriate fence {@link BlockShape} given a connection bitmask.
     * @param mask 4-bit connection mask (1=N, 2=S, 4=W, 8=E). Bits above the 4th are ignored.
     * @return Corresponding fence enum variant.
     */
    public static BlockShape fence(int mask) {
        return switch (mask & 15) {
            case 1 -> FENCE_N; case 2 -> FENCE_S; case 3 -> FENCE_NS;
            case 4 -> FENCE_W; case 5 -> FENCE_NW; case 6 -> FENCE_SW;
            case 7 -> FENCE_NSW; case 8 -> FENCE_E; case 9 -> FENCE_NE;
            case 10 -> FENCE_SE; case 11 -> FENCE_NSE; case 12 -> FENCE_WE;
            case 13 -> FENCE_NWE; case 14 -> FENCE_SWE; case 15 -> FENCE_NSWE;
            default -> FENCE_NONE;
        };
    }

    /**
     * Determines which direction a vertical slab should face based on where a block was targeted.
     *
     * @param pointX Target point X-coordinate in world space.
     * @param pointZ Target point Z-coordinate in world space.
     * @param blockX World grid position X of the targeted block.
     * @param blockZ World grid position Z of the targeted block.
     * @return The facing {@link BlockShape} for the vertical slab.
     */
    public static BlockShape verticalSlabFacing(float pointX, float pointZ,
                                                int blockX, int blockZ) {
        float offsetX = pointX - (blockX + 0.5f);
        float offsetZ = pointZ - (blockZ + 0.5f);
        if (Math.abs(offsetX) > Math.abs(offsetZ)) {
            return offsetX < 0.0f ? VERTICAL_SLAB_WEST : VERTICAL_SLAB_EAST;
        }
        return offsetZ < 0.0f ? VERTICAL_SLAB_NORTH : VERTICAL_SLAB_SOUTH;
    }

    /**
     * Determines which direction a staircase should face based on where a block was targeted.
     *
     * @param pointX Target point X-coordinate in world space.
     * @param pointZ Target point Z-coordinate in world space.
     * @param blockX World grid position X of the targeted block.
     * @param blockZ World grid position Z of the targeted block.
     * @return The facing {@link BlockShape} for the staircase.
     */
    public static BlockShape staircaseFacing(float pointX, float pointZ,
                                              int blockX, int blockZ) {
        float offsetX = pointX - (blockX + 0.5f);
        float offsetZ = pointZ - (blockZ + 0.5f);
        if (Math.abs(offsetX) > Math.abs(offsetZ)) {
            return offsetX < 0.0f ? STAIRCASE_WEST : STAIRCASE_EAST;
        }
        return offsetZ < 0.0f ? STAIRCASE_NORTH : STAIRCASE_SOUTH;
    }

    /**
     * Builds the connected mesh variant for a staircase. The mask uses the
     * same bits as fences: north, south, west and east (1, 2, 4 and 8).
     * Connections are intentionally type-agnostic so stone and wood stairs join.
     * @param orientation original placed staircase orientation
     * @param mask four-bit neighboring-staircase connection mask
     * @return the straight or corner staircase shape matching the connections
     */
    public static BlockShape staircaseConnected(BlockShape orientation, int mask) {
        if (!orientation.isStaircase()) return orientation;
        return switch (mask & 15) {
            case 5 -> STAIRCASE_CORNER_NW;
            case 6 -> STAIRCASE_CORNER_SW;
            case 9 -> STAIRCASE_CORNER_NE;
            case 10 -> STAIRCASE_CORNER_SE;
            default -> orientation;
        };
    }

    /**
     * Finds the highest local Y-level among all bounding boxes in this shape.
     * @return Maximum height offset (range 0.0f to 1.0f).
     */
    public float getTop() {
        float top = 0.0f;
        for (Box box : boxes) top = Math.max(top, box.maxY());
        return top;
    }

    /**
     * Finds the highest local Y-level at a specific local (X, Z) coordinate pair within the block.
     * @param localX Local X offset within the block (0.0f to 1.0f).
     * @param localZ Local Z offset within the block (0.0f to 1.0f).
     * @return Highest local Y value at that position, or 0.0f if no box covers it.
     */
    public float getTopAt(float localX, float localZ) {
        float top = 0.0f;
        for (Box box : boxes) {
            if (localX >= box.minX() && localX <= box.maxX()
                    && localZ >= box.minZ() && localZ <= box.maxZ()) {
                top = Math.max(top, box.maxY());
            }
        }
        return top;
    }

    /**
     * Tests whether a world-space AABB intersects with any bounding box of this shape.
     * @param blockX World position X of this block.
     * @param blockY World position Y of this block.
     * @param blockZ World position Z of this block.
     * @param minX   World-space minimum X of the colliding volume.
     * @param minY   World-space minimum Y of the colliding volume.
     * @param minZ   World-space minimum Z of the colliding volume.
     * @param maxX   World-space maximum X of the colliding volume.
     * @param maxY   World-space maximum Y of the colliding volume.
     * @param maxZ   World-space maximum Z of the colliding volume.
     * @return {@code true} if an intersection occurs; {@code false} otherwise.
     */
    public boolean intersects(int blockX, int blockY, int blockZ,
                              float minX, float minY, float minZ,
                              float maxX, float maxY, float maxZ) {
        for (Box box : boxes) {
            if (minX < blockX + box.maxX() && maxX > blockX + box.minX()
                    && minY < blockY + box.maxY() && maxY > blockY + box.minY()
                    && minZ < blockZ + box.maxZ() && maxZ > blockZ + box.minZ()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Casts a ray against all bounding boxes of this shape placed at a world position,
     * returning the nearest hit.
     *
     * @param origin    Ray origin in world coordinates.
     * @param direction Normalized ray direction vector.
     * @param blockX    World position X of this block.
     * @param blockY    World position Y of this block.
     * @param blockZ    World position Z of this block.
     * @return The closest {@link RayHit} details, or {@code null} if the ray misses all boxes.
     */
    public RayHit raycast(Vector3f origin, Vector3f direction,
                          int blockX, int blockY, int blockZ) {
        RayHit closest = null;
        for (Box box : boxes) {
            RayHit hit = raycastBox(origin, direction, box, blockX, blockY, blockZ);
            if (hit != null && (closest == null || hit.distance() < closest.distance())) {
                closest = hit;
            }
        }
        return closest;
    }

    /**
     * Helper ray-box slab intersection test for a single {@link Box} placed in world space.
     *
     * @param origin    Ray origin vector.
     * @param direction Normalized ray direction vector.
     * @param box       Local target bounding box.
     * @param blockX    World position X of this block.
     * @param blockY    World position Y of this block.
     * @param blockZ    World position Z of this block.
     * @return {@link RayHit} object containing hit distance and normal vector, or {@code null} on miss.
     */
    private static RayHit raycastBox(Vector3f origin, Vector3f direction, Box box,
                                     int blockX, int blockY, int blockZ) {
        float[] originAxis = {origin.x, origin.y, origin.z};
        float[] directionAxis = {direction.x, direction.y, direction.z};
        float[] minimum = {blockX + box.minX(), blockY + box.minY(), blockZ + box.minZ()};
        float[] maximum = {blockX + box.maxX(), blockY + box.maxY(), blockZ + box.maxZ()};
        float near = Float.NEGATIVE_INFINITY;
        float far = Float.POSITIVE_INFINITY;
        int normalX = 0, normalY = 0, normalZ = 0;

        for (int axis = 0; axis < 3; axis++) {
            float component = directionAxis[axis];
            if (Math.abs(component) < EPSILON) {
                if (originAxis[axis] < minimum[axis] || originAxis[axis] > maximum[axis]) return null;
                continue;
            }
            float first = (minimum[axis] - originAxis[axis]) / component;
            float second = (maximum[axis] - originAxis[axis]) / component;
            int axisNormal = component > 0.0f ? -1 : 1;
            if (first > second) {
                float swap = first;
                first = second;
                second = swap;
            }
            if (first > near) {
                near = first;
                normalX = axis == 0 ? axisNormal : 0;
                normalY = axis == 1 ? axisNormal : 0;
                normalZ = axis == 2 ? axisNormal : 0;
            }
            far = Math.min(far, second);
            if (near > far) return null;
        }
        if (far < 0.0f) return null;
        return new RayHit(Math.max(near, 0.0f), normalX, normalY, normalZ);
    }

    /**
     * The Box object which represents the block
     * @param minX {@link Float} supplied as {@code minX}
     * @param minY {@link Float} supplied as {@code minY}
     * @param minZ {@link Float} supplied as {@code minZ}
     * @param maxX {@link Float} supplied as {@code maxX}
     * @param maxY {@link Float} supplied as {@code maxY}
     * @param maxZ {@link Float} supplied as {@code maxZ}
     */
    public record Box(float minX, float minY, float minZ,
                      float maxX, float maxY, float maxZ) { }

    /**
     * The collision ray with an object, raycasting
     * @param distance {@link Float} supplied as {@code distance}
     * @param normalX {@link Integer} supplied as {@code normalX}
     * @param normalY {@link Integer} supplied as {@code normalY}
     * @param normalZ {@link Integer} supplied as {@code normalZ}
     */
    public record RayHit(float distance, int normalX, int normalY, int normalZ) { }
}
