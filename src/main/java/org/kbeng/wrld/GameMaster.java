package org.kbeng.wrld;

import org.kbeng.craft.Recipe;
import org.kbeng.craft.RecipeRegistry;
import org.kbeng.data.*;
import org.kbeng.entity.*;
import org.kbeng.graphics.*;
import org.kbeng.input.*;
import org.kbeng.item.Item;
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
 * Represents the game master component of the kbengine runtime.
 *
 * <p>This type centralizes the state, lifecycle and behavior required by its callers,
 * keeping domain rules together with the data they operate on.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li><b>State:</b> Owns the data needed to represent the component consistently.</li>
 *   <li><b>Lifecycle:</b> Exposes the operations used to create, update and release its state.</li>
 *   <li><b>Integration:</b> Coordinates with the surrounding game, rendering or UI systems through its public API.</li>
 * </ul>
 *
 * <p>Callers should use the documented public operations and allow this type to preserve
 * its invariants rather than modifying implementation details directly.
 */
@SuppressWarnings("all")
@Singleton
public class GameMaster {
    public static final GameMaster game = new GameMaster();
    private static final Logger log = LoggerFactory.getLogger(GameMaster.class);
    private final long windowHandle = Intro.getWindow();
    private final World world = World.wrld;
    private final Sun sun = new Sun("Sun");
    private final Moon moon = new Moon("Moon");
    private final CelestialLighting celestialLighting = new CelestialLighting(sun, moon);
    private final UIManager uiManager = Intro.getUiManager();
    private final CommandRegistry commandRegistry = new CommandRegistry();
    private final CommandService commandService = new CommandService(commandRegistry);
    private final ItemRegistry itemRegistry = new ItemRegistry();
    private final RainEngine rainEngine = new RainEngine();
    private final ViewService viewService = new ViewService();
    private List<Entity> entities = new LinkedList<>();
    private List<Recipe> recipes;
    private ShadowMap shadowMap;
    private ChunkManager chunkManager;
    private ItemRenderer itemRenderer;
    private Framebuffer sceneFbo;
    private Framebuffer blurFbo;
    private Camera camera;
    private CameraController cameraController;
    private float windowWidth = K.Window.DEFAULT_WIDTH;
    private float windowHeight = K.Window.DEFAULT_HEIGHT;
    private Difficulty difficulty = Difficulty.NORMAL;

    private boolean isChatOpen = false;
    private boolean isInventoryOpen = false;
    private boolean isBackpackOpen = false;
    private boolean isHUDShown = true;
    private volatile boolean areEntitiesActive;

    private float genDelta;

    /**
     * Creates a new {@link GameMaster} instance.
     */
    private GameMaster() {}

    /**
     * Loads the resources.
     * @param progressCallback the {@link Consumer} supplied as {@code progressCallback}
     */
    public void loadResources(Consumer<Float> progressCallback) {
        float totalSteps = 15.0f;
        int currentStep = 0;

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_MULTISAMPLE);
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        this.shadowMap = new ShadowMap((int) Settings.getShadowMapSize(),
                (int) Settings.getShadowMapSize());
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        this.sceneFbo = new Framebuffer((int) windowWidth, (int) windowHeight);
        this.blurFbo = new Framebuffer((int) windowWidth, (int) windowHeight);
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        notifyProgress(progressCallback, ++currentStep / totalSteps);

        this.chunkManager = new ChunkManager(world, FluidSimulation.forBlock(BlockData.WATER));
        this.itemRenderer = new ItemRenderer();
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        this.camera = new Camera(windowWidth, windowHeight, Settings.getRenderDistance());

        this.cameraController = new CameraController(camera);
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        notifyProgress(progressCallback, ++currentStep / totalSteps);

        recipes = RecipeRegistry.reg.init();
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        addEntity(Player.plyr);
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        NPCService.npcs.init();
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        EnemyService.enms.init();
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        AnimalService.anml.init();
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        BlockRegistry.init();
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        Library.initItems(itemRegistry);
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        Library.initCommands(this);
        notifyProgress(progressCallback, ++currentStep / totalSteps);
    }

    /**
     * Notifies the relevant subsystem about progress.
     * @param callback the {@link Consumer} supplied as {@code callback}
     * @param progress the {@code float} supplied as {@code progress}
     */
    private void notifyProgress(Consumer<Float> callback, float progress) {
        if (callback != null) {
            callback.accept(progress);
        }
    }

    /**
     * Initializes the ui.
     */
    public void initUI() {
        GameUIService.init(this,
                uiManager, ResourceManager.rem.getSeedIcons(), ResourceManager.rem.getCropIcons(),
                ResourceManager.rem.getBlockIcons(), ResourceManager.rem.getToolIcons(),
                ResourceManager.rem.getArmorIcons(),
                ResourceManager.rem.getMaterialIcons(),
                ResourceManager.rem.getInventoryIcons());

        if (NPCService.npcs.getTrader() != null) {
            GameUIService.ui.setTrader(NPCService.npcs.getTrader());
        }
    }

    /**
     * Transfers or creates the relevant entity or item for spawn.
     */
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

    /**
     * Returns the sun.
     * @return the {@link Sun} representing the sun
     */
    public Sun getSun() {
        return sun;
    }

    /**
     * Returns the moon.
     * @return the {@link Moon} representing the moon
     */
    public Moon getMoon() {
        return moon;
    }

    /**
     * Returns the celestial lighting.
     * @return the {@link CelestialLighting} representing the celestial lighting
     */
    public CelestialLighting getCelestialLighting() {
        return celestialLighting;
    }

    /**
     * Returns the shadow map.
     * @return the {@link ShadowMap} representing the shadow map
     */
    public ShadowMap getShadowMap() {
        return shadowMap;
    }

    /**
     * Returns the chunk manager.
     * @return the {@link ChunkManager} representing the chunk manager
     */
    public ChunkManager getChunkManager() {
        return chunkManager;
    }

    /**
     * Returns the item renderer.
     * @return the {@link ItemRenderer} representing the item renderer
     */
    public ItemRenderer getItemRenderer() {
        return itemRenderer;
    }

    /**
     * Returns the scene fbo.
     * @return the {@link Framebuffer} representing the scene fbo
     */
    public Framebuffer getSceneFbo() {
        return sceneFbo;
    }

    /**
     * Returns the rain engine.
     * @return the {@link RainEngine} representing the rain engine
     */
    public RainEngine getRainEngine() {
        return rainEngine;
    }

    /** Returns the active isometric visibility classifier. */
    public ViewService getViewService() {
        return viewService;
    }

    /**
     * Returns the window handle.
     * @return {@code long}; the window handle
     */
    public long getWindowHandle() {
        return windowHandle;
    }

    /**
     * Returns the world.
     * @return the {@link World} representing the world
     */
    public World getWorld() {
        return world;
    }

    /**
     * Returns the command registry.
     * @return the {@link CommandRegistry} representing the command registry
     */
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    /**
     * Returns the item registry.
     * @return the {@link ItemRegistry} representing the item registry
     */
    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    /**
     * Returns the command service.
     * @return the {@link CommandService} representing the command service
     */
    public CommandService getCommandService() {
        return commandService;
    }

    /**
     * Returns the window width.
     * @return {@code float}; the window width
     */
    public float getWindowWidth() {
        return windowWidth;
    }

    /**
     * Returns the window height.
     * @return {@code float}; the window height
     */
    public float getWindowHeight() {
        return windowHeight;
    }

    /**
     * Returns the ortho camera.
     * @return the {@link Camera} representing the ortho camera
     */
    public Camera getCamera() {
        return camera;
    }

    /**
     * Returns the blur fbo.
     * @return the {@link Framebuffer} representing the blur fbo
     */
    public Framebuffer getBlurFbo() {
        return blurFbo;
    }

    /**
     * Checks whether the chat open condition is met.
     * @return {@code true} if chat open; otherwise {@code false}
     */
    public boolean isChatOpen() {
        return isChatOpen;
    }

    /**
     * Sets the chat open.
     * @param isChatOpen the {@code boolean} supplied as {@code isChatOpen}
     */
    public void setChatOpen(boolean isChatOpen) {
        this.isChatOpen = isChatOpen;
    }

    /**
     * Checks whether the inventory open condition is met.
     * @return {@code true} if inventory open; otherwise {@code false}
     */
    public boolean isInventoryOpen() {
        return isInventoryOpen;
    }

    /**
     * Sets the inventory open.
     * @param isInventoryOpen the {@code boolean} supplied as {@code isInventoryOpen}
     */
    public void setInventoryOpen(boolean isInventoryOpen) {
        this.isInventoryOpen = isInventoryOpen;
        if (isInventoryOpen) isBackpackOpen = false;
    }

    /** Returns whether the standalone backpack panel is open. */
    public boolean isBackpackOpen() {
        return isBackpackOpen;
    }

    /** Opens or closes the standalone backpack panel. */
    public void setBackpackOpen(boolean isBackpackOpen) {
        this.isBackpackOpen = isBackpackOpen;
        if (isBackpackOpen) isInventoryOpen = false;
    }

    /**
     * Checks whether the hudshown condition is met.
     * @return {@code true} if hudshown; otherwise {@code false}
     */
    public boolean isHUDShown() {
        return isHUDShown;
    }

    /**
     * Sets the value for {@code isHUDShown}
     * @param isHudShown {@link Boolean}
     * @return {@link Boolean} supplied as {@code isHudShown}
     */
    public boolean setisHudShown(boolean isHudShown) {
        return this.isHUDShown = isHudShown;
    }

    /**
     * Toggles the setting represented by HUD and applies it immediately.
     */
    public void toggleHUD() {
        this.isHUDShown = !isHUDShown;
    }

    /**
     * Returns the crop sprite sheet.
     * @param type the {@link CropType} supplied as {@code type}
     * @return the {@link SpriteSheet} representing the crop sprite sheet
     */
    public SpriteSheet getCropSpriteSheet(CropType type) {
        return ResourceManager.rem.getCropSpritesheets().get(type);
    }

    /**
     * Returns the season.
     * @return the {@link Season} representing the season
     */
    public Season getSeason() {
        return TimeService.ts.getCurrentSeason();
    }

    /**
     * Returns the entities immutable.
     * @return the {@link List} representing the entities immutable
     */
    public List<Entity> getEntitiesImmutable() {
        return List.copyOf(entities);
    }

    /**
     * Returns the entities.
     * @return the {@link List} representing the entities
     */
    public List<Entity> getEntities() {
        return entities;
    }

    /**
     * Returns the recipes.
     * @return the {@link List} representing the recipes
     */
    public List<Recipe> getRecipes() {
        return recipes;
    }

    /**
     * Returns the difficulty.
     * @return the {@link Difficulty} representing the difficulty
     */
    public Difficulty getDifficulty() {
        return difficulty;
    }

    /**
     * Sets the difficulty.
     * @param difficulty the {@link Difficulty} supplied as {@code difficulty}
     */
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    /**
     * Returns the active camera.
     * @return the {@link CameraView} representing the active camera
     */
    public CameraView getActiveCamera() {
        return camera;
    }

    /**
     * Adds the entity.
     * @param entity the {@link Entity} supplied as {@code entity}
     */
    public void addEntity(Entity entity) {
        if (entity == null) return;
        entities.add(entity);
    }

    /**
     * Removes the entity.
     * @param entity the {@link Entity} supplied as {@code entity}
     */
    public void removeEntity(Entity entity) {
        if (entity == null) return;
        entities.remove(entity);
    }

    /**
     * Updates the entities.
     * @param delta the {@code float} supplied as {@code delta}
     */
    private void updateEntities(float delta) {
        if (!areEntitiesActive) return;
        entities.removeIf(entity -> entity != Player.plyr && !entity.isAlive());
        for (Entity entity : entities) {
            entity.update(HoveredCell.get(this), delta);
            entity.updateEnvironmentalDamage(world, delta);
        }

        if (!Player.plyr.isAlive() && Player.plyr.getRespawnTimer() == 0.0f) {
            Player.plyr.respawn();
        }
    }

    /**
     * Returns the gen delta.
     * @return {@code float}; the gen delta
     */
    public float getGenDelta() {
        return genDelta;
    }

    /**
     * Returns the fps.
     * @return the {@link String} representing the fps
     */
    public String getFps() {
        return String.format("%.0f", 1.0f / genDelta) + " FPS";
    }

    /**
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void update(float delta) {
        if (WeatherService.isRaining()) {
            rainEngine.update(delta);
            if (Settings.doEnableMusic()) {
                SoundService.fx.setBackgroundSound(SoundGroup.RAIN);
            } else {
                SoundService.fx.setBackgroundSound(null);
            }
        } else {
            if (Settings.doEnableMusic()) {
                SoundService.fx.setBackgroundSound(SoundGroup.NATURE);
            } else {
                SoundService.fx.setBackgroundSound(null);
            }
        }

        BookService.bs.update();
        GameUIService.ui.update(delta);

        genDelta = delta;
        TimeService.ts.update(delta, WeatherService.wes);
        float timeOfDay = TimeService.ts.getHour() + (TimeService.ts.getMinute() / 60.0f);
        celestialLighting.update(HoveredCell.get(this), timeOfDay);

        NPC trader = NPCService.npcs.getTrader();
        if (trader != null) {
            trader.updateShop(TimeService.ts);
        }

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
        chunkManager.update(Player.plyr.getPosition().x(),
                Player.plyr.getPosition().z(), delta);

        Mouse.update();
        Keyboard.update();
        Joystick.update();
    }

    /**
     * Renders this object in the requested render pass.
     */
    public void render() {
        GameRenderer.gamr.render(this, chunkManager.getChunkMeshes());
        GameUIService.ui.render(isHUDShown(), this);
        GameRenderer.gamr.renderPaper(this);
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        chunkManager.dispose();
        ResourceManager.rem.dispose();
        itemRenderer.dispose();

        Frontend.dispose();
        sceneFbo.dispose();
        blurFbo.dispose();

        rainEngine.dispose();
        shadowMap.dispose();
        PointShadowSystem.sys.dispose();

        cameraController.release(this);
        SoundService.fx.cleanup();
        log.info("GameMaster resources successfully cleaned up");
    }

    /**
     * Returns the world item.
     * @param item the {@link Item} supplied as {@code item}
     * @return the {@link WorldItem} representing the world item
     */
    public WorldItem getWorldItem(Item item) {
        return entities.stream()
                .filter(WorldItem.class::isInstance)
                .map(WorldItem.class::cast)
                .filter(worldItem -> worldItem.getItem().equals(item))
                .findFirst()
                .orElse(null);
    }

    /**
     * Toggles the setting represented by inventory and applies it immediately.
     */
    public void toggleInventory() {
        setInventoryOpen(!isInventoryOpen());
    }

    /**
     * Handles resize and updates the affected state.
     * @param newWidth the {@code int} supplied as {@code newWidth}
     * @param newHeight the {@code int} supplied as {@code newHeight}
     */
    public void onResize(int newWidth, int newHeight) {
        this.windowWidth = newWidth;
        this.windowHeight = newHeight;

        if (camera != null) {
            camera.updateProjection(newWidth, newHeight,
                    Settings.getRenderDistance());
        }

        if (sceneFbo != null) {
            sceneFbo.dispose();
            sceneFbo = new Framebuffer(newWidth, newHeight);
        }

        if (blurFbo != null) {
            blurFbo.dispose();
            blurFbo = new Framebuffer(newWidth, newHeight);
        }

        if (uiManager != null) {
            uiManager.resize(newWidth, newHeight);
            Frontend.resize(newWidth, newHeight);
        }

        if (GameUIService.ui != null) {
            GameUIService.ui.onResize(newWidth, newHeight);
        }
        ToastFactory.onResize(newWidth);
    }

    /**
     * Rebuilds chunk mesh at from the authoritative runtime state.
     * @param worldX the {@code int} supplied as {@code worldX}
     * @param worldZ the {@code int} supplied as {@code worldZ}
     */
    public void rebuildChunkMeshAt(int worldX, int worldZ) {
        chunkManager.rebuildChunkMeshAt(worldX, worldZ);
        int localX = Math.floorMod(worldX, Chunk.SIZE_X);
        int localZ = Math.floorMod(worldZ, Chunk.SIZE_Z);

        if (localX == 0) {
            chunkManager.rebuildChunkMeshAt(worldX - 1, worldZ);
        }

        if (localX == Chunk.SIZE_X - 1) {
            chunkManager.rebuildChunkMeshAt(worldX + 1, worldZ);
        }

        if (localZ == 0) {
            chunkManager.rebuildChunkMeshAt(worldX, worldZ - 1);
        }

        if (localZ == Chunk.SIZE_Z - 1) {
            chunkManager.rebuildChunkMeshAt(worldX, worldZ + 1);
        }
    }

    /**
     * Rebuilds chunk mesh at from the authoritative runtime state.
     * @param pos the {@link BlockPos} supplied as {@code pos}
     */
    public void rebuildChunkMeshAt(BlockPos pos) {
        rebuildChunkMeshAt(pos.x(), pos.z());
    }

    /**
     * Rebuilds the chunks around a breaking block immediately.
     * @param worldX the world x coordinate of the breaking block
     * @param worldZ the world z coordinate of the breaking block
     */
    public void rebuildBreakingChunkMeshAt(int worldX, int worldZ) {
        chunkManager.rebuildBreakingChunkMeshAt(worldX, worldZ);
    }
}
