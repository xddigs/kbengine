package com.isofarm.data;

import com.isofarm.graphics.TextureAtlas;
import com.isofarm.item.Block;
import com.isofarm.item.Item;
import com.isofarm.item.MiningComponent;
import com.isofarm.item.Tool;
import com.isofarm.utils.K;
import com.isofarm.utils.Local;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Enumerates the supported block data values.
 */
@DataClass
public enum BlockData implements Blockable {
    AIR((byte) 0, (byte) 0, (byte) 0, false, false, 0, null, SoundGroup.SILENT, 0f, true, new Object[]{}, Tier.NONE),
    DIRT((byte) 1, (byte) 1, (byte) 0, true, false, 100, "assets/textures/blocks/dirt.png", SoundGroup.SOIL, 0.9f, false, new Object[]{}, Tier.NONE),
    GRASS((byte) 2, (byte) 2, (byte) 0, true, false, 120, "assets/textures/blocks/grass_top.png", "assets/textures/blocks/grass_bottom.png","assets/textures/blocks/grass.png",SoundGroup.SOIL, 1.0f, false, new Seed[]{new Seed()}, Tier.NONE),
    STONE((byte) 3, (byte) 3, (byte) 0, false, false, 150, "assets/textures/blocks/stone.png", SoundGroup.STONE, 6.0f, false, new Object[]{}, Tier.NONE),
    TILLED_DIRT((byte) 4, (byte) 4, (byte) 0, true, false,  110, "assets/textures/blocks/dirt_tilled.png", "assets/textures/blocks/dirt.png", "assets/textures/blocks/dirt.png", SoundGroup.SOIL, 0.9f, false, new Object[]{}, Tier.NONE),
    VOIDSTONE((byte) 5, (byte) 5, (byte) 0, false, false, 999, "assets/textures/blocks/voidstone.png", SoundGroup.STONE, 999999.0f, false,  new Object[]{}, Tier.NONE),
    SNOW((byte) 6, (byte) 6, (byte) 0, false, false, 120, "assets/textures/blocks/snow.png", SoundGroup.SNOW, 0.8f, false, new Object[]{}, Tier.NONE),
    GLASS((byte) 7, (byte) 7, (byte) 0, false,false,  200, "assets/textures/blocks/glass.png", SoundGroup.GLASS, 1.2f, true, new Object[]{}, Tier.NONE),

    OAK_LEAVES((byte) 8, (byte) 1, (byte) 1, false, false, 50, "assets/textures/blocks/oak_leaves.png", SoundGroup.SOIL, 1.1f, false,  new Object[]{MaterialID.STICK, "OAK_BONSAI"}, Tier.NONE),
    OAK_LOG((byte) 9, (byte) 2, (byte) 1, false, false, 150, "assets/textures/blocks/oak_log_top.png", "assets/textures/blocks/oak_log_bottom.png", "assets/textures/blocks/oak_log_side.png", SoundGroup.WOOD, 2.2f, false,  new Object[]{}, Tier.WOODEN),
    OAK_PLANK((byte) 10, (byte) 3, (byte) 1, false, false, 100, "assets/textures/blocks/oak_plank.png", SoundGroup.WOOD, 4.0f, false,  new Object[]{}, Tier.WOODEN),
    OAK_PLANK_SLAB((byte) 11, (byte) 4, (byte) 1, false, false, 50, "assets/textures/blocks/oak_plank.png", SoundGroup.WOOD, 4.0f, false,  new Object[]{}, Tier.WOODEN),
    OAK_PLANK_VERTICAL_SLAB((byte) 12, (byte) 5, (byte) 1, false, false, 80, "assets/textures/blocks/oak_plank.png", SoundGroup.WOOD, 4.0f, false,  new Object[]{}, Tier.WOODEN),
    OAK_PLANK_FENCE((byte) 13, (byte) 7, (byte) 1, false, false, 90, "assets/textures/blocks/oak_plank.png", SoundGroup.WOOD, 4.0f, false,  new Object[]{}, Tier.WOODEN),

    COPPER_ORE((byte) 14, (byte) 1, (byte) 4, false, false, 150, "assets/textures/blocks/copper_ore.png", SoundGroup.STONE, 6.0f, false, new MiningComponent[]{new MiningComponent(Tier.COPPER, MaterialID.RAW_ORE)}, Tier.COPPER),
    IRON_ORE((byte) 15, (byte) 2, (byte) 4, false, false, 150, "assets/textures/blocks/iron_ore.png", SoundGroup.STONE, 8.0f, false, new MiningComponent[]{new MiningComponent(Tier.IRON, MaterialID.RAW_ORE)}, Tier.IRON),
    STEEL_ORE((byte) 16, (byte) 3, (byte) 4, false, false, 200, "assets/textures/blocks/steel_ore.png", SoundGroup.STONE, 10.0f, false,new MiningComponent[]{new MiningComponent(Tier.STEEL, MaterialID.RAW_ORE)}, Tier.STEEL),
    GOLD_ORE((byte) 17, (byte) 4, (byte) 4, false, false, 500, "assets/textures/blocks/gold_ore.png", SoundGroup.STONE, 12.0f, false, new MiningComponent[]{new MiningComponent(Tier.GOLDEN, MaterialID.RAW_ORE)}, Tier.GOLDEN),
    PLATINUM_ORE((byte) 18, (byte) 5, (byte) 4, false, false, 800, "assets/textures/blocks/platinum_ore.png", SoundGroup.STONE, 14.0f, false, new MiningComponent[]{new MiningComponent(Tier.PLATINUM, MaterialID.RAW_ORE)}, Tier.PLATINUM),
    DIAMOND_ORE((byte) 19, (byte) 6, (byte) 4, false, false, 1000, "assets/textures/blocks/diamond_ore.png", SoundGroup.STONE, 16.0f, false, new MiningComponent[]{new MiningComponent(Tier.DIAMOND, MaterialID.RAW_ORE)}, Tier.DIAMOND),
    SAND((byte) 20, (byte) 7, (byte) 4, false, false, 100, "assets/textures/blocks/sand.png", SoundGroup.SOIL, 0.7f, false, new Object[]{}, Tier.NONE),
    GRAVEL((byte) 21, (byte) 8, (byte) 4, false, false, 100, "assets/textures/blocks/gravel.png", SoundGroup.SOIL, 0.9f, false, new Object[]{}, Tier.NONE),
    FOSSIL((byte) 22, (byte) 9, (byte) 4, false, false, 100, "assets/textures/blocks/fossil.png", SoundGroup.STONE, 8.0f, false, new Object[]{}, Tier.NONE),
    OBSIDIAN((byte) 23,(byte) 10,(byte) 4, false,  false, 100, "assets/textures/blocks/obsidian.png", SoundGroup.STONE, 48.0f, false, new Object[]{}, Tier.NONE),

    TALL_GRASS((byte) 24, (byte) 1, (byte) (K.UI.ICON_BLOCK_ROWS - 1), false, true, 5, "assets/textures/blocks/tall_grass.png", SoundGroup.SOIL, 0.01f, true, new Object[]{new Seed(CropType.WHEAT)}, Tier.NONE),
    ROSE((byte) 25, (byte) 2, (byte) (K.UI.ICON_BLOCK_ROWS - 1), false, true, 10, "assets/textures/blocks/rose.png", SoundGroup.SOIL, 0.01f, true, new Object[]{}, Tier.NONE),
    ROSEBUSH((byte) 26, (byte) 3, (byte) (K.UI.ICON_BLOCK_ROWS - 1), false, true, 20, "assets/textures/blocks/rosebush.png", SoundGroup.SOIL, 0.01f, true,  new Object[]{}, Tier.NONE),
    LILY((byte) 27, (byte) 4, (byte) (K.UI.ICON_BLOCK_ROWS - 1), false, true, 10, "assets/textures/blocks/lily.png", SoundGroup.SOIL, 0.01f, true, new Object[]{}, Tier.NONE),
    GHOSTFLOWER((byte) 28, (byte) 5, (byte) (K.UI.ICON_BLOCK_ROWS - 1), false, true, 15, "assets/textures/blocks/ghostflower.png", SoundGroup.SOIL, 0.01f, true,  new Object[]{}, Tier.NONE),
    RED_MUSHROOM((byte) 29, (byte) 6, (byte) (K.UI.ICON_BLOCK_ROWS - 1), false, true, 15, "assets/textures/blocks/red_mushroom.png", SoundGroup.SOIL, 0.01f, true,  new Object[]{}, Tier.NONE),
    BRIGHT_FLOWER((byte) 30, (byte) 7, (byte) (K.UI.ICON_BLOCK_ROWS - 1), false, true, 10, "assets/textures/blocks/bright_flower.png", SoundGroup.SOIL, 0.01f, true,  new Object[]{}, Tier.NONE),
    BLUE_FLOWER((byte) 31, (byte) 8, (byte) (K.UI.ICON_BLOCK_ROWS - 1), false, true, 10, "assets/textures/blocks/blue_flower.png", SoundGroup.SOIL, 0.01f, true,  new Object[]{}, Tier.NONE),
    ROSES((byte) 32, (byte) 9, (byte) (K.UI.ICON_BLOCK_ROWS - 1), false, true, 15, "assets/textures/blocks/roses.png", SoundGroup.SOIL, 0.01f, true,  new Object[]{}, Tier.NONE),
    TULIP((byte) 33, (byte) 10, (byte) (K.UI.ICON_BLOCK_ROWS - 1), false, true, 10, "assets/textures/blocks/tulip.png", SoundGroup.SOIL, 0.01f, true, new Object[]{}, Tier.NONE),

    OAK_BONSAI((byte) 34, (byte) 1, (byte) (K.UI.ICON_BLOCK_ROWS - 2), false, true, 100, "assets/textures/blocks/oak_bonsai.png", SoundGroup.SOIL, 0.01f, true, new Object[]{}, Tier.NONE),
    LAVA((byte) 126, (byte) -1, (byte) -1, false, false, 150, "assets/textures/blocks/lava.png", SoundGroup.LAVA, 0.0f, false, new Object[]{}, Tier.NONE),
    WATER((byte) 127, (byte) -1, (byte) -1, false, false, 80, "assets/textures/blocks/water.png", SoundGroup.WATER, 0.0f, true, new Object[]{}, Tier.NONE);

    public static final BlockData[] ORES = {COPPER_ORE, IRON_ORE, STEEL_ORE, GOLD_ORE, PLATINUM_ORE, DIAMOND_ORE};
    private static final BlockData[] BY_ID = createById();
    private final byte id;
    private final byte col;
    private final byte row;
    private final boolean isTillable;
    private final boolean isPlant;
    private final boolean isTransparent;
    private final Object[] drops;
    private final Tier tier;
    private final int value;

    private final String topPath;
    private final String bottomPath;
    private final String sidePath;
    private final SoundGroup soundGroup;
    private final float destroyTime;

    private TextureAtlas.TextureRegion topRegion;
    private TextureAtlas.TextureRegion bottomRegion;
    private TextureAtlas.TextureRegion sideRegion;

    /**
     * Creates a new {@code BlockData} instance.
     * @param id the {@code byte} supplied as {@code id}
     * @param col the {@code byte} supplied as {@code col}
     * @param row the {@code byte} supplied as {@code row}
     * @param isTillable the {@code boolean} supplied as {@code isTillable}
     * @param isPlant the {@code boolean} supplied as {@code isPlant}
     * @param value the {@code int} supplied as {@code value}
     * @param topPath the {@link String} supplied as {@code topPath}
     * @param bottomPath the {@link String} supplied as {@code bottomPath}
     * @param sidePath the {@link String} supplied as {@code sidePath}
     * @param soundGroup the {@link SoundGroup} supplied as {@code soundGroup}
     * @param destroyTime the {@code float} supplied as {@code destroyTime}
     * @param isTransparent the {@code boolean} supplied as {@code isTransparent}
     * @param drops an array of {@link Object} values supplied as {@code drops}
     * @param tier the {@link Tier} supplied as {@code tier}
     */
    BlockData(byte id, byte col, byte row, boolean isTillable, boolean isPlant, int value,
              String topPath, String bottomPath, String sidePath, SoundGroup soundGroup,
              float destroyTime, boolean isTransparent, Object[] drops, Tier tier) {
        this.id = id;
        this.col = col;
        this.row = row;
        this.isTillable = isTillable;
        this.isPlant = isPlant;
        this.value = value;
        this.topPath = topPath;
        this.bottomPath = bottomPath;
        this.sidePath = sidePath;
        this.soundGroup = soundGroup;
        this.destroyTime = destroyTime;
        this.isTransparent = isTransparent;
        this.drops = drops;
        this.tier = tier;
    }

    /**
     * Creates a new {@code BlockData} instance.
     * @param id the {@code byte} supplied as {@code id}
     * @param col the {@code byte} supplied as {@code col}
     * @param row the {@code byte} supplied as {@code row}
     * @param isTillable the {@code boolean} supplied as {@code isTillable}
     * @param isPlant the {@code boolean} supplied as {@code isPlant}
     * @param value the {@code int} supplied as {@code value}
     * @param texturePath the {@link String} supplied as {@code texturePath}
     * @param soundGroup the {@link SoundGroup} supplied as {@code soundGroup}
     * @param destroyTime the {@code float} supplied as {@code destroyTime}
     * @param isTransparent the {@code boolean} supplied as {@code isTransparent}
     * @param drops an array of {@link Object} values supplied as {@code drops}
     * @param tier the {@link Tier} supplied as {@code tier}
     */
    BlockData(byte id, byte col, byte row, boolean isTillable, boolean isPlant,
              int value, String texturePath, SoundGroup soundGroup, float destroyTime,
              boolean isTransparent, Object[] drops, Tier tier) {
        this(id, col, row, isTillable, isPlant, value, texturePath, texturePath, texturePath, soundGroup, destroyTime, isTransparent, drops, tier);
    }

    /**
     * Returns the all texture paths.
     * @return the {@link List} representing the all texture paths
     */
    public static List<String> getAllTexturePaths() {
        List<String> paths = new ArrayList<>();
        for (BlockData block : values()) {
            if (block.topPath != null && !paths.contains(block.topPath)) paths.add(block.topPath);
            if (block.bottomPath != null && !paths.contains(block.bottomPath)) paths.add(block.bottomPath);
            if (block.sidePath != null && !paths.contains(block.sidePath)) paths.add(block.sidePath);
        }
        return paths;
    }

    /**
     * Creates and returns the by id.
     * @return an array of {@link BlockData} values; the created by id
     */
    private static BlockData[] createById() {
        byte maxId = 0;
        for (BlockData block : values()) {
            if (block.id > maxId) {
                maxId = block.id;
            }
        }

        BlockData[] result = new BlockData[maxId + 1];
        for (BlockData block : values()) {
            if (result[block.id] != null) {
                throw new IllegalStateException("Duplicate block id " + block.id
                        + " for " + result[block.id].name() + " and " + block.name());
            }
            result[block.id] = block;
        }

        return result;
    }

    /**
     * Creates or returns from id from the supplied arguments.
     * @param id the {@code byte} supplied as {@code id}
     * @return the {@link BlockData} representing the from id result
     */
    public static BlockData fromId(byte id) {
        int index = Byte.toUnsignedInt(id);
        if (index < 0 || index >= BY_ID.length) {
            return null;
        }
        return BY_ID[index];
    }

    /**
     * Creates or returns from id to from the supplied arguments.
     * @param id the {@code byte} supplied as {@code id}
     * @return the {@link Block} representing the from id to result
     */
    public static Block fromIdTo(byte id) {
        return new Block(fromId(id));
    }

    /**
     * Creates or returns from name from the supplied arguments.
     * @param name the {@link String} supplied as {@code name}
     * @return the {@link BlockData} representing the from name result
     */
    public static BlockData fromName(String name) {
        for (BlockData block : all()) {
            if (block.name().equals(name)) return block;
        }
        return null;
    }

    /**
     * Creates or returns all plants from the supplied arguments.
     * @return an array of {@link BlockData} values; the all plants result
     */
    public static BlockData[] allPlants() {
        List<BlockData> result = new ArrayList<>();
        for (BlockData block : values()) {
            if (block.isPlant) result.add(block);
        }
        return result.toArray(new BlockData[0]);
    }

    /**
     * Creates or returns all from the supplied arguments.
     * @return an array of {@link BlockData} values; the all result
     */
    public static BlockData[] all() {
        return values();
    }

    /**
     * Returns the ores.
     * @return an array of {@link BlockData} values; the ores
     */
    public static BlockData[] getOres() {
        return ORES;
    }

    /**
     * Returns the ore.
     * @param tier the {@link Tier} supplied as {@code tier}
     * @return the {@link BlockData} representing the ore
     */
    public static BlockData getOre(Tier tier) {
        for (BlockData block : ORES) {
            if (block.getTier() == tier) return block;
        }
        return null;
    }

    /**
     * Returns the random ore.
     * @return the {@link BlockData} representing the random ore
     */
    public static BlockData getRandomOre() {
        return ORES[(int) (Math.random() * ORES.length)];
    }

    /**
     * Initializes the regions.
     * @param atlas the {@link TextureAtlas} supplied as {@code atlas}
     */
    public void initRegions(TextureAtlas atlas) {
        if (atlas == null) return;
        if (topPath != null) this.topRegion = atlas.getRegion(topPath);
        if (bottomPath != null) this.bottomRegion = atlas.getRegion(bottomPath);
        if (sidePath != null) this.sideRegion = atlas.getRegion(sidePath);
    }

    /**
     * Returns the id.
     * @return {@code byte}; the id
     */
    public byte getId() {
        return id;
    }

    /**
     * Returns the col.
     * @return {@code byte}; the col
     */
    public byte getCol() {
        return col;
    }

    /**
     * Returns the row.
     * @return {@code byte}; the row
     */
    public byte getRow() {
        return row;
    }

    /**
     * Returns the name.
     * @return the {@link String} representing the name
     */
    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    public String getDisplayName() {
        return Local.lang.t("block." + name().toLowerCase(Locale.ROOT));
    }

    /**
     * Returns the value.
     * @return {@code int}; the value
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns the tier.
     * @return the {@link Tier} representing the tier
     */
    public Tier getTier() {
        return tier;
    }

    /**
     * Checks whether the plant condition is met.
     * @return {@code true} if plant; otherwise {@code false}
     */
    public boolean isPlant() {
        return isPlant;
    }

    /**
     * Checks whether the tillable condition is met.
     * @return {@code true} if tillable; otherwise {@code false}
     */
    public boolean isTillable() {
        return isTillable;
    }

    /**
     * Checks whether the transparent condition is met.
     * @return {@code true} if transparent; otherwise {@code false}
     */
    public boolean isTransparent() {
        return isTransparent;
    }

    /**
     * Checks whether the solid condition is met.
     * @return {@code true} if solid; otherwise {@code false}
     */
    public boolean isSolid() {
        return this != AIR && !isPlant && !isFluid();
    }

    /**
     * Checks whether the fluid condition is met.
     * @return {@code true} if fluid; otherwise {@code false}
     */
    public boolean isFluid() {
        return this == WATER || this == LAVA;
    }

    /** Returns the physical/render shape used by this block. */
    public BlockShape getShape() {
        return switch (this) {
            case OAK_PLANK_SLAB -> BlockShape.HORIZONTAL_SLAB;
            case OAK_PLANK_VERTICAL_SLAB -> BlockShape.VERTICAL_SLAB_WEST;
            case OAK_PLANK_FENCE -> BlockShape.FENCE_NONE;
            default -> BlockShape.FULL_CUBE;
        };
    }

    /** Returns whether this block completely occludes each face of its cell. */
    public boolean isFullCube() {
        return isSolid() && getShape().isFullCube();
    }

    /**
     * Returns the top path.
     * @return the {@link String} representing the top path
     */
    public String getTopPath() {
        return topPath;
    }

    /**
     * Returns the bottom path.
     * @return the {@link String} representing the bottom path
     */
    public String getBottomPath() {
        return bottomPath;
    }

    /**
     * Returns the side path.
     * @return the {@link String} representing the side path
     */
    public String getSidePath() {
        return sidePath;
    }

    /**
     * Returns the top region.
     * @return the {@link TextureAtlas.TextureRegion} representing the top region
     */
    public TextureAtlas.TextureRegion getTopRegion() {
        return topRegion;
    }

    /**
     * Returns the bottom region.
     * @return the {@link TextureAtlas.TextureRegion} representing the bottom region
     */
    public TextureAtlas.TextureRegion getBottomRegion() {
        return bottomRegion;
    }

    /**
     * Returns the side region.
     * @return the {@link TextureAtlas.TextureRegion} representing the side region
     */
    public TextureAtlas.TextureRegion getSideRegion() {
        return sideRegion;
    }

    /**
     * Returns the sound group.
     * @return the {@link SoundGroup} representing the sound group
     */
    public SoundGroup getSoundGroup() {
        return soundGroup;
    }

    /**
     * Returns the destroy time.
     * @return {@code float}; the destroy time
     */
    public float getDestroyTime() {
        return destroyTime;
    }

    /**
     * Returns the effective destroy time for the item currently being used.
     * Incompatible items keep the block's base time. Compatible, usable tools
     * apply their type speed and gain 25% more speed per material tier.
     * @param item item used to break this block, or {@code null} for an empty hand
     * @return effective destroy time in seconds
     */
    public float getDestroyTime(Item item) {
        if (!(item instanceof Tool tool)
                || !tool.canBeUsed()
                || !tool.getType().isUsableOn(this)) {
            return destroyTime;
        }

        int tierLevel = Math.max(0, tool.getTier().getId());
        float tierSpeed = 1.0f + tierLevel * 0.25f;
        return destroyTime / (tool.getType().getDestroySpeed() * tierSpeed);
    }

    /**
     * Returns the drops.
     * @return an array of {@link Object} values; the drops
     */
    public Object[] getDrops() {
        return drops;
    }

    /**
     * Checks whether the drops condition is met.
     * @return {@code true} if drops; otherwise {@code false}
     */
    public boolean hasDrops() {
        return drops != null && drops.length > 0;
    }

    /**
     * Returns the random drop.
     * @return the {@link Object} representing the random drop
     */
    public Object getRandomDrop() {
        if (!hasDrops()) return null;
        if (Math.random() < 0.50f) {
            int index = (int) (Math.random() * drops.length);
            Object rawDrop = drops[index];

            if (rawDrop instanceof String blockKey) {
                return new Block(BlockData.valueOf(blockKey));
            }

            return rawDrop;
        }
        return null;
    }
}
