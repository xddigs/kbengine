package org.kbeng.games.rpg.graphics;

import org.kbeng.engine.graphics.CameraView;
import org.kbeng.engine.graphics.Shader;
import org.kbeng.engine.utils.Settings;
import org.kbeng.games.rpg.voxel.*;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;

/** Owns RPG voxel mesh lifetime. Generation and uploads have a per-frame budget;
 * edits are flushed before drawing, so removal cannot display the old voxel for
 * several frames. Eviction releases GPU buffers but VoxelWorld retains edits. */
public final class VoxelTerrain implements AutoCloseable {
    private final VoxelWorld world;
    private final Map<Long, MeshPair> meshes = new HashMap<>();
    private final ArrayDeque<Long> pending = new ArrayDeque<>();
    private int centerX = Integer.MIN_VALUE, centerZ = Integer.MIN_VALUE, radius = -1;
    public VoxelTerrain(VoxelWorld world) { this.world = world; }
    public void build(int x, int z) {
        VoxelChunk chunk = world.chunk(x, z);
        if (chunk == null) return;
        VoxelMesher.Result data = VoxelMesher.build(world, chunk);
        MeshPair next = new MeshPair(new VoxelMesh(data.solid()), new VoxelMesh(data.fluid()));
        MeshPair old = meshes.put(VoxelGrid.key(x, z), next);
        if (old != null) old.close();
        world.meshed(x, z);
    }
    public void request(int x, int z) {
        int r = Settings.getRenderDistance();
        if (world.chunk(x, z) == null) world.generate(x, z);
        if (centerX == x && centerZ == z && radius == r) return;
        centerX = x; centerZ = z; radius = r;
        List<Long> needed = new ArrayList<>();
        for (int dz = -r; dz <= r; dz++) for (int dx = -r; dx <= r; dx++) {
            if (dx * dx + dz * dz > r * r) continue;
            if (world.chunk(x + dx, z + dz) == null) needed.add(VoxelGrid.key(x + dx, z + dz));
        }
        needed.sort(Comparator.comparingDouble(k -> distance(k, x * 16f, z * 16f)));
        pending.clear(); pending.addAll(needed);
        int unload = r + Settings.getUnloadMargin();
        for (VoxelChunk chunk : List.copyOf(world.chunks())) {
            if (Math.abs(chunk.x - x) <= unload && Math.abs(chunk.z - z) <= unload) continue;
            world.unload(chunk.x, chunk.z);
            MeshPair old = meshes.remove(VoxelGrid.key(chunk.x, chunk.z));
            if (old != null) old.close();
        }
    }
    public void update(float x, float z) {
        request((int) Math.floor(x / 16f), (int) Math.floor(z / 16f));
        for (int i = 0; i < 2 && !pending.isEmpty(); i++) {
            long k = pending.removeFirst(); world.generate((int) (k >> 32), (int) k);
        }
        flush();
    }
    public void flush() {
        for (long key : world.takeDirty()) build((int) (key >> 32), (int) key);
    }
    /** Draws opaque surfaces front-to-back and fluid chunks back-to-front;
     * all local voxel coordinates are converted by the RPG shader to world units. */
    public void render(Shader shader, CameraView camera, boolean fluids) {
        FrustumIntersection frustum = new FrustumIntersection(new Matrix4f(camera.getProjectionMatrix()).mul(camera.getViewMatrix()));
        List<Long> keys = new ArrayList<>(meshes.keySet());
        Comparator<Long> order = Comparator.comparingDouble(k -> distance(k, camera.getPosition().x, camera.getPosition().z));
        keys.sort(fluids ? order.reversed() : order);
        for (long key : keys) {
            float x = (int) (key >> 32) * 16f, z = (int) key * 16f;
            if (!frustum.testAab(x, 0, z, x + 16, 256, z + 16)) continue;
            shader.setUniform("uOrigin", new Vector3f(x, 0, z));
            MeshPair pair = meshes.get(key);
            (fluids ? pair.fluid : pair.solid).render();
        }
    }
    private static double distance(long k, float x, float z) {
        double dx = (int) (k >> 32) * 16.0 + 8 - x, dz = (int) k * 16.0 + 8 - z;
        return dx * dx + dz * dz;
    }
    /** Culls against the light frustum, not the viewer's frustum: terrain just
     * outside the screen can still cast a shadow onto visible actors and voxels. */
    public void renderDepth(Shader shader, Matrix4f lightSpace) {
        FrustumIntersection frustum = new FrustumIntersection(lightSpace);
        for (var entry : meshes.entrySet()) {
            long key = entry.getKey();
            float x = (int) (key >> 32) * 16f, z = (int) key * 16f;
            if (!frustum.testAab(x, 0, z, x + 16, 256, z + 16)) continue;
            shader.setUniform("uOrigin", new Vector3f(x, 0, z));
            entry.getValue().solid.render();
        }
    }
    @Override public void close() { meshes.values().forEach(MeshPair::close); meshes.clear(); pending.clear(); }
    private record MeshPair(VoxelMesh solid, VoxelMesh fluid) implements AutoCloseable {
        @Override public void close() { solid.close(); fluid.close(); }
    }
}
