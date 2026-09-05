package com.isofarm.data;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Geometry and collision bounds for voxel blocks that are not full cubes. */
public enum BlockShape {
    FULL_CUBE(new Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)),
    HORIZONTAL_SLAB(new Box(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f)),
    VERTICAL_SLAB_WEST(new Box(0.0f, 0.0f, 0.0f, 0.5f, 1.0f, 1.0f)),
    VERTICAL_SLAB_EAST(new Box(0.5f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)),
    VERTICAL_SLAB_NORTH(new Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.5f)),
    VERTICAL_SLAB_SOUTH(new Box(0.0f, 0.0f, 0.5f, 1.0f, 1.0f, 1.0f)),
    FENCE_NONE(0),
    FENCE_N(1), FENCE_S(2), FENCE_NS(3),
    FENCE_W(4), FENCE_NW(5), FENCE_SW(6), FENCE_NSW(7),
    FENCE_E(8), FENCE_NE(9), FENCE_SE(10), FENCE_NSE(11),
    FENCE_WE(12), FENCE_NWE(13), FENCE_SWE(14), FENCE_NSWE(15);

    private static final float EPSILON = 0.00001f;
    private final Box[] boxes;

    BlockShape(Box... boxes) {
        this.boxes = boxes;
    }

    BlockShape(int fenceMask) {
        this.boxes = createFenceBoxes(fenceMask);
    }

    private static Box[] createFenceBoxes(int mask) {
        List<Box> result = new ArrayList<>();
        result.add(new Box(0.375f, 0.0f, 0.375f, 0.625f, 1.0f, 0.625f));
        if ((mask & 1) != 0) addFenceRails(result, 0.4375f, 0.0f, 0.5625f, 0.5f);
        if ((mask & 2) != 0) addFenceRails(result, 0.4375f, 0.5f, 0.5625f, 1.0f);
        if ((mask & 4) != 0) addFenceRails(result, 0.0f, 0.4375f, 0.5f, 0.5625f);
        if ((mask & 8) != 0) addFenceRails(result, 0.5f, 0.4375f, 1.0f, 0.5625f);
        return result.toArray(new Box[0]);
    }

    private static void addFenceRails(List<Box> boxes, float minX, float minZ,
                                      float maxX, float maxZ) {
        boxes.add(new Box(minX, 0.375f, minZ, maxX, 0.5625f, maxZ));
        boxes.add(new Box(minX, 0.6875f, minZ, maxX, 0.875f, maxZ));
    }

    public Box[] getBoxes() {
        return boxes.clone();
    }

    public int getBoxCount() {
        return boxes.length;
    }

    public boolean isFullCube() {
        return this == FULL_CUBE;
    }

    public boolean isVerticalSlab() {
        return this == VERTICAL_SLAB_WEST || this == VERTICAL_SLAB_EAST
                || this == VERTICAL_SLAB_NORTH || this == VERTICAL_SLAB_SOUTH;
    }

    public boolean isFence() {
        return ordinal() >= FENCE_NONE.ordinal();
    }

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

    /** Chooses the half-cell closest to the supplied world-space point. */
    public static BlockShape verticalSlabFacing(float pointX, float pointZ,
                                                int blockX, int blockZ) {
        float offsetX = pointX - (blockX + 0.5f);
        float offsetZ = pointZ - (blockZ + 0.5f);
        if (Math.abs(offsetX) > Math.abs(offsetZ)) {
            return offsetX < 0.0f ? VERTICAL_SLAB_WEST : VERTICAL_SLAB_EAST;
        }
        return offsetZ < 0.0f ? VERTICAL_SLAB_NORTH : VERTICAL_SLAB_SOUTH;
    }

    public float getTop() {
        float top = 0.0f;
        for (Box box : boxes) top = Math.max(top, box.maxY());
        return top;
    }

    /** Returns the top occupied height at a local horizontal point, or zero. */
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

    public record Box(float minX, float minY, float minZ,
                      float maxX, float maxY, float maxZ) { }

    public record RayHit(float distance, int normalX, int normalY, int normalZ) { }
}
