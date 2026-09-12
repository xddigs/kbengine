package org.kbeng.rpg.item;

import org.kbeng.rpg.data.BlockData;
import org.kbeng.rpg.data.BlockPos;
import org.kbeng.rpg.data.Enchantment;
import org.kbeng.rpg.data.Usables;
import org.kbeng.rpg.entity.Player;
import org.kbeng.engine.utils.HoveredCell;
import org.kbeng.engine.utils.Local;
import org.kbeng.rpg.wrld.GameMaster;
import org.kbeng.rpg.wrld.FluidSimulation;
import org.kbeng.rpg.wrld.World;

/**
 * Bucket provides bucket capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Usable, inheriting shared behavior while specializing subsystem-specific logic.
 */
public class Bucket extends Usable {
    private BlockData type;

    /**
     * Creates a new {@code Bucket} instance.
     * @param type the {@link BlockData} supplied as {@code type}
     */
    public Bucket(BlockData type) {
        super(Usables.BUCKET, Local.lang.t("item.usable.bucket"));
        this.type = type;
    }

    /**
     * Creates a new {@code Bucket} instance.
     */
    public Bucket() {
        this(BlockData.AIR);
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Bucket(getBlockType());
    }

    /**
     * {@inheritDoc}
     * Handles use and applies its effect to the current interaction state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param isCtrlHeld the {@code boolean} supplied as {@code isCtrlHeld}
     * @return {@code boolean}; the use result
     */
    @Override
    public boolean use(GameMaster gameMaster, boolean isCtrlHeld) {
        Player player = Player.plyr;
        if (player == null) {
            return false;
        }

        if (!isFull()) {
            BlockPos targetBlock = HoveredCell.get(gameMaster, false);
            if (targetBlock == null) {
                return false;
            }

            BlockData fluidType = BlockData.fromId(World.wrld.getBlockTypeAt(targetBlock));
            FluidSimulation simulation = FluidSimulation.forBlock(fluidType);
            if (simulation == null) {
                return false;
            }

            if (!simulation.isSource(
                    targetBlock.x(), targetBlock.y(), targetBlock.z())) {
                return false;
            }

            if (!simulation.removeFluid(
                    targetBlock.x(),
                    targetBlock.y(),
                    targetBlock.z())) {
                return false;
            }
            fill(fluidType);
            return true;
        }

        BlockPos targetBlock = HoveredCell.get(gameMaster, false);
        if (targetBlock == null) {
            return false;
        }

        int normalX = gameMaster.getCamera().getLastHitNormalX();
        int normalY = gameMaster.getCamera().getLastHitNormalY();
        int normalZ = gameMaster.getCamera().getLastHitNormalZ();

        int placeX = targetBlock.x() + normalX;
        int placeY = targetBlock.y() + normalY;
        int placeZ = targetBlock.z() + normalZ;

        FluidSimulation simulation = FluidSimulation.forBlock(type);
        if (simulation == null || !simulation.addSource(placeX, placeY, placeZ)) {
            return false;
        }
        empty();
        return true;
    }

    /**
     * {@inheritDoc}
     * Updates the current state.
     */
    @Override
    public void update() {}

    /**
     * Returns name according to the current object state.
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return getBucketDisplayName();
    }

    /**
     * {@inheritDoc}
     * Returns the localized name for the bucket's current contents.
     * @return {@link String} the localized display name
     */
    @Override
    public String getDisplayName() {
        return getBucketDisplayName();
    }

    /**
     * Returns the localized name for the bucket's current contents.
     * @return {@link String} the localized display name
     */
    private String getBucketDisplayName() {
        return switch (type) {
            case WATER -> Local.lang.t("item.usable.water_bucket");
            case LAVA -> Local.lang.t("item.usable.lava_bucket");
            case null, default -> Local.lang.t("item.usable.bucket");
        };
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

    /**
     * Returns the block type.
     * @return the {@link BlockData} representing the block type
     */
    public BlockData getBlockType() {
        return type;
    }

    /**
     * Sets the block type.
     * @param type the {@link BlockData} supplied as {@code type}
     */
    public void setBlockType(BlockData type) {
        this.type = type;
    }

    /**
     * Fills the bucket with a supported fluid.
     * @param fluidType the {@link BlockData} argument; the fluid block type
     */
    public void fill(BlockData fluidType) {
        FluidSimulation simulation = FluidSimulation.forBlock(fluidType);
        if (simulation != null) setBlockType(simulation.getFluidType());
    }

    /**
     * Determines whether this object contains no elements or active content.
     */
    public void empty() {
        setBlockType(BlockData.AIR);
    }

    /**
     * Checks whether the full condition is met.
     * @return {@code true} if full; otherwise {@code false}
     */
    public boolean isFull() {
        return type != null && type.isFluid();
    }
}
