package org.kbeng.games.rpg.graphics;

import org.kbeng.games.rpg.voxel.VoxelMesher;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import static org.lwjgl.opengl.GL33.*;

/** RPG-only packed surface mesh: one interleaved 12-byte vertex buffer and one
 * index buffer. Uses the existing engine Shader abstraction without changing
 * engine Mesh's textured vertex format or allocating one object per voxel. */
public final class VoxelMesh implements AutoCloseable {
    private final int vao, vbo, ebo, count;
    public VoxelMesh(VoxelMesher.Data data) {
        count = data.indices().length;
        vao = glGenVertexArrays(); vbo = glGenBuffers(); ebo = glGenBuffers();
        glBindVertexArray(vao);
        ByteBuffer vertices = MemoryUtil.memAlloc(data.vertices().length);
        try {
            vertices.put(data.vertices()).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_UNSIGNED_SHORT, false, 12, 0);
            glVertexAttribPointer(1, 3, GL_BYTE, true, 12, 6);
            glVertexAttribPointer(2, 3, GL_UNSIGNED_BYTE, true, 12, 9);
            for (int i = 0; i < 3; i++) glEnableVertexAttribArray(i);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, data.indices(), GL_STATIC_DRAW);
        } finally { MemoryUtil.memFree(vertices); glBindVertexArray(0); }
    }
    public void render() { glBindVertexArray(vao); glDrawElements(GL_TRIANGLES, count, GL_UNSIGNED_INT, 0); glBindVertexArray(0); }
    @Override public void close() { glDeleteBuffers(vbo); glDeleteBuffers(ebo); glDeleteVertexArrays(vao); }
}
