package com.isofarm.data;

import com.isofarm.item.Item;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Enumerates the supported tool type values.
 */
public enum ToolType implements Item {
    SWORD((byte) 0, 7.0f, new BlockData[]{BlockData.OAK_LEAVES, BlockData.TALL_GRASS, BlockData.ROSE, BlockData.ROSEBUSH, BlockData.LILY, BlockData.GHOSTFLOWER, BlockData.RED_MUSHROOM, BlockData.BRIGHT_FLOWER, BlockData.BLUE_FLOWER, BlockData.ROSES, BlockData.TULIP, BlockData.OAK_BONSAI, BlockData.SPRUCE_LEAVES, BlockData.SPRUCE_BONSAI}, 10, 1.5f),
    PICKAXE((byte) 1, 6.0f, new BlockData[]{BlockData.STONE, BlockData.STONE_SLAB, BlockData.STONE_VERTICAL_SLAB, BlockData.STONE_STAIRCASE, BlockData.COBBLESTONE, BlockData.GLASS, BlockData.COPPER_ORE, BlockData.IRON_ORE, BlockData.STEEL_ORE, BlockData.GOLD_ORE, BlockData.PLATINUM_ORE, BlockData.DIAMOND_ORE, BlockData.FOSSIL, BlockData.OBSIDIAN}, 7, 2.5f),
    AXE((byte) 2, 8.5f, new BlockData[]{BlockData.OAK_LOG, BlockData.OAK_PLANK, BlockData.OAK_PLANK_SLAB, BlockData.OAK_PLANK_VERTICAL_SLAB, BlockData.OAK_PLANK_STAIRCASE, BlockData.OAK_PLANK_FENCE, BlockData.OAK_LEAVES, BlockData.SPRUCE_LOG, BlockData.SPRUCE_PLANK, BlockData.SPRUCE_PLANK_SLAB, BlockData.SPRUCE_PLANK_VERTICAL_SLAB, BlockData.SPRUCE_PLANK_STAIRCASE, BlockData.SPRUCE_PLANK_FENCE, BlockData.SPRUCE_LEAVES}, 10, 2.5f),
    HOE((byte) 3, 5.0f, new BlockData[]{BlockData.GRASS, BlockData.DIRT, BlockData.TILLED_DIRT, BlockData.OAK_LEAVES}, 8, 1.75f),
    SHOVEL((byte) 4, 4.0f, new BlockData[]{BlockData.GRASS, BlockData.DIRT, BlockData.TILLED_DIRT, BlockData.SNOW, BlockData.SAND, BlockData.GRAVEL}, 5, 3.0f),
    SHIELD((byte) 5, 0.0f, 15.0f, BlockData.all(), 250, 0.0f);

    private final byte id;
    private final float baseDamage;
    private final float baseDefense;
    private final BlockData[] usableOn;
    private final int baseDurability;
    private final float destroySpeed;

    /**
     * Creates a new {@code ToolType} instance.
     * @param id the {@code byte} supplied as {@code id}
     * @param baseDamage the {@code float} supplied as {@code baseDamage}
     * @param usableOn an array of {@link BlockData} values supplied as {@code usableOn}
     * @param baseDurability the {@code int} supplied as {@code baseDurability}
     * @param destroySpeed multiplier applied while breaking compatible blocks
     */
    ToolType(byte id, float baseDamage, BlockData[] usableOn,
             int baseDurability, float destroySpeed) {
        this.id = id;
        this.baseDamage = baseDamage;
        this.usableOn = usableOn;
        this.baseDurability = baseDurability;
        this.destroySpeed = destroySpeed;
        this.baseDefense = 0.0f;
    }

    /**
     * Creates a new {@code ToolType} instance. A defense type.
     * @param id the {@code byte} supplied as {@code id}
     * @param baseDamage the {@code float} supplied as {@code baseDamage}
     * @param baseDefense the {@code float} supplied as {@code baseDefense}
     * @param usableOn an array of {@link BlockData} values supplied as {@code usableOn}
     * @param baseDurability the {@code int} supplied as {@code baseDurability}
     * @param destroySpeed multiplier applied while breaking compatible blocks
     */
    ToolType(byte id, float baseDamage, float baseDefense, BlockData[] usableOn,
             int baseDurability, float destroySpeed) {
        this.id = id;
        this.baseDamage = baseDamage;
        this.baseDefense = baseDefense;
        this.usableOn = usableOn;
        this.baseDurability = baseDurability;
        this.destroySpeed = destroySpeed;
    }

    /**
     * {@inheritDoc}
     * Returns the id.
     * @return {@code byte}; the id
     */
    @Override
    public byte getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     * Returns the name.
     * @return the {@link String} representing the name
     */
    @Override
    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * {@inheritDoc}
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    @Override
    public String getDisplayName() {
        return "item." + getName();
    }

    /**
     * {@inheritDoc}
     * Returns the value.
     * @return {@code int}; the value
     */
    @Override
    public int getValue() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return this;
    }

    /**
     * Returns the base damage.
     * @return {@code float}; the base damage
     */
    public float getBaseDamage() {
        return baseDamage;
    }

    /**
     * Returns the {@code baseDefense} value
     * @return {@link float} value of baseDefense
     */
    public float getBaseDefense() {
        return baseDefense;
    }

    /**
     * Returns the usable on.
     * @return an array of {@link BlockData} values; the usable on
     */
    public BlockData[] getUsableOn() {
        return usableOn;
    }

    /**
     * Returns the base durability.
     * @return {@code int}; the base durability
     */
    public int getBaseDurability() {
        return baseDurability;
    }

    /**
     * Checks whether this tool type is effective against the supplied block.
     */
    public boolean isUsableOn(BlockData block) {
        if (block == null) return false;
        for (BlockData compatibleBlock : usableOn) {
            if (compatibleBlock == block) return true;
        }
        return false;
    }

    /**
     * Returns the breaking-speed multiplier for compatible blocks.
     */
    public float getDestroySpeed() {
        return destroySpeed;
    }

    public static void forEach(Consumer<ToolType> consumer) {
        for (ToolType toolType : ToolType.values()) {
            consumer.accept(toolType);
        }
    }
}
