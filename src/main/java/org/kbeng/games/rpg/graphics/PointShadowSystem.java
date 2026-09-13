package org.kbeng.games.rpg.graphics;

import org.kbeng.engine.graphics.*;

import org.kbeng.engine.utils.Settings;
import org.kbeng.games.rpg.wrld.Chunk;
import org.kbeng.games.rpg.wrld.GameMaster;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

/**
 * PointShadowSystem provides point shadow system capabilities within the graphics subsystem.
 * It contributes to OpenGL resource ownership, render-state setup, or frame-pipeline execution.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public final class PointShadowSystem {
    public static final PointShadowSystem sys = new PointShadowSystem();
    public static final int MAX_SHADOWED_LIGHTS = 4;
    private static final float FAR_PLANE = 8.0f;
    private static final Vector3f[] DIRECTIONS = {
            new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(0, 1, 0),
            new Vector3f(0, -1, 0), new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)};
    private static final Vector3f[] UPS = {
            new Vector3f(0, -1, 0), new Vector3f(0, -1, 0), new Vector3f(0, 0, 1),
            new Vector3f(0, 0, -1), new Vector3f(0, -1, 0), new Vector3f(0, -1, 0)};
    private final PointShadowMap[] maps = new PointShadowMap[MAX_SHADOWED_LIGHTS];
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f lightSpace = new Matrix4f();
    private final Matrix4f model = new Matrix4f();
    private int activeCount;

    private PointShadowSystem() {}

    /**
     * Renders this object in the requested render pass.
     * @param game the {@link GameMaster} supplied as {@code game}
     * @param chunkMeshes the {@link Map} supplied as {@code chunkMeshes}
     * @param lights the {@link List} supplied as {@code lights}
     */
    public void render(GameMaster game,
                       Map<Chunk, ChunkMeshBuilder.ChunkRenderMesh> chunkMeshes,
                       List<Vector3f> lights) {
        if (!Settings.doEnableShadows()) {
            activeCount = 0;
            return;
        }
        int count = Math.min(lights.size(), MAX_SHADOWED_LIGHTS);
        activeCount = count;
        if (count == 0) return;
        Shader shader = ResourceManager.rem.getPointShadowShader();
        projection.identity().perspective((float) Math.toRadians(90.0), 1.0f, 0.1f, FAR_PLANE);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        shader.bind();
        shader.setUniform("uFarPlane", FAR_PLANE);
        for (int index = 0; index < count; index++) {
            if (maps[index] == null) maps[index] = new PointShadowMap();
            Vector3f light = lights.get(index);
            shader.setUniform("uLightPosition", light);
            for (int face = 0; face < 6; face++) {
                maps[index].bindFace(face);
                lightSpace.set(projection).mul(new Matrix4f().lookAt(light,
                        new Vector3f(light).add(DIRECTIONS[face]), UPS[face]));
                shader.setUniform("uLightSpaceMatrix", lightSpace);
                for (var entry : chunkMeshes.entrySet()) {
                    Chunk chunk = entry.getKey();
                    var mesh = entry.getValue();
                    if (mesh == null || mesh.solidMesh() == null || mesh.solidMesh().getIndicesCount() == 0) continue;
                    float x = chunk.getChunkX() * Chunk.SIZE_X;
                    float z = chunk.getChunkZ() * Chunk.SIZE_Z;
                    if (Math.abs(x + Chunk.SIZE_X * 0.5f - light.x) > FAR_PLANE + Chunk.SIZE_X
                            || Math.abs(z + Chunk.SIZE_Z * 0.5f - light.z) > FAR_PLANE + Chunk.SIZE_Z) continue;
                    model.identity().translate(x, 0, z);
                    shader.setUniform("uModel", model);
                    mesh.solidMesh().render();
                }
            }
        }
        shader.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, (int) game.getWindowWidth(), (int) game.getWindowHeight());
    }

    /**
     * Binds this object to the active runtime context.
     * @param shader the {@link Shader} supplied as {@code shader}
     * @param textureUnit the {@code int} supplied as {@code textureUnit}
     */
    public void bind(Shader shader, int textureUnit) {
        shader.setUniform("uShadowedTorchCount", activeCount);
        shader.setUniform("uTorchShadowFarPlane", FAR_PLANE);
        for (int index = 0; index < MAX_SHADOWED_LIGHTS; index++) {
            glActiveTexture(GL_TEXTURE0 + textureUnit + index);
            glBindTexture(GL_TEXTURE_CUBE_MAP, maps[index] == null ? 0 : maps[index].getDepthTexture());
            shader.setUniform("uTorchShadowMaps[" + index + "]", textureUnit + index);
        }
    }

    /** Releases the point-light depth cubemaps. */
    public void dispose() {
        for (int index = 0; index < maps.length; index++) {
            if (maps[index] != null) {
                maps[index].dispose();
                maps[index] = null;
            }
        }
        activeCount = 0;
    }
}
