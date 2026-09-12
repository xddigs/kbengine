package org.kbeng.graphics;

import org.kbeng.data.BlockData;
import org.kbeng.data.Crop;
import org.kbeng.data.RenderPass;
import org.kbeng.data.Singleton;
import org.kbeng.entity.Entity;
import org.kbeng.entity.WorldItem;
import org.kbeng.utils.K;
import org.kbeng.utils.Settings;
import org.kbeng.wrld.Chunk;
import org.kbeng.wrld.GameMaster;
import org.kbeng.entity.Player;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * ShadowSystem provides shadow system capabilities within the graphics subsystem.
 *
 * It contributes to OpenGL resource ownership, render-state setup, or frame-pipeline execution.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@Singleton
public class ShadowSystem {
    public static final ShadowSystem sys = new ShadowSystem();
    private static final float SHADOW_DISTANCE = 100.0f;
    private static final float SHADOW_SIZE = 70.0f;
    private static final float SHADOW_NEAR = 1.0f;
    private static final float SHADOW_FAR = 220.0f;

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f lightSpace = new Matrix4f();

    private final Vector3f lightPosition = new Vector3f();
    private final Vector3f target = new Vector3f();
    private final Vector3f lightDirection = new Vector3f();
    private final Vector3f lightRight = new Vector3f();
    private final Vector3f lightUp = new Vector3f();
    private final Vector3f snappedTarget = new Vector3f();

    private final Matrix4f modelMatrix = new Matrix4f();

    /**
     * Renders this object in the requested render pass.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param chunkMeshes the {@link Map} supplied as {@code chunkMeshes}
     */
    public void render(GameMaster gameMaster,
                       Map<Chunk, ChunkMeshBuilder.ChunkRenderMesh> chunkMeshes) {
        if (!Settings.doEnableShadows()) return;

        ShadowMap shadowMap = gameMaster.getShadowMap();
        updateLightMatrix(gameMaster, shadowMap);
        shadowMap.bind();

        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        Shader shadowShader = ResourceManager.rem.getShadowMapShader();
        shadowShader.bind();
        shadowShader.setUniform("uLightSpaceMatrix", lightSpace);
        shadowShader.setUniform("uAlphaTest", false);
        Player player = Player.plyr;
        for (Map.Entry<Chunk, ChunkMeshBuilder.ChunkRenderMesh> entry : chunkMeshes.entrySet()) {
            Chunk chunk = entry.getKey();
            ChunkMeshBuilder.ChunkRenderMesh chunkMesh = entry.getValue();
            if (chunkMesh == null) continue;
            if (chunkMesh.solidMesh() == null || chunkMesh.solidMesh().getIndicesCount() <= 0) continue;

            float worldX = chunk.getChunkX() * Chunk.SIZE_X;
            float worldZ = chunk.getChunkZ() * Chunk.SIZE_Z;
            float centerX = worldX + Chunk.SIZE_X * 0.5f;
            float centerZ = worldZ + Chunk.SIZE_Z * 0.5f;
            if (player != null && (Math.abs(centerX - player.getPosition().x) > SHADOW_SIZE
                    || Math.abs(centerZ - player.getPosition().z) > SHADOW_SIZE)) continue;
            modelMatrix.identity().translate(worldX, 0.0f, worldZ);
            shadowShader.setUniform("uModel", modelMatrix);
            chunkMesh.solidMesh().render();
        }

        for (Entity entity : gameMaster.getEntities()) {
            if (entity == null || !entity.isAlive()) continue;
            if (entity instanceof WorldItem) continue;
            entity.render(gameMaster, RenderPass.SHADOW);
        }

        gameMaster.getWorld().forEach(block -> {
            if (!(block instanceof Crop crop)) return;
            SpriteSheet sheet = ResourceManager.rem.getCropSpritesheets().get(crop.getCropType());
            if (sheet == null) return;

            glActiveTexture(GL_TEXTURE0 + K.Render.PRIMARY_TEXTURE_UNIT);
            sheet.bind();
            shadowShader.setUniform("uTexture", K.Render.PRIMARY_TEXTURE_UNIT);
            shadowShader.setUniform("uAlphaTest", true);

            int frame = crop.getStage().getFrameIndex();
            shadowShader.setUniform("uUVBounds", sheet.getUVBounds(frame));
            float renderX = crop.getX() + 0.5f;
            boolean usesPlantMesh = crop.getCropType().usesPlantMesh();
            float renderY = crop.getY() + (usesPlantMesh
                    ? 1.0f : K.World.SHORTER_BLOCK_HEIGHT);
            float renderZ = crop.getZ() + 0.5f;

            modelMatrix.identity()
                    .translate(renderX, renderY, renderZ);

            shadowShader.setUniform("uModel", modelMatrix);
            if (usesPlantMesh) {
                ResourceManager.rem.getBillboardMesh().render();
            } else {
                ResourceManager.rem.getBillboardMesh().render();
            }
            sheet.unbind();
        });

        gameMaster.getWorld().forEachPlant(plant -> {
            BlockData data = plant.data();
            TextureAtlas.TextureRegion region = data.getTopRegion();
            if (region == null) return;

            glActiveTexture(GL_TEXTURE0 + K.Render.PRIMARY_TEXTURE_UNIT);
            ResourceManager.rem.getBlocksAtlas().bind();

            shadowShader.setUniform("uTexture", K.Render.PRIMARY_TEXTURE_UNIT);
            shadowShader.setUniform("uAlphaTest", true);

            shadowShader.setUniform("uUVBounds",
                    new Vector4f(
                    region.uvMin().x,
                    region.uvMax().y,
                    region.uvMax().x,
                    region.uvMin().y));

            float renderX = plant.x() + 0.5f;
            float renderY = plant.y();
            float renderZ = plant.z() + 0.5f;

            modelMatrix.identity()
                    .translate(renderX, renderY, renderZ);

            shadowShader.setUniform("uModel", modelMatrix);
            ResourceManager.rem.getBillboardMesh().render();
        });

        shadowShader.unbind();
        glCullFace(GL_BACK);
        shadowMap.unbind((int) gameMaster.getWindowWidth(),
                (int) gameMaster.getWindowHeight());
    }

    /**
     * Updates the light matrix.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param shadowMap the {@link ShadowMap} supplied as {@code shadowMap}
     */
    private void updateLightMatrix(GameMaster gameMaster, ShadowMap shadowMap) {
        lightDirection.set(gameMaster.getCelestialLighting().getDirection()).normalize();

        Player player = Player.plyr;
        if (player != null) {
            target.set(player.getPosition());
        } else {
            target.set(0.0f, 0.0f, 0.0f);
        }

        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

        if (Math.abs(lightDirection.y) > 0.98f) {
            up.set(0.0f, 0.0f, 1.0f);
        }

        lightRight.set(lightDirection).cross(up).normalize();
        lightUp.set(lightRight).cross(lightDirection).normalize();
        float texelSize = (SHADOW_SIZE * 2.0f) / shadowMap.getWidth();
        float targetLightX = target.dot(lightRight);
        float targetLightY = target.dot(lightUp);
        snappedTarget.set(target)
                .fma(Math.round(targetLightX / texelSize) * texelSize - targetLightX, lightRight)
                .fma(Math.round(targetLightY / texelSize) * texelSize - targetLightY, lightUp);

        lightPosition.set(snappedTarget).sub(new Vector3f(lightDirection).mul(SHADOW_DISTANCE));

        projection.identity().ortho(-SHADOW_SIZE, SHADOW_SIZE, -SHADOW_SIZE, SHADOW_SIZE, SHADOW_NEAR, SHADOW_FAR);
        view.identity().lookAt(lightPosition, snappedTarget, up);
        lightSpace.set(projection).mul(view);
    }

    /**
     * Returns the light space matrix.
     * @return the {@link Matrix4f} representing the light space matrix
     */
    public Matrix4f getLightSpaceMatrix() {
        return lightSpace;
    }
}
