package org.kbeng.games.rpg.graphics;

import org.kbeng.engine.graphics.*;
import org.kbeng.engine.utils.Settings;
import org.kbeng.games.rpg.data.RenderPass;
import org.kbeng.games.rpg.service.*;
import org.kbeng.games.rpg.entity.Player;
import org.kbeng.games.rpg.utils.HoveredCell;
import org.kbeng.games.rpg.wrld.GameMaster;
import org.joml.*;
import java.util.Map;
import static org.lwjgl.opengl.GL33.*;

/** Active colour-only terrain frame. Actors retain their original model shaders
 * and transforms; the original UI is drawn by GraphicsEngine after this scene.
 * The old textured terrain renderer is retained separately for archival use. */
public final class VoxelSceneRenderer {
    private final Shader terrain = new Shader("rpg/shaders/voxel.vert", "rpg/shaders/voxel.frag");
    private final Shader shadow = new Shader("rpg/shaders/voxel_shadow.vert", "rpg/shaders/voxel_shadow.frag");
    public void render(GameMaster game) {
        VoxelTerrain meshes = game.getChunkManager().getVoxelTerrain();
        meshes.flush();
        ShadowSystem.sys.renderVoxels(game, meshes, shadow);
        var camera = game.getActiveCamera();
        var lighting = game.getCelestialLighting();
        var sky = TimeService.getSkyColor();
        game.getSceneFbo().bind();
        glViewport(0, 0, (int) game.getWindowWidth(), (int) game.getWindowHeight());
        glEnable(GL_DEPTH_TEST); glDepthMask(true); glEnable(GL_CULL_FACE); glCullFace(GL_BACK);
        glClearColor(sky.x, sky.y, sky.z, 1); glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        terrain.bind();
        terrain.setUniform("uProjection", camera.getProjectionMatrix());
        terrain.setUniform("uView", camera.getViewMatrix());
        terrain.setUniform("uLightDirection", lighting.getDirection());
        terrain.setUniform("uSunColor", lighting.getColor());
        terrain.setUniform("uLightIntensity", lighting.getIntensity());
        terrain.setUniform("uAmbientIntensity", lighting.getAmbientIntensity());
        terrain.setUniform("uEnableShadows", Settings.doEnableShadows());
        terrain.setUniform("uLightSpaceMatrix", ShadowSystem.sys.getLightSpaceMatrix());
        terrain.setUniform("uShadowMap", 1);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, game.getShadowMap().getDepthTexture()); glActiveTexture(GL_TEXTURE0);
        GameRenderer.gamr.uploadView(terrain, camera);
        if (game.getCamera().isFirstPerson()) {
            terrain.setUniform("uViewFogStrength", 0f);
            terrain.setUniform("uIgnoreViewFog", true);
        }
        terrain.setUniform("uAlpha", 1f);
        var hit = HoveredCell.voxel(game);
        boolean selected = org.kbeng.games.rpg.voxel.VoxelInteraction.reachable(hit)
                && !game.isInventoryOpen() && !game.isBackpackOpen() && !game.isChatOpen()
                && !BookService.bs.isOpen();
        terrain.setUniform("uSelected", selected);
        if (selected) terrain.setUniform("uSelectedMin", hit.minimum());
        meshes.render(terrain, camera, false);
        terrain.unbind();

        Shader actor = ResourceManager.rem.getDefaultShader();
        actor.bind();
        actor.setUniform("uProjection", camera.getProjectionMatrix()); actor.setUniform("uView", camera.getViewMatrix());
        GameRenderer.gamr.uploadView(actor, camera);
        if (game.getCamera().isFirstPerson()) {
            actor.setUniform("uViewFogStrength", 0f);
            actor.setUniform("uIgnoreViewFog", true);
        }
        actor.setUniform("uSunColor", lighting.getColor());
        actor.setUniform("uLightDirection", lighting.getDirection());
        actor.setUniform("uLightIntensity", lighting.getIntensity());
        actor.setUniform("uAmbientIntensity", lighting.getAmbientIntensity());
        actor.setUniform("uSkyColor", sky);
        actor.setUniform("uEnableShadows", Settings.doEnableShadows());
        actor.setUniform("uIgnoreViewFog", false); actor.setUniform("uVoxelBreakActive", false);
        actor.setUniform("uModelBreakActive", false); actor.setUniform("uIsWater", false);
        actor.setUniform("uIsSubmergedEntity", false); actor.setUniform("uParticleAlpha", 1f);
        actor.setUniform("uIsMaskPass", false); actor.setUniform("uPaperTool", false);
        actor.setUniform("uTorchCount", 0); actor.setUniform("uShadowedTorchCount", 0);
        actor.setUniform("uLightSpaceMatrix", ShadowSystem.sys.getLightSpaceMatrix());
        actor.setUniform("uShadowMap", 1);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, game.getShadowMap().getDepthTexture()); glActiveTexture(GL_TEXTURE0);
        for (var entity : game.getEntities()) {
            if (game.getCamera().isFirstPerson() && entity == Player.plyr) continue;
            entity.render(game, RenderPass.NORMAL);
        }
        actor.bind();
        ParticleEngine.peng.render(actor, ResourceManager.rem.getBillboardMesh(), camera);
        actor.unbind();

        terrain.bind(); terrain.setUniform("uAlpha", 0.72f);
        glEnable(GL_BLEND); glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); glDepthMask(false);
        meshes.render(terrain, camera, true);
        glDepthMask(true); terrain.unbind();
        if (WeatherService.isRaining()) game.getRainEngine().render(ResourceManager.rem.getRainShader(),
                camera.getViewMatrix(), camera.getProjectionMatrix(), new Vector3f(Player.plyr.getPosition()).add(0, 10, 0), game.getWorld());
        game.getSceneFbo().unbind((int) game.getWindowWidth(), (int) game.getWindowHeight());
        present(game);
    }
    /** Preserves the inventory/book blur and ordinary scene presentation. */
    private void present(GameMaster game) {
        boolean blur = game.isInventoryOpen() || game.isBackpackOpen() || BookService.bs.isOpen();
        glDisable(GL_DEPTH_TEST); glDisable(GL_CULL_FACE);
        if (blur) {
            Shader shader = ResourceManager.rem.getBlurShader(); shader.bind();
            shader.setUniform("uResolution", new Vector2f(game.getWindowWidth(), game.getWindowHeight()));
            shader.setUniform("screenTexture", 0); shader.setUniform("uBlurRadius", 5f);
            shader.setUniform("uDirection", new Vector2f(1, 0));
            game.getBlurFbo().bind();
            glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, game.getSceneFbo().getTextureId());
            ResourceManager.rem.getScreenQuadMesh().render();
            game.getBlurFbo().unbind((int) game.getWindowWidth(), (int) game.getWindowHeight());
            shader.setUniform("uDirection", new Vector2f(0, 1));
            glBindTexture(GL_TEXTURE_2D, game.getBlurFbo().getTextureId());
            ResourceManager.rem.getScreenQuadMesh().render(); shader.unbind();
        } else {
            Shader shader = ResourceManager.rem.getMotionBlurShader(); shader.bind();
            shader.setUniform("uScene", 0);
            shader.setUniform("uVelocity", new Vector2f(GameRenderer.gamr.blurX(), GameRenderer.gamr.blurY()));
            shader.setUniform("uStrength", Settings.doEnableMotions());
            glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, game.getSceneFbo().getTextureId());
            ResourceManager.rem.getScreenQuadMesh().render(); shader.unbind();
        }
        glEnable(GL_DEPTH_TEST); glEnable(GL_CULL_FACE);
    }
    public void dispose() { terrain.dispose(); shadow.dispose(); }
}
