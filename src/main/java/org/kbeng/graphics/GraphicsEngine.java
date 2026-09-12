package org.kbeng.graphics;

import org.kbeng.wrld.GameMaster;
import org.kbeng.wrld.World;

/**
 * Runtime graphics coordinator and GPU resource owner.
 * <p>This component centralizes render-target lifecycle and high-level render
 * execution that used to live in {@code GameMaster}. It owns:
 * scene/blur framebuffers, directional shadow map, world-item renderer,
 * and rain simulation/render resources.
 */
public final class GraphicsEngine {
    private final GameMaster gameMaster;

    private Framebuffer sceneFbo;
    private Framebuffer blurFbo;
    private ShadowMap shadowMap;
    private ItemRenderer itemRenderer;
    private RainEngine rainEngine;

    /**
     * Creates a graphics engine bound to one game orchestrator instance.
     * @param gameMaster runtime coordinator providing world/camera/context access
     */
    public GraphicsEngine(GameMaster gameMaster) {
        this.gameMaster = gameMaster;
    }

    /**
     * Allocates long-lived GPU resources for rendering.
     * @param width initial viewport width in pixels
     * @param height initial viewport height in pixels
     */
    public void initialize(int width, int height) {
        shadowMap = new ShadowMap((int) org.kbeng.utils.Settings.getShadowMapSize(),
                (int) org.kbeng.utils.Settings.getShadowMapSize());
        sceneFbo = new Framebuffer(width, height);
        blurFbo = new Framebuffer(width, height);
        itemRenderer = new ItemRenderer();
        rainEngine = new RainEngine();
    }

    /**
     * Advances rain simulation timing.
     * @param delta elapsed frame time in seconds
     */
    public void updateRain(float delta) {
        if (rainEngine != null) rainEngine.update(delta);
    }

    /**
     * Executes the full frame render pipeline.
     * <p>The world and camera parameters are part of the explicit contract of
     * the orchestrator call site. Current internals still route through
     * {@link GameRenderer}, HUD render, and paper pass.
     * @param world current world instance
     * @param camera active camera view
     */
    public void render(World world, CameraView camera) {
        if (world == null || camera == null || gameMaster.getChunkManager() == null) return;
        GameRenderer.gamr.render(gameMaster, gameMaster.getChunkManager().getChunkMeshes());
        org.kbeng.ui.GameUIService.ui.render(gameMaster.isHUDShown(), gameMaster);
        GameRenderer.gamr.renderPaper(gameMaster);
    }

    /**
     * Recreates render targets after a framebuffer-size change.
     * @param width new framebuffer width in pixels
     * @param height new framebuffer height in pixels
     */
    public void onResize(int width, int height) {
        if (sceneFbo == null || blurFbo == null) return;
        sceneFbo.dispose();
        blurFbo.dispose();
        sceneFbo = new Framebuffer(width, height);
        blurFbo = new Framebuffer(width, height);
    }

    /**
     * Releases all owned GPU/native graphics resources.
     */
    public void dispose() {
        if (itemRenderer != null) itemRenderer.dispose();
        if (sceneFbo != null) sceneFbo.dispose();
        if (blurFbo != null) blurFbo.dispose();
        if (rainEngine != null) rainEngine.dispose();
        if (shadowMap != null) shadowMap.dispose();
        PointShadowSystem.sys.dispose();
        ResourceManager.rem.dispose();
    }

    /**
     * Returns the primary scene framebuffer used by the world pass.
     * @return scene framebuffer
     */
    public Framebuffer getSceneFbo() {
        return sceneFbo;
    }

    /**
     * Returns the auxiliary framebuffer used by blur/post-processing passes.
     * @return blur framebuffer
     */
    public Framebuffer getBlurFbo() {
        return blurFbo;
    }

    /**
     * Returns the active directional shadow map.
     * @return shadow depth target
     */
    public ShadowMap getShadowMap() {
        return shadowMap;
    }

    /**
     * Returns the dedicated renderer for world-item entities.
     * @return world-item renderer
     */
    public ItemRenderer getItemRenderer() {
        return itemRenderer;
    }

    /**
     * Returns the rain subsystem used by weather rendering.
     * @return rain engine instance
     */
    public RainEngine getRainEngine() {
        return rainEngine;
    }
}
