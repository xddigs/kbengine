package org.kbeng.graphics;

import org.kbeng.entity.WorldItem;
import org.kbeng.item.Item;
import org.kbeng.item.Tool;
import org.kbeng.utils.K;
import org.kbeng.utils.Settings;
import org.kbeng.wrld.GameMaster;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * ItemRenderer provides item renderer capabilities within the graphics subsystem.
 *
 * It contributes to OpenGL resource ownership, render-state setup, or frame-pipeline execution.
 *
 * The renderer focuses on draw ordering, shader inputs, and frame-consistent visual output.
 */
public class ItemRenderer {
    private static final int THICKNESS_LAYERS = 24;
    private static final float LAYER_DEPTH = 0.0025f;

    private final Matrix4f baseModelMatrix;
    private final Mesh quadMesh;

    /**
     * Creates a new {@code ItemRenderer} instance.
     */
    public ItemRenderer() {
        this.baseModelMatrix = new Matrix4f();
        this.quadMesh = Mesh.createCenteredQuad();
    }

    /**
     * Transforms this object according to the supplied values.
     * @param value the {@code float} supplied as {@code value}
     * @return {@code float}; the clamp result
     */
    private float clamp(float value) {
        return Math.clamp(value, 0.0f, 1.0f);
    }

    /**
     * Renders the world item.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param worldItem the {@link WorldItem} supplied as {@code worldItem}
     * @param lighting the {@link CelestialLighting} supplied as {@code lighting}
     */
    public void renderWorldItem(GameMaster gameMaster, WorldItem worldItem,
                                CelestialLighting lighting) {
        if (worldItem == null) return;
        Item item = worldItem.getItem();
        if (item == null) return;
        SpriteSheet spriteSheet = ResourceManager.getItemSpriteSheet(item);
        if (spriteSheet == null) return;
        Shader shader = ResourceManager.rem.getShader("item");
        if (shader == null) return;
        renderWorldItemMesh(gameMaster, worldItem, item, spriteSheet, shader, lighting);
    }

    /**
     * Renders the world item mesh.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param worldItem the {@link WorldItem} supplied as {@code worldItem}
     * @param item the {@link Item} supplied as {@code item}
     * @param spriteSheet the {@link SpriteSheet} supplied as {@code spriteSheet}
     * @param shader the {@link Shader} supplied as {@code shader}
     * @param lighting the {@link CelestialLighting} supplied as {@code lighting}
     */
    private void renderWorldItemMesh(GameMaster gameMaster, WorldItem worldItem,
                                     Item item, SpriteSheet spriteSheet, Shader shader,
                                     CelestialLighting lighting) {

        if (item == null || spriteSheet == null || quadMesh == null) {
            return;
        }

        Vector3f position = worldItem.getPosition();
        float scale = 0.4f;
        baseModelMatrix.identity().translate(position.x, position.y, position.z)
                .rotateY((float) Math.toRadians(worldItem.getRotation()))
                .rotateZ((float) Math.toRadians(180.0f)).scale(scale, scale, scale);

        int frameIndex = ResourceManager.getItemFrame(item);

        frameIndex = Math.clamp(frameIndex, 0, spriteSheet.getTotalFrames() - 1);
        Vector4f uvBounds = spriteSheet.getUVBounds(frameIndex);
        int textureUnit = K.Render.PRIMARY_TEXTURE_UNIT;

        shader.bind();
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        spriteSheet.bind();
        shader.setUniform("uTexture", textureUnit);
        shader.setUniform("uUseTexture", true);
        shader.setUniform("uUseFaceAtlas", false);
        shader.setUniform("uUVBounds", uvBounds);
        shader.setUniform("uAtlasScale", new Vector2f(1.0f, 1.0f));
        shader.setUniform("uAtlasOffset", new Vector2f(0.0f, 0.0f));
        shader.setUniform("uTopAtlasOffset", new Vector2f(0.0f, 0.0f));
        shader.setUniform("uBottomAtlasOffset", new Vector2f(0.0f, 0.0f));
        shader.setUniform("uSideAtlasOffset", new Vector2f(0.0f, 0.0f));
        shader.setUniform("uProjection", gameMaster.getActiveCamera().getProjectionMatrix());
        shader.setUniform("uView", gameMaster.getActiveCamera().getViewMatrix());
        shader.setUniform("uParticleAlpha", 1.0f);
        shader.setUniform("uIsMaskPass", false);
        shader.setUniform("uPaperTool", Settings.doEnablePaper() && item instanceof Tool);
        shader.setUniform("uEnableShadows", false);
        shader.setUniform("uBaseColor", new Vector3f(1.0f));

        setupUniforms(shader, gameMaster, lighting);
        glDisable(GL_CULL_FACE);
        for (int i = THICKNESS_LAYERS - 1; i >= 0; i--) {
            float zOffset = (i - THICKNESS_LAYERS / 2.0f) * LAYER_DEPTH;
            Matrix4f layerMatrix = new Matrix4f(baseModelMatrix).translate(0.0f, 0.0f, zOffset);
            shader.setUniform("uModel", layerMatrix);
            quadMesh.render();
        }
        glEnable(GL_CULL_FACE);
        shader.setUniform("uPaperTool", false);
        spriteSheet.unbind();
        shader.unbind();
    }

    /**
     * Sets setup uniforms.
     * @param shader the {@link Shader} supplied as {@code shader}
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param lighting the {@link CelestialLighting} supplied as {@code lighting}
     */
    private void setupUniforms(Shader shader, GameMaster gameMaster,
                               CelestialLighting lighting) {
        shader.setUniform("uUseTexture", true);
        shader.setUniform("uUseFaceAtlas", false);
        shader.setUniform("uParticleAlpha", 1.0f);
        shader.setUniform("uAtlasScale", new Vector2f(1.0f, 1.0f));
        shader.setUniform("uAtlasOffset", new Vector2f(0.0f, 0.0f));
        shader.setUniform("uTopAtlasOffset", new Vector2f(0.0f, 0.0f));
        shader.setUniform("uBottomAtlasOffset", new Vector2f(0.0f, 0.0f));
        shader.setUniform("uSideAtlasOffset", new Vector2f(0.0f, 0.0f));
        shader.setUniform("uBaseColor", new Vector3f(1.0f, 1.0f, 1.0f));
        shader.setUniform("uIsMaskPass", false);
        shader.setUniform("uEnableShadows", false);

        Vector3f viewLightDir = new Vector3f(lighting.getDirection());
        gameMaster.getActiveCamera().getViewMatrix().transformDirection(viewLightDir);
        shader.setUniform("uLightDirection", viewLightDir);
        shader.setUniform("uSunColor", lighting.getColor());
        shader.setUniform("uSkyColor", lighting.getColor());
        shader.setUniform("uLightIntensity", lighting.getIntensity());
        shader.setUniform("uAmbientIntensity", lighting.getAmbientIntensity());
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        if (quadMesh != null) {
            quadMesh.dispose();
        }
    }
}
