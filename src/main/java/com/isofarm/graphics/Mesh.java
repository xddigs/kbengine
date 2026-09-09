package com.isofarm.graphics;

import com.isofarm.data.BlockShape;
import com.isofarm.graphics.gltf.GLTFModel;
import com.isofarm.item.Block;
import com.isofarm.utils.K;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Encapsulates the state and operations required by mesh within the game runtime.
 */
public class Mesh {
    private static final Logger log = LoggerFactory.getLogger(Mesh.class);
    private final int vaoId;
    private final int posVboId;
    private final int normalVboId;
    private final int uvVboId;
    private final int eboId;
    private final int vertexCount;

    /**
     * Creates a new {@code Mesh} instance.
     * @param positions an array of {@code float} values supplied as {@code positions}
     * @param normals an array of {@code float} values supplied as {@code normals}
     * @param textCoords an array of {@code float} values supplied as {@code textCoords}
     * @param indices an array of {@code int} values supplied as {@code indices}
     */
    public Mesh(float[] positions, float[] normals, float[] textCoords, int[] indices) {
        this.vertexCount = indices.length;

        FloatBuffer posBuffer = null;
        FloatBuffer normalBuffer = null;
        FloatBuffer texBuffer = null;
        IntBuffer idxBuffer = null;

        try {
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            posBuffer = MemoryUtil.memAllocFloat(positions.length);
            posBuffer.put(positions).flip();
            posVboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, posVboId);
            glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(0);

            normalBuffer = MemoryUtil.memAllocFloat(normals.length);
            normalBuffer.put(normals).flip();
            normalVboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, normalVboId);
            glBufferData(GL_ARRAY_BUFFER, normalBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(1);

            texBuffer = MemoryUtil.memAllocFloat(textCoords.length);
            texBuffer.put(textCoords).flip();
            uvVboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, uvVboId);
            glBufferData(GL_ARRAY_BUFFER, texBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(2);

            idxBuffer = MemoryUtil.memAllocInt(indices.length);
            idxBuffer.put(indices).flip();
            eboId = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxBuffer, GL_STATIC_DRAW);

            glBindVertexArray(0);

        } finally {
            if (posBuffer != null) MemoryUtil.memFree(posBuffer);
            if (normalBuffer != null) MemoryUtil.memFree(normalBuffer);
            if (texBuffer != null) MemoryUtil.memFree(texBuffer);
            if (idxBuffer != null) MemoryUtil.memFree(idxBuffer);
        }

        log.trace("Mesh created successfully [VAO ID: {}, Vertices: {}]", vaoId, vertexCount);
    }

    /**
     * Returns the indices count.
     * @return {@code int}; the indices count
     */
    public int getIndicesCount() { return vertexCount; }

    /**
     * Renders this object in the requested render pass.
     */
    public void render() {
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    /**
     * Renders the lines.
     */
    public void renderLines() {
        glBindVertexArray(vaoId);
        glLineWidth(K.Render.LINE_WIDTH);
        glDrawElements(GL_LINES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(posVboId);
        glDeleteBuffers(normalVboId);
        glDeleteBuffers(uvVboId);
        glDeleteBuffers(eboId);

        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);

        log.trace("Mesh resources cleaned up [VAO ID: {}]", vaoId);
    }

    /**
     * Creates and returns the mesh.
     * @param depth the {@code float} supplied as {@code depth}
     * @return the {@link Mesh} representing the created mesh
     */
    public static Mesh createMesh(float depth) {
        float[] positions = getFloats(depth);
        float[] normals = new float[]{0,1,0, 0,1,0, 0,1,0, 0,1,0, 0,0,1, 0,0,1, 0,0,1, 0,0,1, 1,0,0, 1,0,0, 1,0,0, 1,0,0, 0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1, -1,0,0, -1,0,0, -1,0,0, -1,0,0, 0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0};
        float[] textCoords = new float[]{0,0, 0,1, 1,1, 1,0, 0,0, 0,1, 1,1, 1,0, 0,0, 0,1, 1,1, 1,0, 0,0, 0,1, 1,1, 1,0, 0,0, 0,1, 1,1, 1,0, 0,0, 0,1, 1,1, 1,0};
        int[] indices = new int[36];
        for (int i = 0; i < 6; i++) { int v = i * 4; int idx = i * 6; indices[idx] = v; indices[idx + 1] = v + 1; indices[idx + 2] = v + 3; indices[idx + 3] = v + 3; indices[idx + 4] = v + 1; indices[idx + 5] = v + 2; }
        return new Mesh(positions, normals, textCoords, indices);
    }

    /**
     * Creates and returns the mesh.
     * @param block the {@link Block} supplied as {@code block}
     * @param top the {@link TextureAtlas.TextureRegion} supplied as {@code top}
     * @param bottom the {@link TextureAtlas.TextureRegion} supplied as {@code bottom}
     * @param side the {@link TextureAtlas.TextureRegion} supplied as {@code side}
     * @return the {@link Mesh} representing the created mesh
     */
    public static Mesh createVoxelBlockMesh(Block block,
                                            TextureAtlas.TextureRegion top,
                                            TextureAtlas.TextureRegion bottom,
                                            TextureAtlas.TextureRegion side,
                                            boolean[] neighborSolid) {
        int subdivisions = 4;
        int maxFaces = subdivisions * subdivisions * subdivisions * 6;
        float[] positions = new float[maxFaces * 12];
        float[] normals = new float[maxFaces * 12];
        float[] uv = new float[maxFaces * 8];
        int[] indices = new int[maxFaces * 6];

        int pos = 0, normal = 0, tex = 0, index = 0, vertex = 0;
        float size = 1.0f / subdivisions;

        for (int x = 0; x < subdivisions; x++) {
            for (int y = 0; y < subdivisions; y++) {
                for (int z = 0; z < subdivisions; z++) {
                    if (!block.isVoxelSolid(x, y, z)) continue;

                    float x0 = x * size, x1 = x0 + size;
                    float y0 = y * size, y1 = y0 + size;
                    float z0 = z * size, z1 = z0 + size;

                    boolean topOccluded = (y < subdivisions - 1) ? block.isVoxelSolid(x, y + 1, z) : neighborSolid[0];
                    if (!topOccluded) {
                        int[] next = addVoxelFace(positions, normals, uv, indices, pos, normal, tex, index, vertex, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, 0,1,0, top,bottom,side);
                        pos=next[0]; normal=next[1]; tex=next[2]; index=next[3]; vertex=next[4];
                    }

                    boolean bottomOccluded = (y > 0) ? block.isVoxelSolid(x, y - 1, z) : neighborSolid[1];
                    if (!bottomOccluded) {
                        int[] next = addVoxelFace(positions, normals, uv, indices, pos, normal, tex, index, vertex, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, 0,-1,0, top,bottom,side);
                        pos=next[0]; normal=next[1]; tex=next[2]; index=next[3]; vertex=next[4];
                    }

                    boolean northOccluded = (z < subdivisions - 1) ? block.isVoxelSolid(x, y, z + 1) : neighborSolid[2];
                    if (!northOccluded) {
                        int[] next = addVoxelFace(positions, normals, uv, indices, pos, normal, tex, index, vertex, x1,y0,z1, x1,y1,z1, x0,y1,z1, x0,y0,z1, 0,0,1, top,bottom,side);
                        pos=next[0]; normal=next[1]; tex=next[2]; index=next[3]; vertex=next[4];
                    }

                    boolean southOccluded = (z > 0) ? block.isVoxelSolid(x, y, z - 1) : neighborSolid[3];
                    if (!southOccluded) {
                        int[] next = addVoxelFace(positions, normals, uv, indices, pos, normal, tex, index, vertex, x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0, 0,0,-1, top,bottom,side);
                        pos=next[0]; normal=next[1]; tex=next[2]; index=next[3]; vertex=next[4];
                    }

                    boolean eastOccluded = (x < subdivisions - 1) ? block.isVoxelSolid(x + 1, y, z) : neighborSolid[4];
                    if (!eastOccluded) {
                        int[] next = addVoxelFace(positions, normals, uv, indices, pos, normal, tex, index, vertex, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, 1,0,0, top,bottom,side);
                        pos=next[0]; normal=next[1]; tex=next[2]; index=next[3]; vertex=next[4];
                    }

                    boolean westOccluded = (x > 0) ? block.isVoxelSolid(x - 1, y, z) : neighborSolid[5];
                    if (!westOccluded) {
                        int[] next = addVoxelFace(positions, normals, uv, indices, pos, normal, tex, index, vertex, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, -1,0,0, top,bottom,side);
                        pos=next[0]; normal=next[1]; tex=next[2]; index=next[3]; vertex=next[4];
                    }
                }
            }
        }

        return new Mesh(Arrays.copyOf(positions, pos), Arrays.copyOf(normals, normal),
                Arrays.copyOf(uv, tex), Arrays.copyOf(indices, index));
    }

    /**
     * Creates and returns the cross mesh.
     * @return the {@link Mesh} representing the created cross mesh
     */
    public static Mesh createCrossMesh() {
        float[] positions = new float[]{-0.5f, 0.0f, -0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 1.0f, 0.5f, -0.5f, 1.0f, -0.5f, 0.5f, 0.0f, -0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 1.0f, 0.5f, 0.5f, 1.0f, -0.5f};
        float[] normals = new float[]{-0.707f, 0.0f, 0.707f, -0.707f, 0.0f, 0.707f, -0.707f, 0.0f, 0.707f, -0.707f, 0.0f, 0.707f, 0.707f, 0.0f, 0.707f, 0.707f, 0.0f, 0.707f, 0.707f, 0.0f, 0.707f, 0.707f, 0.0f, 0.707f};
        float[] texCoords = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f};
        int[] indices = new int[]{0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4};
        return new Mesh(positions, normals, texCoords, indices);
    }

    /**
     * Creates and returns the centered quad.
     * @return the {@link Mesh} representing the created centered quad
     */
    public static Mesh createCenteredQuad() {
        float[] positions = new float[]{-0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f, 0.5f, 0.5f, 0.0f, -0.5f, 0.5f, 0.0f};
        float[] normals = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f};
        float[] texCoords = new float[]{0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};
        int[] indices = new int[]{0, 1, 2, 2, 3, 0};
        return new Mesh(positions, normals, texCoords, indices);
    }

    /**
     * Creates and returns the quad.
     * @return the {@link Mesh} representing the created quad
     */
    public static Mesh createQuad() {
        float[] positions = new float[]{0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f};
        float[] normals = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f};
        float[] textCoords = new float[]{0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f};
        int[] indices = new int[]{0, 1, 2, 2, 3, 0};
        return new Mesh(positions, normals, textCoords, indices);
    }

    /**
     * Creates or returns screen quad from the supplied arguments.
     * @return the {@link Mesh} representing the screen quad result
     */
    public static Mesh screenQuad() {
        float[] positions = new float[]{-1.0f, 1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 0.0f};
        float[] normals = new float[12];
        float[] texCoords = new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f};
        int[] indices = new int[]{0, 1, 2, 2, 3, 0};
        return new Mesh(positions, normals, texCoords, indices);
    }

    /**
     * Returns the floats.
     * @param depth the {@code float} supplied as {@code depth}
     * @return an array of {@code float} values; the floats
     */
    private static float[] getFloats(float depth) {
        float height = 1.0f;
        return new float[]{
                -0.5f, 0.0f, -0.5f, -0.5f, 0.0f, 0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 0.0f, -0.5f,
                -0.5f, 0.0f, 0.5f, -0.5f, -height, 0.5f, 0.5f, -height, 0.5f, 0.5f, 0.0f, 0.5f,
                0.5f, 0.0f, 0.5f, 0.5f, -height, 0.5f, 0.5f, -height, -0.5f, 0.5f, 0.0f, -0.5f,
                0.5f, 0.0f, -0.5f, 0.5f, -height, -0.5f, -0.5f, -height, -0.5f, -0.5f, 0.0f, -0.5f,
                -0.5f, 0.0f, -0.5f, -0.5f, -height, -0.5f, -0.5f, -height, 0.5f, -0.5f, 0.0f, 0.5f,
                -0.5f, -height, 0.5f, -0.5f, -height, -0.5f, 0.5f, -height, -0.5f, 0.5f, -height, 0.5f
        };
    }

    /**
     * Creates or returns selection from the supplied arguments.
     * @return the {@link Mesh} representing the selection result
     */
    public static Mesh selection() {
        return selection(BlockShape.FULL_CUBE);
    }

    /**
     * Creates an outline matching every cuboid in a block shape.
     */
    public static Mesh selection(BlockShape shape) {
        float epsilon = 0.002f;
        BlockShape.Box[] boxes = shape.getBoxes();
        float[] positions = new float[boxes.length * 24];
        float[] normals = new float[positions.length];
        float[] textCoords = new float[boxes.length * 16];
        int[] indices = new int[boxes.length * 24];
        int positionIndex = 0;
        int index = 0;
        int vertexOffset = 0;

        for (BlockShape.Box box : boxes) {
            float minX = box.minX() - epsilon;
            float minY = box.minY() - epsilon;
            float minZ = box.minZ() - epsilon;
            float maxX = box.maxX() + epsilon;
            float maxY = box.maxY() + epsilon;
            float maxZ = box.maxZ() + epsilon;
            float[] boxPositions = {
                    minX, maxY, minZ, maxX, maxY, minZ,
                    maxX, maxY, maxZ, minX, maxY, maxZ,
                    minX, minY, minZ, maxX, minY, minZ,
                    maxX, minY, maxZ, minX, minY, maxZ
            };
            System.arraycopy(boxPositions, 0, positions, positionIndex, boxPositions.length);
            positionIndex += boxPositions.length;

            int[] boxIndices = {
                    0, 1, 1, 2, 2, 3, 3, 0,
                    4, 5, 5, 6, 6, 7, 7, 4,
                    0, 4, 1, 5, 2, 6, 3, 7
            };
            for (int boxIndex : boxIndices) indices[index++] = vertexOffset + boxIndex;
            vertexOffset += 8;
        }
        return new Mesh(positions, normals, textCoords, indices);
    }

    /**
     * Creates or returns quad vertical from the supplied arguments.
     * @return the {@link Mesh} representing the quad vertical result
     */
    public static Mesh quadVertical() {
        float[] positions = new float[]{-0.5f, 0.0f, 0.0f, 0.5f, 0.0f, 0.0f, 0.5f, 1.0f, 0.0f, -0.5f, 1.0f, 0.0f};
        float[] normals = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f};
        float[] texCoords = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f};
        int[] indices = new int[]{0, 1, 2, 2, 3, 0};
        return new Mesh(positions, normals, texCoords, indices);
    }

    /**
     * Creates and returns the crop.
     * @return the {@link Mesh} representing the created crop
     */
    public static Mesh createCrop() {
        float[] positions = new float[]{-0.4f, 0.0f, -0.25f, 0.4f, 0.0f, -0.25f, 0.4f, 0.8f, -0.25f, -0.4f, 0.8f, -0.25f, -0.4f, 0.0f, 0.25f, 0.4f, 0.0f, 0.25f, 0.4f, 0.8f, 0.25f, -0.4f, 0.8f, 0.25f, -0.25f, 0.0f, -0.4f, -0.25f, 0.0f, 0.4f, -0.25f, 0.8f, 0.4f, -0.25f, 0.8f, -0.4f, 0.25f, 0.0f, -0.4f, 0.25f, 0.0f, 0.4f, 0.25f, 0.8f, 0.4f, 0.25f, 0.8f, -0.4f};
        float[] normals = new float[]{0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f};
        float[] texCoords = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f};
        int[] indices = new int[]{0, 1, 2, 2, 3, 0, 2, 1, 0, 0, 3, 2, 4, 5, 6, 6, 7, 4, 6, 5, 4, 4, 7, 6, 8, 9, 10, 10, 11, 8, 10, 9, 8, 8, 11, 10, 12, 13, 14, 14, 15, 12, 14, 13, 12, 12, 15, 14};
        return new Mesh(positions, normals, texCoords, indices);
    }

    /**
     * Creates and returns the cube.
     * @return the {@link Mesh} representing the created cube
     */
    public static Mesh createCube() {
        float[] positions = new float[]{-0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f};
        float[] normals = new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f};
        float[] textCoords = new float[]{0,0, 0,1, 1,1, 1,0, 0,0, 0,1, 1,1, 1,0, 0,0, 0,1, 1,1, 1,0, 0,0, 0,1, 1,1, 1,0, 0,0, 0,1, 1,1, 1,0, 0,0, 0,1, 1,1, 1,0};
        int[] indices = new int[36];
        for (int i = 0; i < 6; i++) { int v = i * 4; int idx = i * 6; indices[idx] = v; indices[idx + 1] = v + 1; indices[idx + 2] = v + 3; indices[idx + 3] = v + 3; indices[idx + 4] = v + 1; indices[idx + 5] = v + 2; }
        return new Mesh(positions, normals, textCoords, indices);
    }

    /**
     * Creates the visible faces of a block divided into removable voxels.
     * Faces shared by two surviving voxels are omitted while faces next to a
     * removed voxel or the exterior remain in the mesh.
     * @param subdivisions number of voxels per axis
     * @param removed number of voxels removed in the deterministic break order
     * @param top atlas region used by upward-facing faces
     * @param bottom atlas region used by downward-facing faces
     * @param side atlas region used by lateral faces
     * @return mesh containing the visible voxel faces
     */
    public static Mesh createBreakingVoxelMesh(int subdivisions, int removed,
                                                TextureAtlas.TextureRegion top, TextureAtlas.TextureRegion bottom,
                                                TextureAtlas.TextureRegion side) {
        int maxFaces = subdivisions * subdivisions * subdivisions * 6;
        float[] positions = new float[maxFaces * 12];
        float[] normals = new float[maxFaces * 12];
        float[] uv = new float[maxFaces * 8];
        int[] indices = new int[maxFaces * 6];
        int pos = 0, normal = 0, tex = 0, index = 0, vertex = 0;
        float size = 1.0f / subdivisions;

        for (int x = 0; x < subdivisions; x++) for (int y = 0; y < subdivisions; y++)
            for (int z = 0; z < subdivisions; z++) {
                if (isRemovedVoxel(x, y, z, removed)) continue;
                float x0 = x * size, x1 = x0 + size;
                float y0 = y * size, y1 = y0 + size;
                float z0 = z * size, z1 = z0 + size;
                if (isRemovedVoxel(x, y + 1, z, removed)) { int[] next = addVoxelFace(positions, normals, uv, indices, pos, normal, tex, index, vertex, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, 0,1,0, top,bottom,side); pos=next[0]; normal=next[1]; tex=next[2]; index=next[3]; vertex=next[4]; }
                if (isRemovedVoxel(x, y - 1, z, removed)) { int[] next = addVoxelFace(positions, normals, uv, indices, pos, normal, tex, index, vertex, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, 0,-1,0, top,bottom,side); pos=next[0]; normal=next[1]; tex=next[2]; index=next[3]; vertex=next[4]; }
                if (isRemovedVoxel(x, y, z + 1, removed)) { int[] next = addVoxelFace(positions, normals, uv, indices, pos, normal, tex, index, vertex, x0,y0,z1, x0,y1,z1, x1,y1,z1, x1,y0,z1, 0,0,1, top,bottom,side); pos=next[0]; normal=next[1]; tex=next[2]; index=next[3]; vertex=next[4]; }
                if (isRemovedVoxel(x, y, z - 1, removed)) { int[] next = addVoxelFace(positions, normals, uv, indices, pos, normal, tex, index, vertex, x1,y0,z0, x1,y1,z0, x0,y1,z0, x0,y0,z0, 0,0,-1, top,bottom,side); pos=next[0]; normal=next[1]; tex=next[2]; index=next[3]; vertex=next[4]; }
                if (isRemovedVoxel(x + 1, y, z, removed)) { int[] next = addVoxelFace(positions, normals, uv, indices, pos, normal, tex, index, vertex, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, 1,0,0, top,bottom,side); pos=next[0]; normal=next[1]; tex=next[2]; index=next[3]; vertex=next[4]; }
                if (isRemovedVoxel(x - 1, y, z, removed)) { int[] next = addVoxelFace(positions, normals, uv, indices, pos, normal, tex, index, vertex, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, -1,0,0, top,bottom,side); pos=next[0]; normal=next[1]; tex=next[2]; index=next[3]; vertex=next[4]; }
            }
        return new Mesh(Arrays.copyOf(positions, pos), Arrays.copyOf(normals, normal),
                Arrays.copyOf(uv, tex), Arrays.copyOf(indices, index));
    }

    /**
     * Determines whether a voxel is outside the block or already removed.
     * @param x local voxel x coordinate
     * @param y local voxel y coordinate
     * @param z local voxel z coordinate
     * @param removed removal threshold for the current stage
     * @return whether the voxel must be treated as empty
     */
    private static boolean isRemovedVoxel(int x, int y, int z, int removed) {
        if (x < 0 || y < 0 || z < 0 || x >= 4 || y >= 4 || z >= 4) return true;
        return (((x * 17 + y * 31 + z * 47) ^ (x * y * 13 + z * 7)) & 63) < removed;
    }

    /**
     * Appends a textured quad for one exposed voxel face.
     * @return updated write offsets for positions, normals, UVs, indices and vertices
     */
    private static int[] addVoxelFace(float[] positions, float[] normals, float[] uv, int[] indices,
                                      int pos, int normal, int tex, int index, int vertex,
                                      float x1, float y1, float z1, float x2, float y2, float z2,
                                      float x3, float y3, float z3, float x4, float y4, float z4,
                                      float nx, float ny, float nz,
                                      TextureAtlas.TextureRegion top, TextureAtlas.TextureRegion bottom,
                                      TextureAtlas.TextureRegion side) {
        float[] face = {x1,y1,z1, x2,y2,z2, x3,y3,z3, x4,y4,z4};
        System.arraycopy(face, 0, positions, pos, 12);
        for (int i = 0; i < 4; i++) { normals[normal++] = nx; normals[normal++] = ny; normals[normal++] = nz; }
        float[] local = nx != 0.0f ? new float[]{z1,y1, z2,y2, z3,y3, z4,y4}
                : ny != 0.0f ? new float[]{x1,z1, x2,z2, x3,z3, x4,z4}
                : new float[]{x1,y1, x2,y2, x3,y3, x4,y4};
        TextureAtlas.TextureRegion region = ny > 0.0f ? top : ny < 0.0f ? bottom : side;
        if (region == null) region = side != null ? side : top;
        float[] coords = new float[8];
        for (int i = 0; i < 8; i += 2) {
            coords[i] = region.uvMin().x + local[i] * (region.uvMax().x - region.uvMin().x);
            float v = ny == 0.0f ? 1.0f - local[i + 1] : local[i + 1];
            coords[i + 1] = region.uvMin().y + v * (region.uvMax().y - region.uvMin().y);
        }
        System.arraycopy(coords, 0, uv, tex, 8);
        indices[index++] = vertex; indices[index++] = vertex + 1; indices[index++] = vertex + 3;
        indices[index++] = vertex + 3; indices[index++] = vertex + 1; indices[index++] = vertex + 2;
        return new int[]{pos + 12, normal, tex + 8, index, vertex + 4};
    }
}
