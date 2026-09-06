package com.isofarm.graphics;

import com.isofarm.data.*;
import com.isofarm.graphics.gltf.GLTFLoader;
import com.isofarm.graphics.gltf.GLTFModel;
import com.isofarm.item.*;
import com.isofarm.utils.K;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the state and operations required by resource manager within the game runtime.
 */
@Singleton
public class ResourceManager {
    public static final ResourceManager rem = new ResourceManager();
    private static final Logger log = LoggerFactory.getLogger(ResourceManager.class);

    private static final SpriteSheet seedIcons = new SpriteSheet(K.Paths.SEED_ICONS, K.UI.ICON_SEED_SEEDS_COLS, 1);
    private static final SpriteSheet cropIcons = new SpriteSheet(K.Paths.CROP_ICONS, K.UI.ICON_SEED_CROPS_COLS, 1);
    private static final SpriteSheet toolIcons = new SpriteSheet(K.Paths.TOOL_ICONS, K.UI.ICON_TOOL_COLS, K.UI.ICON_TOOL_ROWS);
    private static final SpriteSheet blockIcons = new SpriteSheet(K.Paths.BLOCK_ICONS, K.UI.ICON_BLOCK_COLS, K.UI.ICON_BLOCK_ROWS);
    private static final SpriteSheet torchIcons = new SpriteSheet(K.Paths.TORCH_ICONS, K.UI.TORCH_COLS, 1);
    private static final SpriteSheet materialIcons = new SpriteSheet(K.Paths.MATERIAL_ICONS, K.UI.ICON_MATERIAL_COLS, K.UI.ICON_MATERIAL_ROWS);
    private static final SpriteSheet usablesIcons = new SpriteSheet(K.Paths.USABLES_ICONS, K.UI.ICON_USABLES_COLS, 1);
    private static final SpriteSheet inventoryIcons = new SpriteSheet(K.Paths.INVENTORY_ICONS, K.UI.ICON_INV_COLS, 1);
    private static final SpriteSheet bookAnimationSheet = new SpriteSheet(K.Paths.BOOK_ANIMATION, 16, 1);
    private static final SpriteSheet bookSortNameIcon = new SpriteSheet(K.Paths.BOOK_SORT_NAME, 1, 1);
    private static final SpriteSheet bookSortTypeIcon = new SpriteSheet(K.Paths.BOOK_SORT_TYPE, 1, 1);
    private static final SpriteSheet bookHomeCraftings = new SpriteSheet(K.Paths.BOOK_LOCAL_CRAFTINGS, 2, 1);
    private static final SpriteSheet bookCloseIcon = new SpriteSheet(K.Paths.BOOK_CLOSE, 1, 1);
    private static final SpriteSheet heartsSpriteSheet = new SpriteSheet(K.Paths.HEARTS_SPRITESHEET, 1, K.UI.ICON_HEARTS_ROWS);
    private static final SpriteSheet destroyTexture = new SpriteSheet(K.Paths.DESTROY_STAGES, K.UI.DESTROY_FRAMES, 1);

    private static final SpriteSheet wheat = new SpriteSheet(K.Paths.WHEAT_TEXTURE, K.Render.CROP_TOTAL_FRAMES, 1);
    private static final SpriteSheet carrot = new SpriteSheet(K.Paths.CARROT_TEXTURE, K.Render.CROP_TOTAL_FRAMES, 1);
    private static final SpriteSheet potato = new SpriteSheet(K.Paths.POTATO_TEXTURE, K.Render.CROP_TOTAL_FRAMES, 1);
    private static final SpriteSheet beetroot = new SpriteSheet(K.Paths.BEETROOT_TEXTURE, K.Render.CROP_TOTAL_FRAMES, 1);
    private static final SpriteSheet sugarCane = new SpriteSheet(K.Paths.SUGAR_CANE_TEXTURE, K.Render.CROP_TOTAL_FRAMES, 1);

    private static final GLTFModel playerModel = GLTFLoader.load(K.Paths.PLAYER_MODEL);

    private static final Map<BlockData, GLTFModel> blockModels = new LinkedHashMap<>();
    private static final Map<CropType, SpriteSheet> cropSpritesheets = new EnumMap<>(CropType.class);

    private static final Shader defaultShader = new Shader(K.Paths.DEFAULT_VERT_SHADER, K.Paths.DEFAULT_FRAG_SHADER);
    private static final Shader grassShader = new Shader(K.Paths.GRASS_VERT_SHADER, K.Paths.GRASS_FRAG_SHADER);
    private static final Shader destroyShader = new Shader(K.Paths.DESTROY_VERT_SHADER, K.Paths.DESTROY_FRAG_SHADER);
    private static final Shader rainShader = new Shader(K.Paths.RAIN_VERT_SHADER, K.Paths.RAIN_FRAG_SHADER);
    private static final Shader motionBlurShader = new Shader(K.Paths.MOTION_BLUR_VERT_SHADER, K.Paths.MOTION_BLUR_FRAG_SHADER);
    private static final Shader shadowMapShader = new Shader(K.Paths.SHADOW_VERT_SHADER, K.Paths.SHADOW_FRAG_SHADER);
    private static final Shader pointShadowShader = new Shader(K.Paths.POINT_SHADOW_VERT_SHADER, K.Paths.POINT_SHADOW_FRAG_SHADER);
    private static final Shader blurShader = new Shader(K.Paths.BLUR_VERT_SHADER, K.Paths.BLUR_FRAG_SHADER);
    private final Vector3f grassTint = new Vector3f(1.0f, 1.0f, 1.0f);

    private static final Mesh screenQuadMesh = Mesh.screenQuad();
    private static final Mesh blockMesh = Mesh.createMesh(K.World.DEFAULT_BLOCK_DEPTH);
    private static final Mesh selectionMesh = Mesh.selection();
    private static final Map<BlockShape, Mesh> shapedSelectionMeshes = createShapedSelectionMeshes();
    private static final Mesh spriteMesh = Mesh.createCrop();
    private static final Mesh flowerMesh = Mesh.createCrossMesh();
    private static final Mesh playerMesh = Mesh.quadVertical();
    private static final Mesh destroyOverlayMesh = Mesh.createDestroyOverlayMesh();
    private static final Texture backgroundUI = new Texture(K.Paths.DEFAULT_BACKGROUND_UI);
    private static final Texture selectorUI = new Texture(K.Paths.DEFAULT_SELECTOR_UI);
    private static final Texture scrollBar = new Texture(K.Paths.SCROLL_BAR);
    private static final Texture scrollKnob = new Texture(K.Paths.SCROLL_KNOB);
    private static final TextureAtlas blocksAtlas;

    static {
        List<String> allPaths = BlockData.getAllTexturePaths();
        blocksAtlas = new TextureAtlas(allPaths, 16, 16);

        for (BlockData block : BlockData.values()) {
            block.initRegions(blocksAtlas);
        }

        for (BlockData block : BlockData.values()) {
            if (block.isInteractive() && block.getModelPath() != null) {
                blockModels.put(block, GLTFLoader.load(block.getModelPath()));
            }
        }

        cropSpritesheets.put(CropType.WHEAT, wheat);
        cropSpritesheets.put(CropType.CARROT, carrot);
        cropSpritesheets.put(CropType.POTATO, potato);
        cropSpritesheets.put(CropType.BEETROOT, beetroot);
        cropSpritesheets.put(CropType.SUGAR_CANE_CROP, sugarCane);
    }

    /**
     * Creates a new {@code ResourceManager} instance.
     */
    private ResourceManager() {
    }

    private static Map<BlockShape, Mesh> createShapedSelectionMeshes() {
        Map<BlockShape, Mesh> meshes = new EnumMap<>(BlockShape.class);
        for (BlockShape shape : BlockShape.values()) {
            if (!shape.isFullCube()) meshes.put(shape, Mesh.selection(shape));
        }
        return meshes;
    }

    /**
     * Returns the item sprite sheet.
     * @param item the {@link Item} supplied as {@code item}
     * @return the {@link SpriteSheet} representing the item sprite sheet
     */
    public static SpriteSheet getItemSpriteSheet(Item item) {
        return switch (item) {
            case Block block when block.getType() == BlockData.TORCH -> torchIcons;
            case Crop crop -> cropSpritesheets.get(crop.getCropType());
            case Produce ignored -> cropIcons;
            case Seed seed when seed.getType() == CropType.SUGAR_CANE_CROP -> sugarCane;
            case Seed ignored -> seedIcons;
            case Tool ignored -> toolIcons;
            case Material ignored -> materialIcons;
            case Usable ignored -> usablesIcons;
            case iBlock ignored -> blockIcons;
            case Block ignored -> blockIcons;
            case null, default -> null;
        };
    }

    /**
     * Returns the material frame.
     * @param material the {@link Material} supplied as {@code material}
     * @return {@code int}; the material frame
     */
    private static int getMaterialFrame(Material material) {
        MaterialID materialID = material.getMaterialID();
        int row = materialID.getRow();

        if (row >= 1) {
            int col = materialID.getId();
            return (row * K.UI.ICON_MATERIAL_COLS) + col;
        }

        if (material.getTier() != null) {
            int baseCol = switch (material.getTier()) {
                case IRON -> 2;
                case PLATINUM -> 4;
                case GOLDEN -> 6;
                case STEEL -> 8;
                case DIAMOND -> 10;
                default -> 0;
            };

            int column = (materialID == MaterialID.INGOT) ? baseCol + 1 : baseCol;
            return (row * K.UI.ICON_MATERIAL_COLS) + column;
        }

        return 0;
    }

    /**
     * Returns the item frame.
     * @param item the {@link Item} supplied as {@code item}
     * @return {@code int}; the item frame
     */
    public static int getItemFrame(Item item) {
        if (item instanceof Block block && block.getType() == BlockData.TORCH) {
            return (int) ((System.nanoTime() / 125_000_000L) % K.UI.TORCH_COLS);
        }

        if (item instanceof iBlock block && block.getType() != null) {
            return (block.getType().getRow() * K.UI.ICON_BLOCK_COLS)
                    + block.getType().getCol() - 1;
        }

        if (item instanceof Block block && block.getType() != null) {
            int col = block.getType().getCol() - 1;
            int row = block.getType().getRow();
            return (row * K.UI.ICON_BLOCK_COLS) + col;
        }

        if (item instanceof Produce produce && produce.getType() != null) {
            return produce.getType().getId();
        }

        if (item instanceof Seed seed && seed.getType() != null) {
            if (seed.getType() == CropType.SUGAR_CANE_CROP) {
                return GrowthStage.SEED.getFrameIndex();
            }
            return seed.getType().getId();
        }

        if (item instanceof Crop crop && crop.getCropType() != null) {
            return crop.getCropType().getId();
        }

        if (item instanceof Tool tool) {
            return (tool.getRow() * K.UI.ICON_TOOL_COLS) + tool.getCol();
        }

        if (item instanceof Usable usable) {
            int col = usable.getUsablesID().getCol();
            int row = usable.getUsablesID().getRow();
            int bucketOffset = 0;
            if (usable instanceof Bucket bucket) {
                if (bucket.getBlockType() == BlockData.WATER) {
                    bucketOffset = 1;
                } else if (bucket.getBlockType() == BlockData.LAVA) {
                    bucketOffset = 2;
                }
            }

            return (row * K.UI.ICON_USABLES_COLS) + col + bucketOffset;
        }

        if (item instanceof Material material) {
            return getMaterialFrame(material);
        }

        return 0;
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        blockMesh.dispose();
        flowerMesh.dispose();
        selectionMesh.dispose();
        shapedSelectionMeshes.values().forEach(Mesh::dispose);
        spriteMesh.dispose();
        screenQuadMesh.dispose();
        playerMesh.dispose();
        destroyOverlayMesh.dispose();

        backgroundUI.dispose();
        selectorUI.dispose();
        scrollBar.dispose();
        scrollKnob.dispose();
        blocksAtlas.dispose();

        wheat.dispose();
        carrot.dispose();
        potato.dispose();
        beetroot.dispose();

        cropIcons.dispose();
        seedIcons.dispose();
        toolIcons.dispose();
        blockIcons.dispose();
        torchIcons.dispose();
        materialIcons.dispose();
        usablesIcons.dispose();
        inventoryIcons.dispose();

        playerModel.dispose();
        blockModels.values().forEach(GLTFModel::dispose);
        blockModels.clear();
        bookAnimationSheet.dispose();
        heartsSpriteSheet.dispose();

        defaultShader.dispose();
        grassShader.dispose();
        destroyShader.dispose();
        motionBlurShader.dispose();
        rainShader.dispose();
        shadowMapShader.dispose();
        pointShadowShader.dispose();
        blurShader.dispose();
    }

    /**
     * Returns the default shader.
     * @return the {@link Shader} representing the default shader
     */
    public Shader getDefaultShader() {
        return defaultShader;
    }

    /**
     * Returns the shader that recolors only the green pixels in grass-block textures.
     * @return the {@link Shader} used for the grass tint pass
     */
    public Shader getGrassShader() {
        return grassShader;
    }

    /**
     * Sets the multiplicative grass tint used by the terrain renderer.
     * A value of {@code (1, 1, 1)} preserves the source texture and is the default.
     * @param tint the biome-specific RGB tint
     */
    public void setGrassTint(Vector3f tint) {
        grassTint.set(tint == null ? new Vector3f(1.0f) : tint);
    }

    /**
     * Returns a copy of the configured grass tint.
     * @return the current grass tint
     */
    public Vector3f getGrassTint() {
        return new Vector3f(grassTint);
    }

    /**
     * Returns the shader dedicated to the block destruction overlay.
     * @return the {@link Shader} used to render block cracks
     */
    public Shader getDestroyShader() {
        return destroyShader;
    }

    /**
     * Returns the rain shader.
     * @return the {@link Shader} representing the rain shader
     */
    public Shader getRainShader() {
        return rainShader;
    }

    /**
     * Returns the motion blur shader.
     * @return the {@link Shader} representing the motion blur shader
     */
    public Shader getMotionBlurShader() {
        return motionBlurShader;
    }

    /**
     * Returns the shadow map shader.
     * @return the {@link Shader} representing the shadow map shader
     */
    public Shader getShadowMapShader() {
        return shadowMapShader;
    }

    /** Returns the shader used to fill point-light depth cubemaps. */
    public Shader getPointShadowShader() { return pointShadowShader; }

    /**
     * Returns the blur shader.
     * @return the {@link Shader} representing the blur shader
     */
    public Shader getBlurShader() {
        return blurShader;
    }

    /**
     * Returns the screen quad mesh.
     * @return the {@link Mesh} representing the screen quad mesh
     */
    public Mesh getScreenQuadMesh() {
        return screenQuadMesh;
    }

    /**
     * Returns the block mesh.
     * @return the {@link Mesh} representing the block mesh
     */
    public Mesh getBlockMesh() {
        return blockMesh;
    }

    /**
     * Returns the flower mesh.
     * @return the {@link Mesh} representing the flower mesh
     */
    public Mesh getFlowerMesh() {
        return flowerMesh;
    }

    /**
     * Returns the destroy overlay mesh.
     * @return the {@link Mesh} representing the destroy overlay mesh
     */
    public Mesh getDestroyOverlayMesh() {
        return destroyOverlayMesh;
    }

    /**
     * Returns the selection mesh.
     * @return the {@link Mesh} representing the selection mesh
     */
    public Mesh getSelectionMesh() {
        return selectionMesh;
    }

    /** Returns an outline mesh matching the supplied voxel block. */
    public Mesh getSelectionMesh(BlockShape shape) {
        if (shape == null) return selectionMesh;
        return shapedSelectionMeshes.getOrDefault(shape, selectionMesh);
    }

    /**
     * Returns the sprite mesh.
     * @return the {@link Mesh} representing the sprite mesh
     */
    public Mesh getSpriteMesh() {
        return spriteMesh;
    }

    /**
     * Returns the player mesh.
     * @return the {@link Mesh} representing the player mesh
     */
    public Mesh getPlayerMesh() {
        return playerMesh;
    }

    /**
     * Returns the background ui.
     * @return the {@link Texture} representing the background ui
     */
    public Texture getBackgroundUI() {
        return backgroundUI;
    }

    /**
     * Returns the selector of the hotbar
     * @return the {@link Texture} representing the hotbar selector
     */
    public Texture getSelectorUI() {
        return selectorUI;
    }

    /**
     * Returns the texture used as the scalable scroll-bar track.
     * @return the {@link Texture} representing the scroll-bar track
     */
    public Texture getScrollBar() {
        return scrollBar;
    }

    /**
     * Returns the texture used by draggable scroll-bar knobs.
     * @return the {@link Texture} representing the scroll-bar knob
     */
    public Texture getScrollKnob() {
        return scrollKnob;
    }

    /**
     * Returns the blocks atlas.
     * @return the {@link TextureAtlas} representing the blocks atlas
     */
    public TextureAtlas getBlocksAtlas() {
        return blocksAtlas;
    }

    /**
     * Returns the destroy texture.
     * @return the {@link SpriteSheet} representing the destroy texture
     */
    public SpriteSheet getDestroyTexture() {
        return destroyTexture;
    }

    /**
     * Returns the player model.
     * @return the {@link GLTFModel} representing the player model
     */
    public GLTFModel getPlayerModel() {
        return playerModel;
    }

    /**
     * Returns the seed icons.
     * @return the {@link SpriteSheet} representing the seed icons
     */
    public SpriteSheet getSeedIcons() {
        return seedIcons;
    }

    /**
     * Returns the crop icons.
     * @return the {@link SpriteSheet} representing the crop icons
     */
    public SpriteSheet getCropIcons() {
        return cropIcons;
    }

    /**
     * Returns the tool icons.
     * @return the {@link SpriteSheet} representing the tool icons
     */
    public SpriteSheet getToolIcons() {
        return toolIcons;
    }

    /**
     * Returns the block icons.
     * @return the {@link SpriteSheet} representing the block icons
     */
    public SpriteSheet getBlockIcons() {
        return blockIcons;
    }

    /**
     * Returns the torch frames/icons.
     * @return the {@link SpriteSheet} representing the torch frames
     */
    public SpriteSheet getTorchIcons() {
        return torchIcons;
    }

    /**
     * Returns the material icons.
     * @return the {@link SpriteSheet} representing the material icons
     */
    public SpriteSheet getMaterialIcons() {
        return materialIcons;
    }

    /**
     * Returns the usables icons.
     * @return the {@link SpriteSheet} representing the usables icons
     */
    public SpriteSheet getUsablesIcons() {
        return usablesIcons;
    }

    /**
     * Returns the inventory icons.
     * @return the {@link SpriteSheet} representing the inventory icons
     */
    public SpriteSheet getInventoryIcons() {
        return inventoryIcons;
    }

    /**
     * Returns the book animation sheet.
     * @return the {@link SpriteSheet} representing the book animation sheet
     */
    public SpriteSheet getBookAnimationSheet() {
        return bookAnimationSheet;
    }

    /**
     * Returns the icon used to sort crafting-book recipes by name.
     * @return the {@link SpriteSheet} representing the sort-by-name icon
     */
    public SpriteSheet getBookSortNameIcon() {
        return bookSortNameIcon;
    }

    /**
     * Returns the icon used to sort crafting-book recipes by type.
     * @return the {@link SpriteSheet} representing the sort-by-type icon
     */
    public SpriteSheet getBookSortTypeIcon() {
        return bookSortTypeIcon;
    }

    /**
     * Returns the icon used to toggle local recipes (available ones)
     * @return the {@link SpriteSheet} representing toggle-local-recipes
     */
    public SpriteSheet getBookHomeCraftings() {
        return bookHomeCraftings;
    }

    /**
     * Returns the icon used to close a book.
     * @return the {@link SpriteSheet} representing the close icon
     */
    public SpriteSheet getBookCloseIcon() {
        return bookCloseIcon;
    }

    /**
     * Returns the hearts sprite sheet.
     * @return the {@link SpriteSheet} representing the hearts sprite sheet
     */
    public SpriteSheet getHeartsSpriteSheet() {
        return heartsSpriteSheet;
    }

    /**
     * Returns the crop spritesheets.
     * @return the {@link Map} representing the crop spritesheets
     */
    public Map<CropType, SpriteSheet> getCropSpritesheets() {
        return cropSpritesheets;
    }

    /**
     * Returns the models associated with interactive block types.
     *
     * @return the {@link Map} representing the interactive block model map
     */
    public Map<BlockData, GLTFModel> getBlockModels() {
        return blockModels;
    }

    /**
     * Returns the shader.
     * @param name the {@link String} supplied as {@code name}
     * @return the {@link Shader} representing the shader
     */
    public Shader getShader(String name) {
        if (name == null) return defaultShader;

        return switch (name.toLowerCase()) {
            case "rain" -> rainShader;
            case "motion_blur" -> motionBlurShader;
            case "blur" -> blurShader;
            case "shadow" -> shadowMapShader;
            case "grass" -> grassShader;
            case "default", "item" -> defaultShader;
            default -> {
                log.warn("Shader '{}' not found, using defaultShader", name);
                yield defaultShader;
            }
        };
    }
}
