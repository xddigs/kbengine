package com.isofarm.utils;

import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Represents the k component of the Isofarm runtime.
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
@Utils
public final class K {

    /**
     * Creates a new {@code K} instance.
     */
    private K() {}

    /**
 * Represents the camera component of the Isofarm runtime.
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
    public static final class Camera {
        public static final float FULL_DEGREES = 360.0f;
        public static final float HALF_DEGREES = 180.0f;
    }

    /**
 * Represents the world component of the Isofarm runtime.
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
    public static final class World {
        public static final float GRAVITY = -25.0f;
        public static final float JUMP_FORCE = 8.0f;

        public static final float TILE_SIZE = 1.0f;
        public static final float DEFAULT_BLOCK_DEPTH = 0.4f;
        public static final float WEATHER_CHANGE_PROBABILITY = 0.01f;

        public static final int STARTING_COINS = 100;
        public static final int MAX_STACK = 64;
        public static final int WATER_LEVEL_MAX = 100;

        public static final int MAX_PARTICLES = 24;
        public static final float DEFAULT_TEXTURE_SCALE = 16.0f;
        public static final float SHORTER_BLOCK_HEIGHT = 0.9375f;
    }

    /**
 * Represents the window component of the Isofarm runtime.
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
    public static final class Window {
        public static final float DEFAULT_WIDTH = 1280.0f;
        public static final float DEFAULT_HEIGHT = 720.0f;
    }

    /**
 * Represents the style component of the Isofarm runtime.
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
    public static final class Style {
        public static final float[] COLOR_TOAST_SUCCESS = {0.30f, 0.85f, 0.40f, 1.0f};
        public static final float[] COLOR_TOAST_SUCCESS_BG = {0.08f, 0.16f, 0.10f, 0.95f};
        public static final float[] COLOR_TOAST_INFO = {0.35f, 0.65f, 1.00f, 1.0f};
        public static final float[] COLOR_TOAST_INFO_BG = {0.07f, 0.11f, 0.18f, 0.95f};
        public static final float[] COLOR_TOAST_WARNING = {1.00f, 0.75f, 0.25f, 1.0f};
        public static final float[] COLOR_TOAST_WARNING_BG = {0.18f, 0.14f, 0.05f, 0.95f};
        public static final float[] COLOR_TOAST_ERROR = {1.00f, 0.35f, 0.35f, 1.0f};
        public static final float[] COLOR_TOAST_ERROR_BG = {0.20f, 0.07f, 0.07f, 0.95f};
        public static final float[] COLOR_TOAST_REWARD = {1.00f, 0.85f, 0.30f, 1.0f};
        public static final float[] COLOR_TOAST_REWARD_BG = {0.18f, 0.15f, 0.05f, 0.95f};
    }

    /**
 * Represents the ui component of the Isofarm runtime.
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
    public static final class UI {
        public static final int INVENTORY_COLUMNS = 9;
        public static final int INVENTORY_ROWS = 3;
        public static final int INVENTORY_SLOTS = INVENTORY_COLUMNS * INVENTORY_ROWS;
        public static final int HOTBAR_SLOTS = INVENTORY_COLUMNS;
        public static final int PLAYER_INVENTORY_SLOTS = INVENTORY_SLOTS + HOTBAR_SLOTS;

        public static final float SQUISH_DURATION = 0.25f;

        public static final int ICON_SEED_SEEDS_COLS = 4;
        public static final int ICON_SEED_CROPS_COLS = 5;
        public static final int ICON_FOOD_COLS = 4;

        public static final int ICON_BLOCK_COLS = 10;
        public static final int ICON_BLOCK_ROWS = 8;
        public static final int TORCH_COLS = 4;

        public static final int ICON_TOOL_COLS = 6;
        public static final int ICON_TOOL_ROWS = 8;
        public static final int ICON_ARMOR_ROWS = 8;
        public static final int ICON_ARMOR_COLS = 3;
        public static final int ICON_USABLES_COLS = 7;

        public static final int ICON_HEARTS_ROWS = 3;

        public static final int ICON_MATERIAL_COLS = 12;
        public static final int ICON_MATERIAL_ROWS = 2;
        public static final int ICON_INV_COLS = 4;


        public static final float TOAST_WIDTH = 500f;
        public static final float TOAST_HEIGHT = 64.0f;
        public static final float TOAST_MARGIN_TOP = 24.0f;
        public static final float TOAST_SPACING = 8.0f;
        public static final float TOAST_SLIDE_SPEED = 8.0f;
        public static final float TOAST_EXIT_SPEED = 10.0f;
        public static final float TOAST_DURATION = 4.0f;

        public static final float TOAST_ACCENT_WIDTH = 4.0f;
        public static final float TOAST_MESSAGE_OFFSET_X = 10.0f;
        public static final float TOAST_GAP_X = 8.0f;
        public static final float TOAST_PADDING_RIGHT = 10.0f;

        public static final float HOTBAR_LABEL_DURATION = 1.75f;
        public static final float HOTBAR_LABEL_OFFSET_Y = 20.0f;
        public static final float HOTBAR_OFFSET = 20;

        public static final float UI_BOOK_PADDING_X = 150.0f;

        public static final float CHAT_HISTORY_X = 10.0f;
        public static final float CHAT_HISTORY_OFFSET_Y = 20.0f;
        public static final float CHAT_HISTORY_LINE_HEIGHT = 20.0f;
        public static final int CHAT_HISTORY_MAX_MESSAGES = 10;
        public static final Vector4f CHAT_HISTORY_TEXT_COLOR = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

        public static final Vector4f UI_BACKGROUND_COLOR = new Vector4f(0.25f, 0.25f, 0.25f, 1.0f);

        public static final Vector4f UI_BORDER_COLOR = new Vector4f(0.06f, 0.06f, 0.06f, 1.0f);
        public static final Vector4f UI_ITEM_TINT = new Vector4f(1.0f);
        public static final Vector4f UI_TEXT_COLOR = new Vector4f(1.0f);
        public static final Vector4f UI_BOOK_TEXT_COLOR = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
        public static final Vector4f UI_HOTBAR_SELECTED_COLOR = new Vector4f(0.35f, 0.9f, 0.35f, 1.0f);
    }

    /**
 * Represents the paths component of the Isofarm runtime.
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
    public static final class Paths {
        public static final String FONT = "font/LeagueSpartan-Regular.ttf";
        public static final String FONT_BOLD = "font/LeagueSpartan-SemiBold.ttf";

        public static final String DEFAULT_VERT_SHADER = "shaders/default.vert";
        public static final String DEFAULT_FRAG_SHADER = "shaders/default.frag";
        public static final String GRASS_VERT_SHADER = "shaders/grass.vert";
        public static final String GRASS_FRAG_SHADER = "shaders/grass.frag";
        public static final String OUTLINE_VERT_SHADER = "shaders/outline.vert";
        public static final String OUTLINE_FRAG_SHADER = "shaders/outline.frag";

        public static final String MOTION_BLUR_VERT_SHADER = "shaders/motion_blur.vert";
        public static final String MOTION_BLUR_FRAG_SHADER = "shaders/motion_blur.frag";

        public static final String UI_VERTEX_SHADER = "shaders/ui.vert";
        public static final String UI_FRAG_SHADER = "shaders/ui.frag";

        public static final String RAIN_VERT_SHADER = "shaders/rain.vert";
        public static final String RAIN_FRAG_SHADER = "shaders/rain.frag";

        public static final String SHADOW_FRAG_SHADER = "shaders/shadow.frag";
        public static final String SHADOW_VERT_SHADER = "shaders/shadow.vert";
        public static final String POINT_SHADOW_FRAG_SHADER = "shaders/point_shadow.frag";
        public static final String POINT_SHADOW_VERT_SHADER = "shaders/point_shadow.vert";

        public static final String BLUR_VERT_SHADER = "shaders/blur.vert";
        public static final String BLUR_FRAG_SHADER = "shaders/blur.frag";

        public static final String WHEAT_TEXTURE = "assets/crops/wheat_crop.png";
        public static final String CARROT_TEXTURE = "assets/crops/carrot_crop.png";
        public static final String POTATO_TEXTURE = "assets/crops/potato_crop.png";
        public static final String BEETROOT_TEXTURE = "assets/crops/beetroot_crop.png";
        public static final String SUGAR_CANE_TEXTURE = "assets/crops/sugar_cane_crop.png";

        public static final String SEED_ICONS = "assets/sprites/seeds.png";
        public static final String CROP_ICONS = "assets/sprites/crops.png";
        public static final String FOOD_ICONS = "assets/sprites/food.png";
        public static final String TOOL_ICONS = "assets/sprites/tools.png";
        public static final String ARMOR_ICONS = "assets/sprites/armor.png";
        public static final String SHIELD_BACK = "assets/sprites/shield_back.png";
        public static final String BLOCK_ICONS = "assets/sprites/blocks.png";
        public static final String TORCH_ICONS = "assets/sprites/torch.png";
        public static final String MATERIAL_ICONS = "assets/sprites/materials.png";
        public static final String USABLES_ICONS = "assets/sprites/usables.png";
        public static final String INVENTORY_ICONS = "assets/sprites/inventory.png";

        public static final String PLAYER_ARMOR_MODEL = "assets/models/player/armor.gltf";
        public static final String PLAYER_MODEL = "assets/models/player/player.gltf";
        public static final String GOBLIN_MODEL = "assets/models/enemies/goblin.gltf";

        public static final String HEARTS_SPRITESHEET = "assets/ui/hearts.png";
        public static final String HUNGER_SPRITESHEET = "assets/ui/hunger.png";

        public static final String CURSOR_POINTER = "assets/ui/pointer.png";
        public static final String DEFAULT_BACKGROUND_UI = "assets/ui/slot.png";
        public static final String DEFAULT_SELECTOR_UI = "assets/ui/selector.png";
        public static final String SCROLL_BAR = "assets/ui/scroll_bar.png";
        public static final String SCROLL_KNOB = "assets/ui/scroll_knob.png";

        public static final String LOGO = "assets/ui/logo.png";

        public static final String ANIMATED_BOOK_UI = "assets/ui/animated_book.png";
        public static final String BOOK_UI = "assets/ui/book.png";
        public static final String BOOK_SORT_NAME = "assets/ui/sort_name.png";
        public static final String BOOK_FAVORITE = "assets/ui/favorite.png";
        public static final String BOOK_SORT_TYPE = "assets/ui/sort_type.png";
        public static final String BOOK_LOCAL_CRAFTINGS = "assets/ui/home_recipes.png";
        public static final String BOOK_CLOSE = "assets/ui/close.png";
    }

    /**
 * Represents the render component of the Isofarm runtime.
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
    public static final class Render {
        public static final float LINE_WIDTH = 2.0f;
        public static final int CROP_TOTAL_FRAMES = 5;
        public static final int PRIMARY_TEXTURE_UNIT = 0;
    }

    /**
 * Represents the colors component of the Isofarm runtime.
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
    public static final class Colors {
        public static final Vector3f OUTLINE_DEFAULT = new Vector3f(0.0f, 0.0f, 0.0f);
        public static final Vector3f RAIN = new Vector3f(0.35f, 0.55f, 1.0f);

    }
}
