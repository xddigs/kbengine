package org.kbeng.wrld;
import org.kbeng.craft.RecipeRegistry;
import org.kbeng.data.*;
import org.kbeng.entity.*;
import org.kbeng.graphics.*;
import org.kbeng.input.*;
import org.kbeng.item.iBlock;
import org.kbeng.pathfinding.GridPos;
import org.kbeng.service.*;
import org.kbeng.ui.Frontend;
import org.kbeng.ui.GameUIService;
import org.kbeng.ui.UIManager;
import org.kbeng.utils.HoveredCell;
import org.kbeng.utils.K;
import org.kbeng.utils.Settings;
import org.kbeng.utils.ToastFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

/**
 * Orquestador principal del runtime del juego.
 */
@Singleton
public class GameMaster {
    public static final GameMaster game = new GameMaster();
    private static final Logger log = LoggerFactory.getLogger(GameMaster.class);
    private final long windowHandle = Intro.getWindow();
    private final World world = World.wrld;
    private final UIManager uiManager = Intro.getUiManager();
    private final CommandRegistry commandRegistry = new CommandRegistry();
    private final CommandService commandService = new CommandService(commandRegistry);
    private final ItemRegistry itemRegistry = new ItemRegistry();
    private final ViewService viewService = new ViewService();
    private final EnvironmentSystem environmentSystem = new EnvironmentSystem();
    private final SoundListener soundListener = new SoundListener();
    private final GraphicsEngine renderEngine = new GraphicsEngine(this);
    private List<Entity> entities = new LinkedList<>();
    private ChunkManager chunkManager;
    private Camera camera;
    private CameraController cameraController;
    private float windowWidth = K.Window.DEFAULT_WIDTH;
    private float windowHeight = K.Window.DEFAULT_HEIGHT;
    private Difficulty difficulty = Difficulty.NORMAL;
    private boolean isChatOpen;
    private boolean isInventoryOpen;
    private boolean isBackpackOpen;
    private boolean isHUDShown = true;
    private volatile boolean areEntitiesActive;
    private float genDelta;
    private GameMaster() { }

    /** Inicializa recursos, servicios base y entidades globales. */
    public void loadResources(Consumer<Float> progressCallback) {
        float totalSteps = 10.0f, step = 0.0f;
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_MULTISAMPLE);
        if (progressCallback != null) progressCallback.accept(++step / totalSteps);
        renderEngine.initialize((int) windowWidth, (int) windowHeight);
        if (progressCallback != null) progressCallback.accept(++step / totalSteps);
        chunkManager = new ChunkManager(FluidSimulation.forBlock(BlockData.WATER));
        camera = new Camera(windowWidth, windowHeight, Settings.getRenderDistance());
        cameraController = new CameraController(camera);
        if (progressCallback != null) progressCallback.accept(++step / totalSteps);
        RecipeRegistry.reg.init();
        addEntity(Player.plyr);
        NPCService.npcs.init();
        EnemyService.enms.init();
        AnimalService.anml.init();
        BlockRegistry.init();
        Library.initItems(itemRegistry);
        Library.initCommands(this);
        while (step < totalSteps) {
            if (progressCallback != null) progressCallback.accept(++step / totalSteps);
        }
    }
    public void initUI() {
        GameUIService.init(this, uiManager, ResourceManager.rem.getSeedIcons(),
                ResourceManager.rem.getCropIcons(), ResourceManager.rem.getBlockIcons(),
                ResourceManager.rem.getToolIcons(), ResourceManager.rem.getArmorIcons(),
                ResourceManager.rem.getMaterialIcons(), ResourceManager.rem.getInventoryIcons());
        if (NPCService.npcs.getTrader() != null) GameUIService.ui.setTrader(NPCService.npcs.getTrader());
    }
    public synchronized void spawn() {
        areEntitiesActive = false;
        chunkManager.updateLoadedChunks(0, 0);
        GridPos spawn = world.getHighestY(0.5f, 0.5f);
        float spawnY = spawn.y() + 1.8f;
        Player.plyr.setPosition(0.5f, spawnY, 0.5f);
        NPCService.npcs.spawn();
        EnemyService.enms.spawn();
        camera.setPosition(0.5f, spawnY + 10.0f, 0.5f);
        areEntitiesActive = true;
    }
    public World getWorld() { return world; }
    public ChunkManager getChunkManager() { return chunkManager; }
    public Camera getCamera() { return camera; }
    public CameraView getActiveCamera() { return camera; }
    public long getWindowHandle() { return windowHandle; }
    public float getWindowWidth() { return windowWidth; }
    public float getWindowHeight() { return windowHeight; }
    public CommandRegistry getCommandRegistry() { return commandRegistry; }
    public ItemRegistry getItemRegistry() { return itemRegistry; }
    public CommandService getCommandService() { return commandService; }
    public ViewService getViewService() { return viewService; }
    public CelestialLighting getCelestialLighting() { return environmentSystem.getCelestialLighting(); }
    public ShadowMap getShadowMap() { return renderEngine.getShadowMap(); }
    public Framebuffer getSceneFbo() { return renderEngine.getSceneFbo(); }
    public Framebuffer getBlurFbo() { return renderEngine.getBlurFbo(); }
    public ItemRenderer getItemRenderer() { return renderEngine.getItemRenderer(); }
    public RainEngine getRainEngine() { return renderEngine.getRainEngine(); }
    public List<Entity> getEntities() { return entities; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public boolean isChatOpen() { return isChatOpen; }
    public void setChatOpen(boolean chatOpen) { isChatOpen = chatOpen; }
    public boolean isInventoryOpen() { return isInventoryOpen; }
    public boolean isBackpackOpen() { return isBackpackOpen; }
    public boolean isHUDShown() { return isHUDShown; }
    public void toggleHUD() { isHUDShown = !isHUDShown; }
    public float getGenDelta() { return genDelta; }
    public String getFps() { return String.format("%.0f", 1.0f / genDelta) + " FPS"; }
    public void setInventoryOpen(boolean inventoryOpen) { isInventoryOpen = inventoryOpen; if (inventoryOpen) isBackpackOpen = false; }
    public void setBackpackOpen(boolean backpackOpen) { isBackpackOpen = backpackOpen; if (backpackOpen) isInventoryOpen = false; }
    public void toggleInventory() { setInventoryOpen(!isInventoryOpen()); }
    public void addEntity(Entity entity) { if (entity != null) entities.add(entity); }
    public void removeEntity(Entity entity) { if (entity != null) entities.remove(entity); }
    public SpriteSheet getCropSpriteSheet(CropType type) { return ResourceManager.rem.getCropSpritesheets().get(type); }
    private void updateEntities(float delta) {
        if (!areEntitiesActive) return;
        entities.removeIf(entity -> entity != Player.plyr && !entity.isAlive());
        for (Entity entity : List.copyOf(entities)) { entity.update(HoveredCell.get(this), delta); entity.updateEnvironmentalDamage(world, delta); }
        entities.removeIf(entity -> entity != Player.plyr && !entity.isAlive());
        if (!Player.plyr.isAlive() && Player.plyr.getRespawnTimer() == 0.0f) Player.plyr.respawn();
    }

    /** Avanza un frame de simulación y coordina todos los sistemas. */
    public void update(float delta) {
        soundListener.update(delta, renderEngine);
        BookService.bs.update();
        GameUIService.ui.update(delta);
        genDelta = delta;
        environmentSystem.update(HoveredCell.get(this), delta);
        NPC trader = NPCService.npcs.getTrader(); if (trader != null) trader.updateShop(TimeService.ts);
        CropService.cs.update(delta, WeatherService.wes.getWeather());
        TreeService.ts.update(this);
        world.forEachInteractiveBlock(iBlock::animate);
        updateEntities(delta);
        viewService.update(world, Player.plyr);
        cameraController.update(this, delta);
        ParticleEngine.peng.update(delta);
        StepController.step.update(this, SoundService.fx, delta);
        GameInteraction.gami.update(this, Settings.selectedItem);
        FluidSimulation.updateAll(delta);
        chunkManager.update(Player.plyr.getPosition().x(), Player.plyr.getPosition().z(), delta);
        Mouse.update();
        Keyboard.update();
        Joystick.update();
    }

    /** Renderiza el frame completo mediante el motor gráfico delegado. */
    public void render() { renderEngine.render(world, camera); }
    public void dispose() {
        chunkManager.dispose();
        Frontend.dispose();
        renderEngine.dispose();
        cameraController.release(this);
        SoundService.fx.cleanup();
        log.info("GameMaster resources successfully cleaned up");
    }
    public void onResize(int newWidth, int newHeight) {
        windowWidth = newWidth;
        windowHeight = newHeight;
        if (camera != null) camera.updateProjection(newWidth, newHeight, Settings.getRenderDistance());
        renderEngine.onResize(newWidth, newHeight);
        if (uiManager != null) { uiManager.resize(newWidth, newHeight); Frontend.resize(newWidth, newHeight); }
        if (GameUIService.ui != null) GameUIService.ui.onResize(newWidth, newHeight);
        ToastFactory.onResize(newWidth);
    }
    public void rebuildChunkMeshAt(int worldX, int worldZ) {
        chunkManager.rebuildChunkMeshAt(worldX, worldZ);
        int localX = Math.floorMod(worldX, Chunk.SIZE_X);
        int localZ = Math.floorMod(worldZ, Chunk.SIZE_Z);
        if (localX == 0) chunkManager.rebuildChunkMeshAt(worldX - 1, worldZ);
        if (localX == Chunk.SIZE_X - 1) chunkManager.rebuildChunkMeshAt(worldX + 1, worldZ);
        if (localZ == 0) chunkManager.rebuildChunkMeshAt(worldX, worldZ - 1);
        if (localZ == Chunk.SIZE_Z - 1) chunkManager.rebuildChunkMeshAt(worldX, worldZ + 1);
    }
    public void rebuildChunkMeshAt(BlockPos pos) { rebuildChunkMeshAt(pos.x(), pos.z()); }
    public void rebuildBreakingChunkMeshAt(int worldX, int worldZ) { chunkManager.rebuildBreakingChunkMeshAt(worldX, worldZ); }
}
