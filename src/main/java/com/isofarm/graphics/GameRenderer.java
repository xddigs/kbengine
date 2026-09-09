package com.isofarm.graphics;

import com.isofarm.data.BlockData;
import com.isofarm.data.BlockPos;
import com.isofarm.data.BlockShape;
import com.isofarm.data.Crop;
import com.isofarm.data.RenderPass;
import com.isofarm.data.View;
import com.isofarm.entity.Player;
import com.isofarm.input.GameInteraction;
import com.isofarm.service.BookService;
import com.isofarm.service.TimeService;
import com.isofarm.service.ViewService;
import com.isofarm.service.WeatherService;
import com.isofarm.utils.HoveredCell;
import com.isofarm.utils.K;
import com.isofarm.utils.Settings;
import com.isofarm.wrld.Chunk;
import com.isofarm.wrld.GameMaster;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

/**
 * Encapsulates the state and operations required by game renderer within the game runtime.
 * It is the main output for the graphics of the game
 */
public class GameRenderer {
    private static final int MAX_TORCH_LIGHTS = 32;
    private static final int BREAK_VOXELS_PER_AXIS = 4;
    public static final GameRenderer gamr = new GameRenderer();
    private final List<Vector3f> torchLights = new ArrayList<>();
    private final Matrix4f modelMatrix = new Matrix4f();
    private final Matrix4f viewProjMatrix = new Matrix4f();
    private final FrustumIntersection frustum = new FrustumIntersection();
    private float previousCameraYaw;
    private float previousCameraPitch;
    private float blurX;
    private float blurY;
    private float waterTime;
    private Mesh voxelBreakMesh;
    private int voxelBreakMeshKey = Integer.MIN_VALUE;
    private static final float VIEW_FOG_TRANSITION_DURATION = 0.15f;
    private ViewFogState displayedViewFog;
    private ViewFogState previousViewFog;
    private ViewFogState targetViewFog;
    private float viewFogTransition = 1.0f;

    /**
     * Renders this object in the requested render pass.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param chunkMeshes the {@link Map} supplied as {@code chunkMeshes}
     */
    public void render(GameMaster gameMaster, Map<Chunk, ChunkMeshBuilder.ChunkRenderMesh> chunkMeshes) {
        ShadowSystem.sys.render(gameMaster, chunkMeshes);
        waterTime += gameMaster.getGenDelta();
        CameraView camera = gameMaster.getActiveCamera();
        updateViewFogTransition(gameMaster);
        collectTorchLights(gameMaster, camera);
        PointShadowSystem.sys.render(gameMaster, chunkMeshes, torchLights);
        float windowWidth = gameMaster.getWindowWidth();
        float windowHeight = gameMaster.getWindowHeight();
        Framebuffer sceneFbo = gameMaster.getSceneFbo();

        sceneFbo.bind();
        glViewport(0, 0, (int) windowWidth, (int) windowHeight);

        ViewService viewService = gameMaster.getViewService();
        Vector3f skyColor = viewService.getView() == View.EXTERIOR
                ? TimeService.getSkyColor() : new Vector3f(0.0f);
        glClearColor(skyColor.x, skyColor.y, skyColor.z, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glActiveTexture(GL_TEXTURE0);
        Shader defaultShader = ResourceManager.rem.getDefaultShader();
        defaultShader.bind();
        defaultShader.setUniform("uIsWater", false);
        defaultShader.setUniform("uIsSubmergedEntity", false);
        defaultShader.setUniform("uVoxelBreakActive", false);

        int textureUnit = K.Render.PRIMARY_TEXTURE_UNIT;
        int shadowUnit = 1;

        defaultShader.setUniform("uTexture", textureUnit);
        defaultShader.setUniform("uShadowMap", shadowUnit);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, gameMaster.getShadowMap().getDepthTexture());

        defaultShader.setUniform("uParticleAlpha", 1.0f);
        defaultShader.setUniform("uEnableShadows", Settings.doEnableShadows());
        defaultShader.setUniform("uIsMaskPass", false);

        defaultShader.setUniform("uProjection", camera.getProjectionMatrix());
        defaultShader.setUniform("uView", camera.getViewMatrix());
        uploadView(defaultShader, camera);

        CelestialLighting lighting = gameMaster.getCelestialLighting();
        defaultShader.setUniform("uSunColor", lighting.getColor());
        defaultShader.setUniform("uLightIntensity", lighting.getIntensity());
        defaultShader.setUniform("uLightDirection", lighting.getDirection());
        defaultShader.setUniform("uAmbientIntensity", lighting.getAmbientIntensity());
        defaultShader.setUniform("uSkyColor", TimeService.getSkyColor());
        defaultShader.setUniform("uLightSpaceMatrix", ShadowSystem.sys.getLightSpaceMatrix());
        defaultShader.setUniform("uUVBounds", new Vector4f(0.0f, 0.0f, 1.0f, 1.0f));
        defaultShader.setUniform("uAtlasScale", new Vector2f(1.0f, 1.0f));
        defaultShader.setUniform("uAtlasOffset", new Vector2f(0.0f, 0.0f));
        defaultShader.setUniform("uIsSprite", false);
        defaultShader.setUniform("uIsTorch", false);
        uploadTorchLights(defaultShader);
        PointShadowSystem.sys.bind(defaultShader, 2);

        TextureAtlas blockAtlas = ResourceManager.rem.getBlocksAtlas();
        if (blockAtlas != null) {
            glActiveTexture(GL_TEXTURE0 + textureUnit);
            blockAtlas.bind();
            defaultShader.setUniform("uUseTexture", true);
            defaultShader.setUniform("uUseFaceAtlas", false);
            defaultShader.setUniform("uUVBounds", new Vector4f(0.0f, 0.0f, 1.0f, 1.0f));
            TextureAtlas.TextureRegion lavaRegion = BlockData.LAVA.getTopRegion();
            if (lavaRegion != null) {
                defaultShader.setUniform("uLavaUVBounds", new Vector4f(
                        lavaRegion.uvMin().x, lavaRegion.uvMin().y,
                        lavaRegion.uvMax().x, lavaRegion.uvMax().y));
            }
            TextureAtlas.TextureRegion waterRegion = BlockData.WATER.getTopRegion();
            if (waterRegion != null) {
                defaultShader.setUniform("uWaterUVBounds", new Vector4f(
                        waterRegion.uvMin().x, waterRegion.uvMin().y,
                        waterRegion.uvMax().x, waterRegion.uvMax().y));
            }
        }

        viewProjMatrix.set(camera.getProjectionMatrix()).mul(camera.getViewMatrix());
        frustum.set(viewProjMatrix);

        updateBlur(camera);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        defaultShader.setUniform("uIsWater", false);
        defaultShader.setUniform("uIsSubmergedEntity", false);
        Player player = Player.plyr;
        VoxelBreakState voxelBreak = getVoxelBreakState(gameMaster);
        defaultShader.setUniform("uVoxelBreakActive", voxelBreak != null);
        if (voxelBreak != null) defaultShader.setUniform("uVoxelBreakPosition",
                new Vector3f(voxelBreak.x(), voxelBreak.y(), voxelBreak.z()));
        chunkMeshes.forEach((chunk, chunkMesh) -> {
            if (chunkMesh != null && chunkMesh.solidMesh() != null && chunkMesh.solidMesh().getIndicesCount() > 0) {
                float minX = chunk.getChunkX() * Chunk.SIZE_X;
                float minY = 0;
                float minZ = chunk.getChunkZ() * Chunk.SIZE_Z;
                float maxX = minX + Chunk.SIZE_X;
                float maxY = Chunk.SIZE_Y;
                float maxZ = minZ + Chunk.SIZE_Z;
                if (frustum.testAab(minX, minY, minZ, maxX, maxY, maxZ)) {
                    modelMatrix.identity().translate(minX, 0, minZ);
                    defaultShader.setUniform("uModel", modelMatrix);
                    chunkMesh.solidMesh().render();
                }
            }
        });
        defaultShader.setUniform("uVoxelBreakActive", false);
        if (voxelBreak != null) renderVoxelBreak(defaultShader, voxelBreak);

        Shader grassShader = ResourceManager.rem.getGrassShader();
        grassShader.bind();
        grassShader.setUniform("uTexture", textureUnit);
        grassShader.setUniform("uShadowMap", shadowUnit);
        grassShader.setUniform("uProjection", camera.getProjectionMatrix());
        grassShader.setUniform("uView", camera.getViewMatrix());
        uploadView(grassShader, camera);
        grassShader.setUniform("uSunColor", lighting.getColor());
        grassShader.setUniform("uLightIntensity", lighting.getIntensity());
        grassShader.setUniform("uLightDirection", lighting.getDirection());
        grassShader.setUniform("uAmbientIntensity", lighting.getAmbientIntensity());
        grassShader.setUniform("uSkyColor", TimeService.getSkyColor());
        grassShader.setUniform("uLightSpaceMatrix", ShadowSystem.sys.getLightSpaceMatrix());
        grassShader.setUniform("uEnableShadows", Settings.doEnableShadows());
        grassShader.setUniform("uGrassTint", ResourceManager.rem.getGrassTint());
        uploadTorchLights(grassShader);
        PointShadowSystem.sys.bind(grassShader, 2);
        TextureAtlas.TextureRegion grassTopRegion = BlockData.GRASS.getTopRegion();
        TextureAtlas.TextureRegion grassSideRegion = BlockData.GRASS.getSideRegion();
        if (grassTopRegion != null && grassSideRegion != null) {
            grassShader.setUniform("uGrassTopUVBounds", new Vector4f(
                    grassTopRegion.uvMin().x, grassTopRegion.uvMin().y,
                    grassTopRegion.uvMax().x, grassTopRegion.uvMax().y));
            grassShader.setUniform("uGrassSideUVBounds", new Vector4f(
                    grassSideRegion.uvMin().x, grassSideRegion.uvMin().y,
                    grassSideRegion.uvMax().x, grassSideRegion.uvMax().y));
            glDepthFunc(GL_LEQUAL);
            chunkMeshes.forEach((chunk, chunkMesh) -> {
                if (chunkMesh == null || chunkMesh.solidMesh() == null
                        || chunkMesh.solidMesh().getIndicesCount() <= 0) return;
                float minX = chunk.getChunkX() * Chunk.SIZE_X;
                float minZ = chunk.getChunkZ() * Chunk.SIZE_Z;
                if (frustum.testAab(minX, 0, minZ, minX + Chunk.SIZE_X,
                        Chunk.SIZE_Y, minZ + Chunk.SIZE_Z)) {
                    modelMatrix.identity().translate(minX, 0, minZ);
                    grassShader.setUniform("uModel", modelMatrix);
                    chunkMesh.solidMesh().render();
                }
            });
            glDepthFunc(GL_LESS);
        }

        if (player != null) {
            defaultShader.bind();
            defaultShader.setUniform("uIgnoreViewFog", true);
            player.render(gameMaster, RenderPass.NORMAL);
            defaultShader.bind();
            defaultShader.setUniform("uIgnoreViewFog", false);
            if (blockAtlas != null) {
                glActiveTexture(GL_TEXTURE0 + textureUnit);
                blockAtlas.bind();
                defaultShader.setUniform("uTexture", textureUnit);
                defaultShader.setUniform("uUseTexture", true);
                defaultShader.setUniform("uUseFaceAtlas", false);
                defaultShader.setUniform("uUVBounds", new Vector4f(0.0f, 0.0f, 1.0f, 1.0f));
            }
        }

        defaultShader.bind();
        defaultShader.setUniform("uIsWater", false);
        defaultShader.setUniform("uIsSubmergedEntity", false);
        defaultShader.setUniform("uParticleAlpha", 1.0f);
        ParticleEngine.peng.render(defaultShader, ResourceManager.rem.getSpriteMesh(),
                gameMaster.getActiveCamera());

        defaultShader.bind();
        defaultShader.setUniform("uIsWater", true);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glDepthMask(false);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        chunkMeshes.forEach((chunk, chunkMesh) -> {
            if (chunkMesh == null || chunkMesh.waterMesh() == null ||
                    chunkMesh.waterMesh().getIndicesCount() <= 0) {
                return;
            }

            float minX = chunk.getChunkX() * Chunk.SIZE_X;
            float minZ = chunk.getChunkZ() * Chunk.SIZE_Z;

            float maxX = minX + Chunk.SIZE_X;
            float maxY = Chunk.SIZE_Y;
            float maxZ = minZ + Chunk.SIZE_Z;

            if (frustum.testAab(minX, 0.0f, minZ, maxX, maxY, maxZ)) {
                modelMatrix.identity().translate(minX, 0.0f, minZ);
                defaultShader.setUniform("uModel", modelMatrix);
                defaultShader.setUniform("uTime", waterTime);
                chunkMesh.waterMesh().render();
            }
        });

        glDepthMask(true);

        BlockPos hoveredCell = HoveredCell.get(gameMaster);
        renderTorches(gameMaster, camera, defaultShader);

        defaultShader.bind();
        defaultShader.setUniform("uIsWater", false);
        defaultShader.setUniform("uIsSubmergedEntity", false);

        gameMaster.getWorld().forEach(block -> {
            if (!(block instanceof Crop crop)) return;
            SpriteSheet sheet = ResourceManager.rem.getCropSpritesheets().get(crop.getCropType());
            if (sheet == null) return;

            glActiveTexture(GL_TEXTURE0 + K.Render.PRIMARY_TEXTURE_UNIT);
            sheet.bind();
            defaultShader.setUniform("uTexture", K.Render.PRIMARY_TEXTURE_UNIT);
            defaultShader.setUniform("uUseTexture", true);
            defaultShader.setUniform("uUseFaceAtlas", false);

            int frame = crop.getStage().getFrameIndex();
            defaultShader.setUniform("uUVBounds", sheet.getUVBounds(frame));
            defaultShader.setUniform("uSunColor", lighting.getColor());
            defaultShader.setUniform("uSkyColor", TimeService.getSkyColor());
            defaultShader.setUniform("uLightDirection", lighting.getDirection());
            defaultShader.setUniform("uLightIntensity", lighting.getIntensity());
            defaultShader.setUniform("uAmbientIntensity", lighting.getAmbientIntensity());
            defaultShader.setUniform("uLightSpaceMatrix", ShadowSystem.sys.getLightSpaceMatrix());

            float renderX = crop.getX() + 0.5f;
            boolean usesPlantMesh = crop.getCropType().usesPlantMesh();
            float renderY = crop.getY() + (usesPlantMesh
                    ? 1.0f : K.World.SHORTER_BLOCK_HEIGHT);
            float renderZ = crop.getZ() + 0.5f;

            modelMatrix.identity().translate(renderX, renderY, renderZ);
            defaultShader.setUniform("uModel", modelMatrix);
            if (usesPlantMesh) glDisable(GL_CULL_FACE);
            if (usesPlantMesh) {
                ResourceManager.rem.getFlowerMesh().render();
            } else {
                ResourceManager.rem.getSpriteMesh().render();
            }
            if (usesPlantMesh) glEnable(GL_CULL_FACE);
            sheet.unbind();
        });

        gameMaster.getWorld().forEachInteractiveBlock(block -> {
            if (block.getBlockModel() == null) return;

            defaultShader.setUniform("uUseFaceAtlas", false);
            defaultShader.setUniform("uIsSprite", false);
            defaultShader.setUniform("uUseTexture", true);
            defaultShader.setUniform("uSunColor", lighting.getColor());
            defaultShader.setUniform("uSkyColor", TimeService.getSkyColor());
            defaultShader.setUniform("uLightDirection", lighting.getDirection());
            defaultShader.setUniform("uLightIntensity", lighting.getIntensity());
            defaultShader.setUniform("uAmbientIntensity", lighting.getAmbientIntensity());

            block.getModelTransform(modelMatrix);
            block.getBlockModel().render(defaultShader, modelMatrix);
        });

        defaultShader.setUniform("uIsWater", false);
        defaultShader.setUniform("uIsSubmergedEntity", false);

        gameMaster.getWorld().forEachPlant(plant -> {
            BlockData data = plant.data();
            TextureAtlas.TextureRegion region = data.getTopRegion();
            if (region == null) return;

            float renderX = plant.x() + 0.5f;
            float renderY = plant.y();
            float renderZ = plant.z() + 0.5f;

            modelMatrix.identity().translate(renderX, renderY, renderZ);
            defaultShader.setUniform("uModel", modelMatrix);
            defaultShader.setUniform("uTexture", K.Render.PRIMARY_TEXTURE_UNIT);
            defaultShader.setUniform("uUseTexture", true);
            defaultShader.setUniform("uUseFaceAtlas", false);

            defaultShader.setUniform("uUVBounds", new Vector4f(
                    region.uvMin().x, region.uvMax().y,
                    region.uvMax().x, region.uvMin().y));

            glDisable(GL_CULL_FACE);
            glActiveTexture(GL_TEXTURE0 + K.Render.PRIMARY_TEXTURE_UNIT);
            ResourceManager.rem.getBlocksAtlas().bind();
            defaultShader.setUniform("uAmbientIntensity", 1.0f);
            ResourceManager.rem.getFlowerMesh().render();
            defaultShader.setUniform("uAmbientIntensity", lighting.getAmbientIntensity());
            glEnable(GL_CULL_FACE);
        });

        if (blockAtlas != null) {
            glActiveTexture(GL_TEXTURE0 + textureUnit);
            blockAtlas.bind();
            defaultShader.setUniform("uUseTexture", true);
            defaultShader.setUniform("uUseFaceAtlas", false);
            defaultShader.setUniform("uUVBounds", new Vector4f(0.0f, 0.0f, 1.0f, 1.0f));
            defaultShader.setUniform("uAtlasScale", new Vector2f(1.0f, 1.0f));
            defaultShader.setUniform("uAtlasOffset", new Vector2f(0.0f, 0.0f));
        }

        defaultShader.setUniform("uIsWater", false);
        defaultShader.setUniform("uIsSubmergedEntity", false);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glDepthMask(true);

        gameMaster.getEntities().stream()
                .filter(entity -> entity != player)
                .forEach(entity -> entity.render(gameMaster, RenderPass.NORMAL));

        if (WeatherService.isRaining() && viewService.getView() == View.EXTERIOR) {
            Vector3f rainTargetPos = (player != null)
                    ? new Vector3f(player.getPosition().x(), player.getPosition().y() + 10.0f,
                    player.getPosition().z())
                    : camera.getPosition();

            gameMaster.getRainEngine().render(ResourceManager.rem.getRainShader(),
                    camera.getViewMatrix(), camera.getProjectionMatrix(),
                    rainTargetPos, gameMaster.getWorld());
        }

        if (blockAtlas != null) blockAtlas.unbind();

        if (hoveredCell != null) {
            Vector3f outlineColor = getOutlineColor();
            glEnable(GL_DEPTH_TEST);
            glLineWidth(2.0f);
            glDepthMask(false);
            defaultShader.bind();
            defaultShader.setUniform("uUseTexture", false);
            defaultShader.setUniform("uUseFaceAtlas", false);
            defaultShader.setUniform("uBaseColor", outlineColor);

            defaultShader.setUniform("uIsWater", false);
            defaultShader.setUniform("uIsSprite", false);
            defaultShader.setUniform("uIsSubmergedEntity", false);
            defaultShader.setUniform("uEnableShadows", false);

            defaultShader.setUniform("uUseParticleAlpha", false);
            defaultShader.setUniform("uParticleAlpha", 1.0f);

            var selectedInteractiveBlock = gameMaster.getWorld().getInteractiveBlockAt(
                    hoveredCell.x(), hoveredCell.y(), hoveredCell.z());
            if (selectedInteractiveBlock != null
                    && selectedInteractiveBlock.getType().isDoor()) {
                selectedInteractiveBlock.getSelectionTransform(modelMatrix);
            } else {
                modelMatrix.identity().translate(
                        hoveredCell.x(), hoveredCell.y(), hoveredCell.z());
            }

            defaultShader.setUniform("uModel", modelMatrix);
            BlockShape selectedShape = hoveredCell.data() instanceof BlockData
                    ? gameMaster.getWorld().getBlockShapeAt(
                    hoveredCell.x(), hoveredCell.y(), hoveredCell.z()) : null;
            ResourceManager.rem.getSelectionMesh(selectedShape).renderLines();

            glDepthMask(true);
            glEnable(GL_DEPTH_TEST);
            defaultShader.bind();
            defaultShader.setUniform("uIsMaskPass", false);
        }

        defaultShader.unbind();
        sceneFbo.unbind((int) windowWidth, (int) windowHeight);

        if (gameMaster.isInventoryOpen() || gameMaster.isBackpackOpen() || BookService.bs.isOpen()) {
            glDisable(GL_DEPTH_TEST);
            Shader blurShader = ResourceManager.rem.getBlurShader();
            Vector2f resolution = new Vector2f(windowWidth, windowHeight);

            Framebuffer blurFbo = gameMaster.getBlurFbo();
            blurFbo.bind();
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            blurShader.bind();
            blurShader.setUniform("uResolution", resolution);
            blurShader.setUniform("uDirection", new Vector2f(1.0f, 0.0f));
            blurShader.setUniform("uBlurRadius", 5.0f);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, sceneFbo.getTextureId());
            blurShader.setUniform("screenTexture", 0);
            ResourceManager.rem.getScreenQuadMesh().render();
            blurShader.unbind();
            blurFbo.unbind((int) windowWidth, (int) windowHeight);

            glClear(GL_COLOR_BUFFER_BIT);
            blurShader.bind();
            blurShader.setUniform("uResolution", resolution);
            blurShader.setUniform("uDirection", new Vector2f(0.0f, 1.0f));
            blurShader.setUniform("uBlurRadius", 3.0f);

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, blurFbo.getTextureId());
            blurShader.setUniform("screenTexture", 0);

            ResourceManager.rem.getScreenQuadMesh().render();
            blurShader.unbind();
            glEnable(GL_DEPTH_TEST);

        } else {
            glDisable(GL_DEPTH_TEST);
            Shader motionBlurShader = ResourceManager.rem.getMotionBlurShader();
            motionBlurShader.bind();
            motionBlurShader.setUniform("uScene", 0);
            motionBlurShader.setUniform("uVelocity", new Vector2f(blurX, blurY));
            motionBlurShader.setUniform("uStrength", Settings.doEnableMotions());

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, sceneFbo.getTextureId());

            ResourceManager.rem.getScreenQuadMesh().render();

            motionBlurShader.unbind();
            glEnable(GL_DEPTH_TEST);
        }

        if (player.isNoClip()) {
            glDisable(GL_CULL_FACE);
        } else {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
            glEnable(GL_DEPTH_TEST);
        }
    }

    /**
     * Returns the outline color.
     * @return the {@link Vector3f} representing the outline color
     */
    private Vector3f getOutlineColor() {
        boolean isSmartShift = GameInteraction.gami != null
                && GameInteraction.gami.isSmartShiftActive();
        return isSmartShift ? new Vector3f(1.0f, 1.0f, 1.0f) : K.Colors.OUTLINE_DEFAULT;
    }

    /** Uploads nearby emissive blocks so the world shader can light them. */
    private void collectTorchLights(GameMaster gameMaster, CameraView camera) {
        torchLights.clear();
        ViewService viewService = gameMaster.getViewService();
        Player player = Player.plyr;
        Vector3f centerPosition = (player != null) ? player.getPosition() : camera.getPosition();
        float searchDistance = 24.0f;
        float searchDistanceSq = searchDistance * searchDistance;

        gameMaster.getWorld().forEachTorch(torch -> {
            BlockShape.Box bounds = getTorchBounds(gameMaster, torch);
            Vector3f position = new Vector3f(
                    torch.x() + center(bounds.minX(), bounds.maxX()),
                    torch.y() + bounds.maxY() - 0.15f,
                    torch.z() + center(bounds.minZ(), bounds.maxZ()));
            if (viewService.isVisible(position)
                    && position.distanceSquared(centerPosition) <= searchDistanceSq) {
                torchLights.add(position);
            }
        });

        gameMaster.getWorld().forEachLava(lava -> {
            Vector3f position = new Vector3f(lava.x() + 0.5f, lava.y() + 0.5f, lava.z() + 0.5f);
            if (viewService.isVisible(position)
                    && position.distanceSquared(centerPosition) <= searchDistanceSq) {
                torchLights.add(position);
            }
        });

        torchLights.sort(Comparator.comparingDouble(position ->
                position.distanceSquared(centerPosition)));
    }

    /** Uploads the collected artificial lights to the material shader. */
    private void uploadTorchLights(Shader shader) {
        int lightCount = Math.min(torchLights.size(), MAX_TORCH_LIGHTS);
        shader.setUniform("uTorchCount", lightCount);
        for (int index = 0; index < lightCount; index++) {
            shader.setUniform("uTorchPositions[" + index + "]", torchLights.get(index));
        }
    }

    /** Renders every torch as an animated, camera-facing billboard. */
    private void renderTorches(GameMaster gameMaster, CameraView camera, Shader shader) {
        SpriteSheet torchFrames = ResourceManager.rem.getTorchIcons();
        if (torchFrames == null) return;

        shader.bind();
        shader.setUniform("uIsWater", false);
        shader.setUniform("uIsSubmergedEntity", false);
        shader.setUniform("uIsSprite", true);
        shader.setUniform("uIsTorch", true);
        shader.setUniform("uUseTexture", true);
        shader.setUniform("uUseFaceAtlas", false);
        shader.setUniform("uUVBounds", torchFrames.getUVBounds(
                (int) ((System.nanoTime() / 125_000_000L) % torchFrames.getTotalFrames())));

        glActiveTexture(GL_TEXTURE0 + K.Render.PRIMARY_TEXTURE_UNIT);
        torchFrames.bind();
        glDisable(GL_CULL_FACE);
        gameMaster.getWorld().forEachTorch(torch -> {
            BlockShape.Box bounds = getTorchBounds(gameMaster, torch);
            float centerX = torch.x() + center(bounds.minX(), bounds.maxX());
            float centerZ = torch.z() + center(bounds.minZ(), bounds.maxZ());
            float angle = (float) Math.atan2(camera.getPosition().x - centerX,
                    camera.getPosition().z - centerZ);
            modelMatrix.identity().translate(centerX, torch.y() + bounds.minY(), centerZ)
                    .rotateY(angle).scale(0.45f, 0.8f, 0.45f);
            shader.setUniform("uModel", modelMatrix);
            ResourceManager.rem.getPlayerMesh().render();
        });
        glEnable(GL_CULL_FACE);
        torchFrames.unbind();
        shader.setUniform("uIsTorch", false);
        shader.setUniform("uIsSprite", false);
    }

    /** Uploads the common cutaway and fog-of-war volume to a world shader. */
    private void uploadView(Shader shader, CameraView camera) {
        ViewFogState fog = getRenderedViewFog();
        Player player = Player.plyr;
        shader.setUniform("uViewMode", fog.view().getShaderId());
        shader.setUniform("uViewPlayerPosition", player == null
                ? new Vector3f() : player.getPosition());
        shader.setUniform("uViewCameraPosition", camera.getPosition());
        shader.setUniform("uViewBounds", fog.bounds());
        shader.setUniform("uViewRadius", fog.radius());
        shader.setUniform("uViewFloorY", fog.floorY());
        shader.setUniform("uViewCeilingY", fog.ceilingY());
        shader.setUniform("uViewFogStrength", getViewFogStrength());
        shader.setUniform("uIgnoreViewFog", false);
    }

    private void updateViewFogTransition(GameMaster gameMaster) {
        ViewFogState current = captureViewFog(gameMaster.getViewService());
        if (displayedViewFog == null) {
            displayedViewFog = current;
            targetViewFog = current;
            return;
        }
        if (current.view() != targetViewFog.view()) {
            previousViewFog = getRenderedViewFog();
            targetViewFog = current;
            viewFogTransition = 0.0f;
        } else if (viewFogTransition >= 1.0f) {
            displayedViewFog = current;
            targetViewFog = current;
        }
        if (viewFogTransition < 1.0f) {
            viewFogTransition = Math.min(1.0f, viewFogTransition
                    + gameMaster.getGenDelta() / VIEW_FOG_TRANSITION_DURATION);
            if (viewFogTransition >= 1.0f) displayedViewFog = targetViewFog;
        }
    }

    private ViewFogState getRenderedViewFog() {
        if (viewFogTransition >= 1.0f || targetViewFog.view() != View.EXTERIOR) {
            return targetViewFog;
        }
        return previousViewFog;
    }

    private float getViewFogStrength() {
        if (viewFogTransition >= 1.0f) return targetViewFog.view() == View.EXTERIOR ? 0.0f : 1.0f;
        return targetViewFog.view() == View.EXTERIOR ? 1.0f - viewFogTransition : viewFogTransition;
    }

    private static ViewFogState captureViewFog(ViewService service) {
        return new ViewFogState(service.getView(), service.getBounds(),
                Settings.getUndergroundViewRadius(), service.getFloorY(), service.getCeilingY());
    }

    private record ViewFogState(View view, Vector4f bounds, float radius,
                                float floorY, float ceilingY) { }

    /** Returns the physical bounds that also anchor a placed torch sprite. */
    private static BlockShape.Box getTorchBounds(GameMaster gameMaster, BlockPos torch) {
        BlockShape shape = gameMaster.getWorld().getBlockShapeAt(
                torch.x(), torch.y(), torch.z());
        BlockShape.Box[] boxes = shape.getBoxes();
        return boxes.length == 0
                ? BlockShape.TORCH_FLOOR.getBoxes()[0]
                : boxes[0];
    }

    private static float center(float minimum, float maximum) {
        return (minimum + maximum) * 0.5f;
    }

    private VoxelBreakState getVoxelBreakState(GameMaster gameMaster) {
        if (!GameInteraction.gami.isBreakingBlock()) return null;
        Vector3i pos = GameInteraction.gami.getBreakingBlockPos();
        BlockData data = BlockData.fromId(gameMaster.getWorld().getBlockTypeAt(pos.x, pos.y, pos.z));
        if (data == null || data == BlockData.AIR || data.isFluid()) return null;
        return new VoxelBreakState(pos.x, pos.y, pos.z, data,
                gameMaster.getWorld().getBlockShapeAt(pos.x, pos.y, pos.z),
                GameInteraction.gami.getBreakProgress());
    }

    private void renderVoxelBreak(Shader shader, VoxelBreakState state) {
        TextureAtlas.TextureRegion region = state.data().getSideRegion();
        if (region == null) region = state.data().getTopRegion();
        if (region == null) return;
        int stages = Math.clamp((int) Math.ceil(state.data().getDestroyTime() * 10.0f), 6, 24);
        int removed = Math.min(stages - 1, (int) (state.progress() * stages))
                * BREAK_VOXELS_PER_AXIS * BREAK_VOXELS_PER_AXIS * BREAK_VOXELS_PER_AXIS / stages;
        shader.setUniform("uUVBounds", new Vector4f(region.uvMin().x, region.uvMin().y,
                region.uvMax().x, region.uvMax().y));
        int key = (((state.x() * 31 + state.y()) * 31 + state.z()) * 67) + removed;
        if (key != voxelBreakMeshKey) {
            if (voxelBreakMesh != null) voxelBreakMesh.dispose();
            voxelBreakMesh = Mesh.createBreakingVoxelMesh(BREAK_VOXELS_PER_AXIS, removed);
            voxelBreakMeshKey = key;
        }
        modelMatrix.identity().translate(state.x(), state.y(), state.z());
        shader.setUniform("uModel", modelMatrix);
        voxelBreakMesh.render();
        shader.setUniform("uUVBounds", new Vector4f(0, 0, 1, 1));
    }

    private record VoxelBreakState(int x, int y, int z, BlockData data, BlockShape shape, float progress) { }

    /**
     * Updates the blur.
     * @param camera the {@link CameraView} supplied as {@code camera}
     */
    private void updateBlur(CameraView camera) {
        float yawDelta = camera.getYaw() - previousCameraYaw;
        if (yawDelta > K.Camera.HALF_DEGREES) yawDelta -= K.Camera.FULL_DEGREES;
        else if (yawDelta < -K.Camera.HALF_DEGREES) yawDelta += K.Camera.FULL_DEGREES;

        float pitchDelta = camera.getPitch() - previousCameraPitch;
        previousCameraYaw = camera.getYaw();
        previousCameraPitch = camera.getPitch();
        blurX = yawDelta / K.Camera.FULL_DEGREES;
        blurY = pitchDelta / K.Camera.HALF_DEGREES;
    }
}
