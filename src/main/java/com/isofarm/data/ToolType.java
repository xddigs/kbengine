package com.isofarm.data;

import com.isofarm.item.Item;

import java.util.Locale;

/**
 * Enumerates the supported tool type values.
 */
public enum ToolType implements Item {
    SWORD((byte) 0, "Sword", 7, new BlockData[]{
            BlockData.OAK_LEAVES, BlockData.TALL_GRASS, BlockData.ROSE,
            BlockData.ROSEBUSH, BlockData.LILY, BlockData.GHOSTFLOWER,
            BlockData.RED_MUSHROOM, BlockData.BRIGHT_FLOWER,
            BlockData.BLUE_FLOWER, BlockData.ROSES, BlockData.TULIP,
            BlockData.OAK_BONSAI
    }, 10, 1.5f),
    PICKAXE((byte) 1, "Pickaxe", 6, new BlockData[]{
            BlockData.STONE, BlockData.VOIDSTONE, BlockData.GLASS,
            BlockData.COPPER_ORE, BlockData.IRON_ORE, BlockData.STEEL_ORE,
            BlockData.GOLD_ORE, BlockData.PLATINUM_ORE,
            BlockData.DIAMOND_ORE, BlockData.FOSSIL, BlockData.OBSIDIAN
    }, 7, 2.5f),
    AXE((byte) 2, "Axe", 8, new BlockData[]{
            BlockData.OAK_LOG, BlockData.OAK_WOOD, BlockData.OAK_LEAVES
    }, 10, 2.5f),
    HOE((byte) 3, "Hoe", 5, new BlockData[]{
            BlockData.GRASS, BlockData.DIRT, BlockData.TILLED_DIRT,
            BlockData.OAK_LEAVES
    }, 8, 1.75f),
    SHOVEL((byte) 4, "Shovel", 4, new BlockData[]{
            BlockData.GRASS, BlockData.DIRT, BlockData.TILLED_DIRT,
            BlockData.SNOW, BlockData.SAND, BlockData.GRAVEL
    }, 5, 3.0f);

    private final byte id;
    private final String name;
    private final float baseDamage;
    private final BlockData[] usableOn;
    private final int baseDurability;
    private final float destroySpeed;

    /**
     * Creates a new {@code ToolType} instance.
     * @param id the {@code byte} supplied as {@code id}
     * @param name the {@link String} supplied as {@code name}
     * @param baseDamage the {@code float} supplied as {@code baseDamage}
     * @param usableOn an array of {@link BlockData} values supplied as {@code usableOn}
     * @param baseDurability the {@code int} supplied as {@code baseDurability}
     * @param destroySpeed multiplier applied while breaking compatible blocks
     */
    ToolType(byte id, String name, float baseDamage, BlockData[] usableOn,
             int baseDurability, float destroySpeed) {
        this.id = id;
        this.name = name;
        this.baseDamage = baseDamage;
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
        return name;
    }

    /**
     * {@inheritDoc}
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    @Override
    public String getDisplayName() {
        return "item." + name().toLowerCase(Locale.ROOT);
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
}
