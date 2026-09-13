package org.kbeng.games.rpg.voxel;

import java.util.*;

/** Authoritative quarter-unit terrain. Public integer coordinates are voxel
 * indices, never whole-block coordinates. Modified columns survive unloading
 * for the lifetime of the session; procedural columns can be regenerated. */
public final class VoxelWorld {
    private final Map<Long, VoxelChunk> chunks = new HashMap<>();
    private final Map<Long, Map<Integer, VoxelColumn>> edits = new HashMap<>();
    private final Set<Long> dirty = new LinkedHashSet<>();
    private final VoxelGenerator generator;

    public VoxelWorld(long seed) { generator = new VoxelGenerator(seed); }

    public Collection<VoxelChunk> chunks() { return chunks.values(); }
    public VoxelChunk chunk(int x, int z) { return chunks.get(VoxelGrid.key(x, z)); }

    public void generate(int x, int z) {
        long key = VoxelGrid.key(x, z);
        if (chunks.containsKey(key)) return;
        VoxelChunk chunk = generator.generate(x, z);
        edits.getOrDefault(key, Map.of()).forEach((index, col) -> chunk.column(index % 64, index / 64, col));
        chunks.put(key, chunk);
        dirty.add(key);
        dirty.add(VoxelGrid.key(x - 1, z)); dirty.add(VoxelGrid.key(x + 1, z));
        dirty.add(VoxelGrid.key(x, z - 1)); dirty.add(VoxelGrid.key(x, z + 1));
    }

    public void unload(int x, int z) {
        chunks.remove(VoxelGrid.key(x, z)); dirty.remove(VoxelGrid.key(x, z));
        dirty.add(VoxelGrid.key(x - 1, z)); dirty.add(VoxelGrid.key(x + 1, z));
        dirty.add(VoxelGrid.key(x, z - 1)); dirty.add(VoxelGrid.key(x, z + 1));
    }

    public void meshed(int x, int z) { dirty.remove(VoxelGrid.key(x, z)); }
    public Set<Long> takeDirty() { Set<Long> result = new LinkedHashSet<>(dirty); dirty.clear(); return result; }
    public VoxelColumn column(int x, int z) {
        VoxelChunk c = chunk(Math.floorDiv(x, 64), Math.floorDiv(z, 64));
        return c == null ? VoxelColumn.AIR : c.column(Math.floorMod(x, 64), Math.floorMod(z, 64));
    }

    public byte get(int x, int y, int z) { return column(x, z).get(y); }
    public byte at(float x, float y, float z) { return get(VoxelGrid.cell(x), VoxelGrid.cell(y), VoxelGrid.cell(z)); }

    /** Replaces precisely one loaded cell and dirties all face-sharing chunk
     * neighbours affected by its exposed faces. Returns false for unloaded cells. */
    public boolean set(int x, int y, int z, byte id) {
        if (y < 0 || y >= VoxelGrid.HEIGHT) return false;
        int cx = Math.floorDiv(x, 64), cz = Math.floorDiv(z, 64);
        VoxelChunk chunk = chunk(cx, cz);
        if (chunk == null || get(x, y, z) == id) return false;
        int lx = Math.floorMod(x, 64), lz = Math.floorMod(z, 64);
        VoxelColumn column = chunk.column(lx, lz).with(y, id);
        chunk.column(lx, lz, column);
        long key = VoxelGrid.key(cx, cz);
        edits.computeIfAbsent(key, k -> new HashMap<>()).put(lx + lz * 64, column);
        dirty.add(key);
        if (lx == 0) dirty.add(VoxelGrid.key(cx - 1, cz));
        if (lx == 63) dirty.add(VoxelGrid.key(cx + 1, cz));
        if (lz == 0) dirty.add(VoxelGrid.key(cx, cz - 1));
        if (lz == 63) dirty.add(VoxelGrid.key(cx, cz + 1));
        return true;
    }
    /** World-space surface suitable for spawn, including fractional heights. */
    public float surface(float x, float z) {
        VoxelColumn col = column(VoxelGrid.cell(x), VoxelGrid.cell(z));
        for (int r = col.runs() - 1; r >= 0; r--) if (VoxelPalette.solid(col.material(r))) return VoxelGrid.world(col.end(r));
        return 0;
    }
    /** Exact actor AABB query. The upper bounds are exclusive, so merely
     * touching a cell face does not count as penetration. */
    public boolean intersects(float ax, float ay, float az, float bx, float by, float bz, boolean fluids) {
        for (int x = VoxelGrid.cell(ax); x <= VoxelGrid.cell(Math.nextDown(bx)); x++)
            for (int z = VoxelGrid.cell(az); z <= VoxelGrid.cell(Math.nextDown(bz)); z++) {
                VoxelColumn col = column(x, z);
                for (int r = 0; r < col.runs(); r++) {
                    boolean match = fluids ? VoxelPalette.fluid(col.material(r)) : VoxelPalette.solid(col.material(r));
                    if (match && VoxelGrid.world(col.end(r)) > ay && VoxelGrid.world(col.start(r)) < by) return true;
                }
            }
        return false;
    }

    /** Material-specific contact, used for lava damage without treating water
     * as harmful. Coordinates are physical AABB bounds; upper faces are open. */
    public boolean intersectsMaterial(float ax, float ay, float az, float bx, float by, float bz, byte id) {
        for (int x = VoxelGrid.cell(ax); x <= VoxelGrid.cell(Math.nextDown(bx)); x++)
            for (int z = VoxelGrid.cell(az); z <= VoxelGrid.cell(Math.nextDown(bz)); z++) {
                VoxelColumn col = column(x, z);
                for (int r = 0; r < col.runs(); r++)
                    if (col.material(r) == id && VoxelGrid.world(col.end(r)) > ay
                            && VoxelGrid.world(col.start(r)) < by) return true;
            }
        return false;
    }

    /** Maximum submerged height over the actor footprint, not the sum of all
     * touched cells. Refining the grid therefore does not multiply buoyancy. */
    public float fluidDepth(float ax, float ay, float az, float bx, float by, float bz) {
        float depth = 0;
        for (int x = VoxelGrid.cell(ax); x <= VoxelGrid.cell(Math.nextDown(bx)); x++)
            for (int z = VoxelGrid.cell(az); z <= VoxelGrid.cell(Math.nextDown(bz)); z++) {
                VoxelColumn col = column(x, z);
                float columnDepth = 0;
                for (int r = 0; r < col.runs(); r++) if (VoxelPalette.fluid(col.material(r)))
                    columnDepth += Math.max(0, Math.min(by, VoxelGrid.world(col.end(r)))
                            - Math.max(ay, VoxelGrid.world(col.start(r))));
                depth = Math.max(depth, columnDepth);
            }
        return depth;
    }
}
