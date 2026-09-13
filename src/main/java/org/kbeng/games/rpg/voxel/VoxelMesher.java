package org.kbeng.games.rpg.voxel;

import java.nio.*;
import java.util.*;

/** Builds visible surfaces from column intervals instead of iterating buried
 * voxels. Coplanar adjacent rectangles with identical material/normal are merged
 * in two directions. A mesh has one RGB colour per material and no UV buffer. */
public final class VoxelMesher {
    private VoxelMesher() {}
    public record Data(byte[] vertices, int[] indices) {
        public int quads() { return indices.length / 6; }
    }
    public record Result(Data solid, Data fluid) {}
    private record Face(int axis, int sign, int plane, int material, int u, int v, int w, int h) {}
    public static Result build(VoxelWorld world, VoxelChunk chunk) {
        List<Face> faces = new ArrayList<>();
        for (int z = 0; z < 64; z++) for (int x = 0; x < 64; x++) {
            VoxelColumn col = chunk.column(x, z);
            int wx = chunk.x * 64 + x, wz = chunk.z * 64 + z;
            for (int r = 0; r < col.runs(); r++) {
                byte id = col.material(r);
                if (id == 0) continue;
                int lo = col.start(r), hi = col.end(r);
                if (visible(id, col.get(lo - 1))) faces.add(new Face(1, -1, lo, id & 255, z, x, 1, 1));
                if (visible(id, col.get(hi))) faces.add(new Face(1, 1, hi, id & 255, z, x, 1, 1));
                side(faces, world.column(wx - 1, wz), 0, -1, x, z, lo, hi, id);
                side(faces, world.column(wx + 1, wz), 0, 1, x + 1, z, lo, hi, id);
                side(faces, world.column(wx, wz - 1), 2, -1, z, x, lo, hi, id);
                side(faces, world.column(wx, wz + 1), 2, 1, z + 1, x, lo, hi, id);
            }
        }
        List<Face> merged = merge(merge(faces, false), true);
        return new Result(pack(merged, false), pack(merged, true));
    }
    private static boolean visible(byte self, byte other) {
        return other == 0 || (VoxelPalette.solid(self) && VoxelPalette.fluid(other))
                || (VoxelPalette.fluid(self) && VoxelPalette.fluid(other) && self != other);
    }
    private static void side(List<Face> faces, VoxelColumn neighbour, int axis, int sign,
                             int plane, int horizontal, int lo, int hi, byte id) {
        for (int r = 0; r < neighbour.runs(); r++) {
            int from = Math.max(lo, neighbour.start(r)), to = Math.min(hi, neighbour.end(r));
            if (from >= to || !visible(id, neighbour.material(r))) continue;
            faces.add(axis == 0 ? new Face(axis, sign, plane, id & 255, from, horizontal, to - from, 1)
                    : new Face(axis, sign, plane, id & 255, horizontal, from, 1, to - from));
        }
    }
    private static List<Face> merge(List<Face> faces, boolean vertical) {
        Comparator<Face> order = Comparator.comparingInt(Face::axis).thenComparingInt(Face::sign)
                .thenComparingInt(Face::plane).thenComparingInt(Face::material);
        order = vertical ? order.thenComparingInt(Face::u).thenComparingInt(Face::w).thenComparingInt(Face::v)
                : order.thenComparingInt(Face::v).thenComparingInt(Face::h).thenComparingInt(Face::u);
        faces.sort(order);
        List<Face> result = new ArrayList<>();
        for (Face f : faces) {
            if (!result.isEmpty()) {
                Face p = result.getLast();
                boolean same = p.axis == f.axis && p.sign == f.sign && p.plane == f.plane && p.material == f.material;
                boolean adjacent = vertical ? p.u == f.u && p.w == f.w && p.v + p.h == f.v
                        : p.v == f.v && p.h == f.h && p.u + p.w == f.u;
                if (same && adjacent) {
                    result.set(result.size() - 1, new Face(p.axis, p.sign, p.plane, p.material, p.u, p.v,
                            vertical ? p.w : p.w + f.w, vertical ? p.h + f.h : p.h));
                    continue;
                }
            }
            result.add(f);
        }
        return result;
    }
    /** Twelve bytes per vertex: unsigned-short local XYZ, signed-byte normal,
     * and unsigned-byte RGB. Indices use 32 bits so complex edited chunks do not overflow. */
    private static Data pack(List<Face> faces, boolean fluid) {
        int count = (int) faces.stream().filter(f -> VoxelPalette.fluid((byte) f.material) == fluid).count();
        ByteBuffer bytes = ByteBuffer.allocate(count * 4 * 12).order(ByteOrder.nativeOrder());
        int[] indices = new int[count * 6];
        int vertex = 0, element = 0;
        for (Face f : faces) {
            if (VoxelPalette.fluid((byte) f.material) != fluid) continue;
            int rgb = VoxelPalette.rgb((byte) f.material);
            int[][] uv = {{f.u, f.v}, {f.u + f.w, f.v}, {f.u + f.w, f.v + f.h}, {f.u, f.v + f.h}};
            for (int[] pair : uv) {
                int[] xyz = new int[3]; xyz[f.axis] = f.plane;
                xyz[(f.axis + 1) % 3] = pair[0]; xyz[(f.axis + 2) % 3] = pair[1];
                for (int coordinate : xyz) bytes.putShort((short) coordinate);
                for (int axis = 0; axis < 3; axis++) bytes.put((byte) (axis == f.axis ? f.sign * 127 : 0));
                bytes.put((byte) (rgb >> 16)).put((byte) (rgb >> 8)).put((byte) rgb);
            }
            for (int index : f.sign > 0 ? new int[]{0, 1, 2, 0, 2, 3} : new int[]{0, 2, 1, 0, 3, 2}) indices[element++] = vertex + index;
            vertex += 4;
        }
        return new Data(bytes.array(), indices);
    }
}
