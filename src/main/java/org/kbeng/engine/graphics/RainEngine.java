package org.kbeng.engine.graphics;

import org.kbeng.engine.utils.K;
import org.kbeng.engine.utils.Settings;
import org.kbeng.rpg.wrld.Chunk;
import org.kbeng.rpg.wrld.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * RainEngine provides rain engine capabilities within the graphics subsystem.
 * It contributes to OpenGL resource ownership, render-state setup, or frame-pipeline execution.
 * The engine encapsulates a dedicated execution pipeline with its own stateful update lifecycle.
 */
@SuppressWarnings("all")
public class RainEngine {
    private static final int DROPS_PER_CHUNK = 400;
    private static final int VERTICES_PER_DROP = 2;

    private final int vao;
    private final int vbo;
    private float timeAccumulator = 0.0f;

    /**
     * Creates a new {@code RainEngine} instance.
     */
    public RainEngine() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        float[] staticPattern = generateChunkRainPattern();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(staticPattern.length);
        buffer.put(staticPattern).flip();

        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    /**
     * Creates chunk rain pattern from the supplied state and configuration.
     * @return an array of {@code float} values; the generate chunk rain pattern result
     */
    private float[] generateChunkRainPattern() {
        Random rnd = new Random(1337);
        float[] data = new float[DROPS_PER_CHUNK * VERTICES_PER_DROP * 3];
        int idx = 0;

        for (int i = 0; i < DROPS_PER_CHUNK; i++) {
            float rx = rnd.nextFloat() * Chunk.SIZE_X;
            float rz = rnd.nextFloat() * Chunk.SIZE_Z;
            float ry = rnd.nextFloat() * 25.0f;
            float length = 0.48f + rnd.nextFloat() * 1.5f;

            data[idx++] = rx;
            data[idx++] = ry;
            data[idx++] = rz;

            data[idx++] = rx;
            data[idx++] = ry - length;
            data[idx++] = rz;
        }
        return data;
    }

    /**
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void update(float delta) {
        timeAccumulator += delta;
    }

    /**
     * Renders this object in the requested render pass.
     * @param shader the {@link Shader} supplied as {@code shader}
     * @param view the {@link Matrix4f} supplied as {@code view}
     * @param projection the {@link Matrix4f} supplied as {@code projection}
     * @param cameraPos the {@link Vector3f} supplied as {@code cameraPos}
     * @param world the {@link World} supplied as {@code world}
     */
    public void render(Shader shader, Matrix4f view, Matrix4f projection,
                       Vector3f cameraPos, World world) {
        shader.bind();
        shader.setUniform("uView", view);
        shader.setUniform("uProjection", projection);
        shader.setUniform("uRainColor", K.Colors.RAIN);
        shader.setUniform("uTime", timeAccumulator);

        glBindVertexArray(vao);
        glLineWidth(2.0f);

        glEnable(GL_DEPTH_TEST);
        glDepthMask(false);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        int centerChunkX = (int) Math.floor(cameraPos.x / Chunk.SIZE_X);
        int centerChunkZ = (int) Math.floor(cameraPos.z / Chunk.SIZE_Z);
        int dist = Math.min(Settings.getRenderDistance(), 2);

        for (int cx = centerChunkX - dist; cx <= centerChunkX + dist; cx++) {
            for (int cz = centerChunkZ - dist; cz <= centerChunkZ + dist; cz++) {
                int worldX = cx * Chunk.SIZE_X + (Chunk.SIZE_X / 2);
                int worldZ = cz * Chunk.SIZE_Z + (Chunk.SIZE_Z / 2);

                float groundY = world.getHighestY(worldX, worldZ).y();
                float spawnY = Math.max(cameraPos.y + 5.0f, groundY + 10.0f);

                shader.setUniform("uChunkPos", new Vector3f(cx * Chunk.SIZE_X, spawnY, cz * Chunk.SIZE_Z));
                shader.setUniform("uGroundY", groundY);

                glDrawArrays(GL_LINES, 0, DROPS_PER_CHUNK * VERTICES_PER_DROP);
            }
        }

        glDepthMask(true);
        glDisable(GL_BLEND);
        glLineWidth(1.0f);
        glBindVertexArray(0);
        shader.unbind();
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
