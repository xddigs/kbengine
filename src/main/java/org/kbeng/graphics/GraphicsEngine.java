package org.kbeng.graphics;

import org.kbeng.wrld.GameMaster;
import org.kbeng.wrld.World;

/**
 * Encapsula los recursos y el pipeline gráfico de runtime.
 *
 * <p>Agrupa los objetos gráficos de alto nivel movidos fuera de
 * {@code GameMaster}: framebuffers, sombras, render de items y lluvia.
 */
public final class GraphicsEngine {
    private final GameMaster gameMaster;

    private Framebuffer sceneFbo;
    private Framebuffer blurFbo;
    private ShadowMap shadowMap;
    private ItemRenderer itemRenderer;
    private RainEngine rainEngine;

    /**
     * Crea un motor gráfico ligado a un orquestador de juego.
     *
     * @param gameMaster orquestador principal
     */
    public GraphicsEngine(GameMaster gameMaster) {
        this.gameMaster = gameMaster;
    }

    /**
     * Inicializa recursos gráficos persistentes del runtime.
     *
     * @param width  ancho inicial del viewport
     * @param height alto inicial del viewport
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
     * Avanza el estado temporal del sistema de lluvia.
     *
     * @param delta tiempo de frame en segundos
     */
    public void updateRain(float delta) {
        if (rainEngine != null) rainEngine.update(delta);
    }

    /**
     * Ejecuta el pipeline completo de render del frame.
     *
     * @param world  mundo actual
     * @param camera cámara activa
     */
    public void render(World world, CameraView camera) {
        if (world == null || camera == null || gameMaster.getChunkManager() == null) return;
        GameRenderer.gamr.render(gameMaster, gameMaster.getChunkManager().getChunkMeshes());
        org.kbeng.ui.GameUIService.ui.render(gameMaster.isHUDShown(), gameMaster);
        GameRenderer.gamr.renderPaper(gameMaster);
    }

    /**
     * Recrea framebuffers cuando cambia el tamaño de ventana.
     *
     * @param width  nuevo ancho
     * @param height nuevo alto
     */
    public void onResize(int width, int height) {
        if (sceneFbo == null || blurFbo == null) return;
        sceneFbo.dispose();
        blurFbo.dispose();
        sceneFbo = new Framebuffer(width, height);
        blurFbo = new Framebuffer(width, height);
    }

    /**
     * Libera todos los recursos gráficos administrados.
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
     * Devuelve el framebuffer principal de escena.
     *
     * @return FBO de escena
     */
    public Framebuffer getSceneFbo() {
        return sceneFbo;
    }

    /**
     * Devuelve el framebuffer auxiliar de blur/postproceso.
     *
     * @return FBO de blur
     */
    public Framebuffer getBlurFbo() {
        return blurFbo;
    }

    /**
     * Devuelve el shadow map direccional activo.
     *
     * @return shadow map
     */
    public ShadowMap getShadowMap() {
        return shadowMap;
    }

    /**
     * Devuelve el renderer de ítems del mundo.
     *
     * @return renderer de ítems
     */
    public ItemRenderer getItemRenderer() {
        return itemRenderer;
    }

    /**
     * Devuelve el motor de lluvia.
     *
     * @return rain engine
     */
    public RainEngine getRainEngine() {
        return rainEngine;
    }
}

