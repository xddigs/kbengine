package org.kbeng.games.rpg.voxel;

import org.kbeng.games.rpg.data.BlockData;

/** RPG material colours, independent of texture paths, UVs and the engine.
 * Every face of a voxel uses the same RGB value. Geological colour layers come
 * from generated material strata, never a different top/side texture. */
public final class VoxelPalette {
    private VoxelPalette() {}
    public static boolean fluid(byte id) {
        return id == BlockData.WATER.getId() || id == BlockData.LAVA.getId();
    }
    public static boolean solid(byte id) { return id != 0 && !fluid(id); }
    public static int rgb(byte id) { return rgb(BlockData.fromId(id)); }
    public static int rgb(BlockData type) {
        if (type == null) return 0;
        return switch (type) {
            case AIR -> 0;
            case DIRT -> 0x8B6145;
            case TILLED_DIRT -> 0x654532;
            case GRASS, TALL_GRASS -> 0x79A64B;
            case STONE, STONE_SLAB, STONE_VERTICAL_SLAB, STONE_STAIRCASE -> 0x85878C;
            case COBBLESTONE -> 0x6A7077;
            case SAND -> 0xDBC68A;
            case GRAVEL -> 0x9B9386;
            case SNOW -> 0xE3EDF0;
            case GLASS -> 0xB0D2D7;
            case OAK_LEAVES, OAK_BONSAI -> 0x4F853E;
            case SPRUCE_LEAVES, SPRUCE_BONSAI -> 0x315F46;
            case OAK_LOG -> 0x745033;
            case SPRUCE_LOG -> 0x503D31;
            case OAK_PLANK, OAK_PLANK_SLAB, OAK_PLANK_VERTICAL_SLAB, OAK_PLANK_STAIRCASE,
                 OAK_PLANK_FENCE, OAK_DOOR, OAK_CHEST -> 0xAD8253;
            case SPRUCE_PLANK, SPRUCE_PLANK_SLAB, SPRUCE_PLANK_VERTICAL_SLAB, SPRUCE_PLANK_STAIRCASE,
                 SPRUCE_PLANK_FENCE, SPRUCE_DOOR, SPRUCE_CHEST -> 0x765637;
            case COPPER_ORE -> 0xB8754E;
            case IRON_ORE -> 0xA49280;
            case STEEL_ORE -> 0x637584;
            case GOLD_ORE -> 0xD4AA46;
            case PLATINUM_ORE -> 0xCDD1DC;
            case DIAMOND_ORE -> 0x66BFBF;
            case FOSSIL -> 0xB6A17B;
            case OBSIDIAN -> 0x382F48;
            case VOIDSEAL -> 0x242632;
            case WATER -> 0x3D8EAF;
            case LAVA -> 0xF36C27;
            case TORCH -> 0xDBA252;
            default -> 0xAD6791;
        };
    }
}
