package org.kbeng.games.rpg.wrld;

import org.kbeng.engine.graphics.*;
import org.kbeng.engine.input.*;
import org.kbeng.engine.ui.*;
import org.kbeng.games.rpg.data.*;
import org.kbeng.games.rpg.graphics.*;
import org.kbeng.games.rpg.service.*;
import org.kbeng.games.rpg.input.CameraController;
import org.kbeng.games.rpg.input.StepController;
import org.kbeng.games.rpg.utils.HoveredCell;
import org.kbeng.engine.utils.K;
import org.kbeng.engine.utils.Local;
import org.kbeng.engine.utils.Settings;
import org.kbeng.engine.utils.ToastFactory;
import org.kbeng.games.rpg.craft.RecipeRegistry;
import org.kbeng.games.rpg.entity.Entity;
import org.kbeng.games.rpg.entity.NPC;
import org.kbeng.games.rpg.entity.Player;
import org.kbeng.games.rpg.entity.pathfinding.GridPos;
import org.kbeng.games.rpg.input.GameInteraction;
import org.kbeng.games.rpg.item.iBlock;
import org.joml.Vector3f;
import org.kbeng.games.rpg.ui.BookUI;
import org.kbeng.games.rpg.ui.GameUIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

/**
 * Central runtime orchestrator for the local game session.
 * <p>This class keeps the frame loop cohesive while delegating specialized work
 * to focused subsystems:
 * <ul>
 *   <li>{@link EnvironmentSystem} drives time, weather transitions, and celestial light.</li>
 *   <li>{@link SoundListener} applies weather-aware ambient audio decisions.</li>
 *   <li>{@link GraphicsEngine} owns framebuffers, shadow resources, and rendering lifecycle.</li>
 * </ul>
 * <p>The orchestrator remains responsible for simulation ordering, entity updates,
 * input flushing, and high-level world coordination.
 */
@Singleton
public final class GameMaster implements Application {
    public static GameMaster game;
    private static final Logger log = LoggerFactory.getLogger(GameMaster.class);
    private long windowHandle;
    private final World world = World.wrld;
    private UIManager uiManager;
    private final CommandRegistry commandRegistry = new CommandRegistry();
    private final CommandService commandService = new CommandService(commandRegistry);
    private final ItemRegistry itemRegistry = new ItemRegistry();
    private final ViewService viewService = new ViewService();
    private final EnvironmentSystem environmentSystem = new EnvironmentSystem();
    private final SoundListener soundListener = new SoundListener();
    private final GraphicsEngine renderEngine = new GraphicsEngine(this);
    private final List<Entity> entities = new LinkedList<>();
    private ChunkManager chunkManager;
    private Camera camera;
    private FirstPersonCamera firstPersonCamera;
    private CameraController cameraController;
    private boolean firstPersonCameraActive;
    private float windowWidth = K.Window.DEFAULT_WIDTH;
    private float windowHeight = K.Window.DEFAULT_HEIGHT;
    private Difficulty difficulty = Difficulty.NORMAL;
    private boolean isChatOpen;
    private boolean isInventoryOpen;
    private boolean isBackpackOpen;
    private boolean isHUDShown = true;
    private volatile boolean areEntitiesActive;
    private float genDelta;
    private UILabel namePrompt;
    private UITextField nameField;

    public GameMaster() {
        if (game != null) throw new IllegalStateException("Only one GameMaster may be active");
        game = this;
    }

    @Override
    public Configuration configuration() {
        return new Configuration("RPG", (int) K.Window.DEFAULT_WIDTH,
                (int) K.Window.DEFAULT_HEIGHT, true, K.Paths.LOGO,
                K.Paths.CURSOR_POINTER, List.of(
                new WindowIcon(16, "/assets/ui/iconx16.png"),
                new WindowIcon(32, "/assets/ui/iconx32.png"),
                new WindowIcon(64, "/assets/ui/iconx64.png"),
                new WindowIcon(128, "/assets/ui/iconx128.png"),
                new WindowIcon(256, "/assets/ui/iconx256.png")));
    }

    /**
     * Loads the RPG and reports game-specific startup work to the generic intro.
     */
    @Override
    public void initialize(Context context, Consumer<LoadingProgress> progressCallback) {
        windowHandle = context.windowHandle();
        uiManager = context.uiManager();
        windowWidth = context.framebufferWidth();
        windowHeight = context.framebufferHeight();
        UIButton.setDefaultClickFeedback(() ->
                SoundService.fx.playUseSound(SoundGroup.BUTTON));
        int renderDistance = Settings.getRenderDistance();
        int visibleChunks = countVisibleChunks(renderDistance);
        int totalTasks = 10 + visibleChunks * 2 + 2;
        int[] completedTasks = {0};
        Consumer<String> report = status -> {
            completedTasks[0]++;
            if (progressCallback != null) {
                progressCallback.accept(new LoadingProgress(
                        (float) completedTasks[0] / totalTasks, status));
            }
        };

        loadResources(ignored -> report.accept(Local.lang.t("engine.loading")));
        for (int chunkX = -renderDistance; chunkX <= renderDistance; chunkX++) {
            for (int chunkZ = -renderDistance; chunkZ <= renderDistance; chunkZ++) {
                if (!isChunkVisible(chunkX, chunkZ, renderDistance)) continue;
                glfwPollEvents();
                if (glfwWindowShouldClose(windowHandle)) return;
                chunkManager.getGenerator().generateChunk(chunkX, chunkZ);
                report.accept(String.format(Local.lang.f("engine.generating_terrain",
                        chunkX, chunkZ)));
            }
        }

        for (int chunkX = -renderDistance; chunkX <= renderDistance; chunkX++) {
            for (int chunkZ = -renderDistance; chunkZ <= renderDistance; chunkZ++) {
                if (!isChunkVisible(chunkX, chunkZ, renderDistance)) continue;
                glfwPollEvents();
                if (glfwWindowShouldClose(windowHandle)) return;
                chunkManager.buildSingleChunkMesh(chunkX, chunkZ);
                report.accept(String.format(Local.lang.f("engine.building_meshes",
                        chunkX, chunkZ)));
            }
        }

        chunkManager.setLastPlayerChunkX(0);
        chunkManager.setLastPlayerChunkZ(0);
        spawn();
        report.accept(Local.lang.t("engine.spawning_player"));
        initUI();
        GameUIService.ui.getHotbarUI().hide();
        report.accept(Local.lang.t("engine.post_processing"));
    }

    @Override
    public void start() {
        requestPlayerName();
        if (!glfwWindowShouldClose(windowHandle)) GameUIService.ui.getHotbarUI().show();
    }

    private boolean isChunkVisible(int chunkX, int chunkZ, int renderDistance) {
        int radiusSquared = renderDistance * renderDistance;
        return chunkX * chunkX + chunkZ * chunkZ <= radiusSquared;
    }

    private int countVisibleChunks(int renderDistance) {
        int visibleChunks = 0;
        for (int chunkX = -renderDistance; chunkX <= renderDistance; chunkX++) {
            for (int chunkZ = -renderDistance; chunkZ <= renderDistance; chunkZ++) {
                if (isChunkVisible(chunkX, chunkZ, renderDistance)) visibleChunks++;
            }
        }
        return visibleChunks;
    }

    /**
     * Initializes shared runtime services and GPU resources for gameplay.
     * <p>Initialization order is intentional: OpenGL state and render resources
     * are prepared first, then chunk/camera systems, then registries/services,
     * and finally core entities.
     * @param progressCallback optional loading progress sink in range {@code [0, 1]}
     */
    private void loadResources(Consumer<Float> progressCallback) {
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
        firstPersonCamera = new FirstPersonCamera(windowWidth, windowHeight,
                Settings.getFov(), 0.05f, (Settings.getRenderDistance() + 2) * 16.0f);
        cameraController = new CameraController(camera, firstPersonCamera);
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

    /**
     * Spawns player and AI actors at world start.
     * <p>Entity updates are paused during spawn to avoid transient collisions
     * while positions are being assigned.
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

    public long getWindowHandle() { return windowHandle; }
    public float getWindowWidth() { return windowWidth; }
    public float getWindowHeight() { return windowHeight; }
    public World getWorld() { return world; }
    public ChunkManager getChunkManager() { return chunkManager; }
    public Camera getCamera() { return camera; }
    public CameraView getActiveCamera() { return firstPersonCameraActive ? firstPersonCamera : camera; }

    /**
     * Returns whether the local scene is currently observed through the
     * player's first-person camera. This is the authoritative mode flag used by
     * input and presentation code; callers do not need to inspect or cast the
     * active {@link CameraView}.
     *
     * @return {@code true} while the perspective eye camera is active
     */
    public boolean isFirstPersonCameraActive() { return firstPersonCameraActive; }

    /**
     * Exchanges the orthographic and first-person cameras without replacing
     * either instance. Keeping both cameras alive preserves each view's pitch,
     * zoom and projection state when the player switches back.
     */
    public void toggleCameraMode() { firstPersonCameraActive = !firstPersonCameraActive; }

    /**
     * Returns the horizontal screen coordinate used to aim. First person is
     * locked to the viewport center represented by its crosshair; the detached
     * orthographic camera continues to use the hardware cursor.
     *
     * @return aim coordinate in framebuffer pixels
     */
    public float getAimScreenX() {
        return firstPersonCameraActive ? windowWidth * 0.5f : Mouse.getX();
    }

    /**
     * Returns the vertical screen coordinate used to aim. First person is
     * locked to the viewport center represented by its crosshair; the detached
     * orthographic camera continues to use the hardware cursor.
     *
     * @return aim coordinate in framebuffer pixels
     */
    public float getAimScreenY() {
        return firstPersonCameraActive ? windowHeight * 0.5f : Mouse.getY();
    }
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

    /**
     * Advances one full simulation frame.
     * <p>Execution order is stable by design: audio/weather, UI systems, environment,
     * entity/world simulation, camera/input interaction, then physics/chunk refresh and
     * input edge-state rollover.
     * @param delta elapsed frame time in seconds
     */
    @Override
    public void update(float delta) {
        if (Controls.isPressed(ControlAction.CHANGE_LANGUAGE)) {
            Local.lang.nextLanguage();
            if (BookService.bs.isOpen() && BookService.bs.getOpenedBook() != null) {
                BookUI.bui.reload(BookService.bs.getOpenedBook());
            }
            ToastFactory.reload();
            ToastFactory.success(Local.lang.f("engine.language_changed",
                    Local.lang.getCurrentLanguage().getName()));
        }
        if (Controls.isPressed(ControlAction.SHOW_LANGUAGE)) {
            ToastFactory.info(Local.lang.f("engine.current_language",
                    Local.lang.getCurrentLanguage().getName()));
        }
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
        GameInteraction.gami.update(this, ItemSelection.selectedItem);
        FluidSimulation.updateAll(delta);
        chunkManager.update(Player.plyr.getPosition().x(), Player.plyr.getPosition().z(), delta);
        Mouse.update();
        Keyboard.update();
        Joystick.update();
    }

    /**
     * Renders the complete frame through the delegated graphics engine.
     */
    @Override
    public void render() {
        Vector3f skyColor = TimeService.getSkyColor();
        glClearColor(skyColor.x, skyColor.y, skyColor.z, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        renderEngine.render(world, getActiveCamera());
    }

    /**
     * Releases runtime resources in a safe teardown sequence.
     */
    @Override
    public void dispose() {
        if (chunkManager != null) chunkManager.dispose();
        Frontend.dispose();
        renderEngine.dispose();
        if (cameraController != null) cameraController.release(this);
        SoundService.fx.cleanup();
        game = null;
        log.info("GameMaster resources successfully cleaned up");
    }

    /**
     * Propagates a window resize through camera, render targets, and UI layout.
     * @param newWidth  framebuffer width in pixels
     * @param newHeight framebuffer height in pixels
     */
    @Override
    public void onResize(int newWidth, int newHeight) {
        windowWidth = newWidth;
        windowHeight = newHeight;
        if (camera != null) camera.updateProjection(newWidth, newHeight, Settings.getRenderDistance());
        if (firstPersonCamera != null) firstPersonCamera.updateProjection(newWidth, newHeight);
        renderEngine.onResize(newWidth, newHeight);
        if (uiManager != null) { uiManager.resize(newWidth, newHeight); Frontend.resize(newWidth, newHeight); }
        if (GameUIService.ui != null) GameUIService.ui.onResize(newWidth, newHeight);
        ToastFactory.onResize(newWidth);
        repositionNamePrompt();
    }

    private void requestPlayerName() {
        namePrompt = new UILabel(0.0f, 0.0f, 360.0f, 30.0f,
                Local.lang.t("intro.who_are_you"));
        namePrompt.setHorizontalAlignment(UILabel.HorizontalAlignment.CENTER);
        nameField = new UITextField(0.0f, 0.0f, 360.0f, 40.0f);
        nameField.setMaxLength(24);
        uiManager.getRoot().addChild(namePrompt);
        uiManager.getRoot().addChild(nameField);
        repositionNamePrompt();
        uiManager.setFocusedElement(nameField);
        Keyboard.update();

        while (!glfwWindowShouldClose(windowHandle)) {
            glfwPollEvents();
            renderNamePromptFrame();
            boolean submitted = Keyboard.isKeyPressed(Keyboard.KEY_ENTER)
                    || Keyboard.isKeyPressed(Keyboard.KEY_KP_ENTER);
            String playerName = nameField.getText().trim();
            if (submitted && !playerName.isEmpty()) {
                Player.plyr.setName(playerName);
                ToastFactory.info(Local.lang.f("toast.open_inventory", playerName));
                Mouse.update();
                Keyboard.update();
                break;
            }
            Mouse.update();
            Keyboard.update();
        }

        uiManager.clearFocus();
        uiManager.getRoot().removeChild(namePrompt);
        uiManager.getRoot().removeChild(nameField);
        namePrompt.dispose();
        nameField.dispose();
        namePrompt = null;
        nameField = null;
    }

    private void renderNamePromptFrame() {
        glViewport(0, 0, (int) windowWidth, (int) windowHeight);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glClearColor(0.15f, 0.15f, 0.15f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        uiManager.update(0.016f);
        Frontend.begin((int) windowWidth, (int) windowHeight);
        uiManager.render();
        Frontend.end();
        glfwSwapBuffers(windowHandle);
        glFlush();
    }

    private void repositionNamePrompt() {
        if (namePrompt == null || nameField == null) return;
        float centerX = (windowWidth - nameField.getWidth()) / 2.0f;
        float centerY = (windowHeight - nameField.getHeight()) / 2.0f;
        namePrompt.setPosition(centerX, centerY - namePrompt.getHeight() - 12.0f);
        nameField.setPosition(centerX, centerY);
    }

    /**
     * Rebuilds the mesh for the target chunk and any affected neighbors.
     * <p>Neighbor rebuilds are required when a changed block touches a chunk border,
     * ensuring face visibility and lighting stay consistent across chunk seams.
     * @param worldX world-space X coordinate of the changed block/cell
     * @param worldZ world-space Z coordinate of the changed block/cell
     */
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
