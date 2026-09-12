package org.kbeng.item;

import org.kbeng.data.BlockData;
import org.kbeng.data.Enchantment;
import org.kbeng.data.Tier;
import org.kbeng.data.ToolType;
import org.kbeng.service.SoundService;
import org.kbeng.wrld.GameMaster;
import org.kbeng.wrld.World;

/**
 * Hoe provides hoe capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Tool, inheriting shared behavior while specializing subsystem-specific logic.
 */
public class Hoe extends Tool {

    /**
     * Creates a new {@code Hoe} instance.
     * @param tier the {@link Tier} supplied as {@code tier}
     */
    public Hoe(Tier tier) {
        super((byte) 3, ToolType.HOE.getName(), 150, ToolType.HOE,
                tier, tier.getDurability() + ToolType.HOE.getBaseDurability());
    }

    /**
     * Creates a new {@code Hoe} instance.
     */
    public Hoe() {
        this(Tier.WOODEN);
    }

    /**
     * Handles use and applies its effect to the current interaction state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param block the {@link Block} supplied as {@code block}
     */
    public void use(GameMaster gameMaster, Block block) {
        super.use();
        World world = gameMaster.getWorld();
        Block target = world.getBlockAt(block.getX(), block.getY(), block.getZ());

        if (target == null) return;
        if (!target.getType().isTillable()) return;
        if (target.getType().equals(BlockData.TILLED_DIRT)) { return; }

        world.setBlockTypeAt(target.getX(), target.getY(),
                target.getZ(), BlockData.TILLED_DIRT.getId());

        gameMaster.rebuildChunkMeshAt(target.getX(), target.getZ());
        SoundService.fx.playPlaceSound(block.getType().getSoundGroup());
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Hoe(getTier());
    }

    /**
     * {@inheritDoc}
     * Applies enchanting and updates the affected character or item state.
     * @param enchantment the {@link Enchantment} supplied as {@code enchantment}
     * @return {@code boolean}; the enchanting result
     */
    @Override
    public boolean enchanting(Enchantment enchantment) {
        return false;
    }
}
