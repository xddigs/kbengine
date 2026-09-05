package com.isofarm.data;

import org.joml.Vector3f;

/** Geometry and collision bounds for voxel blocks that are not full cubes. */
public enum BlockShape {
    FULL_CUBE(new Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)),
    HORIZONTAL_SLAB(new Box(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f)),
    VERTICAL_SLAB(new Box(0.25f, 0.0f, 0.0f, 0.75f, 1.0f, 1.0f)),
    FENCE(new Box(0.375f, 0.0f, 0.375f, 0.625f, 1.0f, 0.625f),
            new Box(0.0f, 0.375f, 0.4375f, 1.0f, 0.5625f, 0.5625f),
            new Box(0.0f, 0.6875f, 0.4375f, 1.0f, 0.875f, 0.5625f),
            new Box(0.4375f, 0.375f, 0.0f, 0.5625f, 0.5625f, 1.0f),
            new Box(0.4375f, 0.6875f, 0.0f, 0.5625f, 0.875f, 1.0f));

    private static final float EPSILON = 0.00001f;
    private final Box[] boxes;

    BlockShape(Box... boxes) {
        this.boxes = boxes;
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
