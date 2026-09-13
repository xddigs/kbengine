package org.kbeng.games.rpg.voxel;

import org.joml.Vector3f;

/** Grid DDA with distances measured in world units and steps measured in voxels.
 * No block shapes, UVs, whole-unit rounding or breaking-fragment state participate. */
public final class VoxelRaycast {
    private VoxelRaycast() {}
    public record Hit(int x, int y, int z, int nx, int ny, int nz, float distance, byte material) {
        public Vector3f minimum() { return new Vector3f(VoxelGrid.world(x), VoxelGrid.world(y), VoxelGrid.world(z)); }
        public Vector3f center() { return minimum().add(0.125f, 0.125f, 0.125f); }
    }
    /** Returns the first occupied cell, even when it is outside interaction reach.
     * Keeping occlusion separate from reach prevents selecting through a far wall. */
    public static Hit cast(VoxelWorld world, Vector3f origin, Vector3f direction, float maxDistance, boolean fluids) {
        if (!origin.isFinite() || !direction.isFinite() || direction.lengthSquared() < 1e-12f
                || !Float.isFinite(maxDistance) || maxDistance < 0) return null;
        Vector3f d = new Vector3f(direction).normalize();
        int[] c = {VoxelGrid.cell(origin.x), VoxelGrid.cell(origin.y), VoxelGrid.cell(origin.z)};
        int[] s = new int[3]; float[] next = new float[3], stride = new float[3];
        for (int a = 0; a < 3; a++) {
            float value = d.get(a);
            s[a] = value > 0 ? 1 : value < 0 ? -1 : 0;
            stride[a] = s[a] == 0 ? Float.POSITIVE_INFINITY : VoxelGrid.SIZE / Math.abs(value);
            next[a] = s[a] == 0 ? Float.POSITIVE_INFINITY
                    : (VoxelGrid.world(c[a] + (s[a] > 0 ? 1 : 0)) - origin.get(a)) / value;
        }
        float t = 0; int nx = 0, ny = 0, nz = 0;
        while (t <= maxDistance) {
            byte id = world.get(c[0], c[1], c[2]);
            if (id != 0 && (fluids || !VoxelPalette.fluid(id))) return new Hit(c[0], c[1], c[2], nx, ny, nz, t, id);
            int axis = next[0] <= next[1] && next[0] <= next[2] ? 0 : next[1] <= next[2] ? 1 : 2;
            t = next[axis]; if (!Float.isFinite(t)) break;
            for (int a = 0; a < 3; a++) if (next[a] == t) {
                c[a] += s[a]; next[a] += stride[a];
            }
            nx = axis == 0 ? -s[axis] : 0; ny = axis == 1 ? -s[axis] : 0; nz = axis == 2 ? -s[axis] : 0;
        }
        return null;
    }
}
