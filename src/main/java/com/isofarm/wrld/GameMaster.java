package com.isofarm.wrld;

import com.isofarm.craft.Recipe;
import com.isofarm.craft.RecipeRegistry;
import com.isofarm.data.*;
import com.isofarm.entity.*;
import com.isofarm.graphics.*;
import com.isofarm.item.iBlock;
import com.isofarm.ui.Frontend;
import com.isofarm.ui.GameUIService;
import com.isofarm.ui.UIManager;
import com.isofarm.input.*;
import com.isofarm.item.Item;
import com.isofarm.pathfinding.GridPos;
import com.isofarm.service.*;
import com.isofarm.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

/**
 * Encapsulates the state and operations required by game master within the game runtime.
 */
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
    private final List<Entity> entities = new LinkedList<>();
    private final List<Entity> entitiesToAdd = new ArrayList<>();
    private final List<Entity> entitiesToRemove = new ArrayList<>();
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
    private Shop shop;
    private Difficulty difficulty = Difficulty.NORMAL;

    private boolean isChatOpen = false;
    private boolean isInventoryOpen = false;
    private boolean isHUDShown = true;

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
        float totalSteps = 10.0f;
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
        this.shop = new Shop();
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        this.camera = new Camera(windowWidth, windowHeight, Settings.getRenderDistance());

        this.cameraController = new CameraController(camera);
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        notifyProgress(progressCallback, ++currentStep / totalSteps);

        recipes = RecipeRegistry.reg.init();
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        addEntity(Player.plyr);
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        Library.initItems(itemRegistry);
        notifyProgress(progressCallback, ++currentStep / totalSteps);

        Library.initCommands(genDelta, this);
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
                ResourceManager.rem.getMaterialIcons(),
                ResourceManager.rem.getInventoryIcons());

        GameUIService.ui.setShop(shop);

    }

    /**
     * Transfers or creates the relevant entity or item for spawn.
     */
    public void spawn() {
        chunkManager.updateLoadedChunks(0, 0);
        GridPos spawn = world.getHighestY(0.5f, 0.5f);
        float spawnY = spawn.y() + 1.8f;
        Player.plyr.setPosition(0.5f, spawnY, 0.5f);
        camera.setPosition(0.5f, spawnY + 10.0f, 0.5f);
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
        if (entity instanceof WorldItem worldItem) {
            worldItem.setWorld(world);
        }
        if (!entities.contains(entity) && !entitiesToAdd.contains(entity)) {
            entitiesToAdd.add(entity);
        }
    }

    /**
     * Removes the entity.
     * @param entity the {@link Entity} supplied as {@code entity}
     */
    public void removeEntity(Entity entity) {
        if (entity == null) return;
        entitiesToRemove.add(entity);
    }

    /**
     * Updates the entities.
     * @param delta the {@code float} supplied as {@code delta}
     */
    private void updateEntities(float delta) {
        if (!entitiesToAdd.isEmpty()) {
            entities.addAll(entitiesToAdd);
            entitiesToAdd.clear();
        }
        if (!entitiesToRemove.isEmpty()) {
            entities.removeAll(entitiesToRemove);
            entitiesToRemove.clear();
        }

        for (Entity entity : entities) {
            entity.update(HoveredCell.get(this), delta);
            entity.updateEnvironmentalDamage(world, delta);
        }

        entities.removeIf(e -> e != Player.plyr && !e.isAlive());
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
        shop.update(TimeService.ts);
        CropService.cs.update(delta, WeatherService.wes.getWeather());
        TreeService.ts.update(this);
        world.forEachInteractiveBlock(iBlock::animate);
        updateEntities(delta);
        cameraController.update(this, delta);
        ParticleEngine.peng.update(delta);
        StepController.step.update(this, SoundService.fx, delta);
        GameInteraction.gami.update(this, Settings.selectedItem);

        FluidSimulation.updateAll(delta);
        chunkManager.update(Player.plyr.getPosition().x,
                Player.plyr.getPosition().z, delta);

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
}
