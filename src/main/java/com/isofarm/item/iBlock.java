package com.isofarm.item;

import com.isofarm.data.BlockPos;
import com.isofarm.data.Inventory;
import com.isofarm.data.InteractiveBlocks;
import com.isofarm.graphics.ResourceManager;
import com.isofarm.graphics.gltf.GLTFNode;
import com.isofarm.graphics.gltf.GLTFModel;
import com.isofarm.ui.GameUIService;
import com.isofarm.wrld.GameMaster;
import org.joml.Quaternionf;

/**
 * A craftable block with a model and interactive state.
 */
public class iBlock implements Craftable {
    private static final float ANIMATION_DURATION = 0.35f;
    private static final float CHEST_OPEN_ANGLE = (float) Math.toRadians(35.0);

    private final InteractiveBlocks type;
    private final GLTFModel blockModel;
    private final Inventory inventory;
    private int x, y, z;
    private boolean isActivated;
    private boolean isAnimating;
    private float animationProgress;
    private float orientation;

    public iBlock(InteractiveBlocks type, int x, int y, int z) {
        this(type, x, y, z, 0.0f);
    }

    /**
     * Creates an interactive block at the supplied position and orientation.
     *
     * @param type the {@link InteractiveBlocks} argument; the interactive block type
     * @param x the {@code int} argument; the x coordinate
     * @param y the {@code int} argument; the y coordinate
     * @param z the {@code int} argument; the z coordinate
     * @param orientation the {@code float} argument; the rotation around the vertical axis, in radians
     */
    public iBlock(InteractiveBlocks type, int x, int y, int z, float orientation) {
        this.type = type;
        this.blockModel = ResourceManager.rem.getBlockModels().get(type);
        this.inventory = new Inventory();
        this.x = x;
        this.y = y;
        this.z = z;
        this.isActivated = false;
        this.isAnimating = false;
        this.animationProgress = 0.0f;
        this.orientation = orientation;
    }

    public iBlock(InteractiveBlocks type, BlockPos pos) {
        this(type, pos.x(), pos.y(), pos.z());
    }

    public iBlock(InteractiveBlocks type) {
        this(type, 0, 0, 0);
    }

    /**
     * {@inheritDoc}
     * Returns the id of the block
     *
     * @return {@link Byte} the id of the block
     */
    @Override
    public byte getId() {
        return type.getId();
    }

    /**
     * {@inheritDoc}
     * Returns the name of the block
     *
     * @return {@link String} the name of the block
     */
    @Override
    public String getName() {
        return type.getName();
    }

    /**
     * {@inheritDoc}
     * Returns the display name of the block
     *
     * @return {@link String} the display name of the block
     */
    @Override
    public String getDisplayName() {
        return type.getDisplayName();
    }

    /**
     * {@inheritDoc}
     * Returns the value of the block
     *
     * @return {@link Integer} the value of the block
     */
    @Override
    public int getValue() {
        return type.getValue();
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     *
     * @return the {@link Item} result; {@code this} the copy result
     */
    @Override
    public Item copy() {
        return new iBlock(getType());
    }

    /**
     * Returns the type of block
     *
     * @return {@link InteractiveBlocks} the type of block
     */
    public InteractiveBlocks getType() {
        return type;
    }

    /**
     * Returns the block model
     * @return {@link GLTFModel} the block model
     */
    public GLTFModel getBlockModel() {
        return blockModel;
    }

    /**
     * Returns this block's persistent inventory.
     *
     * @return the {@link Inventory} representing the block inventory
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Returns if the block is activated
     *
     * @return {@link Boolean} if the block is activated
     */
    public boolean isActivated() {
        return isActivated;
    }

    /**
     * Sets the activated value
     *
     * @param isActivated the {@code boolean} argument; the activated value
     */
    public void setActivated(boolean isActivated) {
        if (this.isActivated == isActivated) {
            return;
        }

        this.isActivated = isActivated;
        this.isAnimating = true;
    }

    /**
     * Advances this block's activation animation by one update step.
     * Blocks without an animated model safely ignore the call.
     */
    public void animate() {
        if (!isAnimating || blockModel == null) {
            return;
        }

        float delta = GameMaster.game == null ? 0.0f : GameMaster.game.getGenDelta();
        if (delta <= 0.0f) return;

        float target = isActivated ? 1.0f : 0.0f;
        animationProgress = moveTowards(
                animationProgress, target, delta / ANIMATION_DURATION);
        applyAnimation();
        isAnimating = animationProgress != target;
    }

    /**
     * Uses the interactive block. Currently this toggles its activated state;
     * calling {@link #animate()} advances the corresponding visual transition.
     */
    public void use() {
        if (type != InteractiveBlocks.CHEST || GameMaster.game == null) return;

        setActivated(true);
        GameUIService.ui.getInventoryUI().openContainer(this);
    }

    /**
     * Checks whether the block is currently animating.
     *
     * @return {@code true} while an activation transition is in progress
     */
    public boolean isAnimating() {
        return isAnimating;
    }

    /**
     * Returns the normalized animation progress.
     *
     * @return {@code float}; a value between {@code 0} (closed) and {@code 1} (open)
     */
    public float getAnimationProgress() {
        return animationProgress;
    }

    /**
     * Returns the block orientation around the vertical axis.
     *
     * @return {@code float}; the orientation in radians
     */
    public float getOrientation() {
        return orientation;
    }

    private void applyAnimation() {
        if (type != InteractiveBlocks.CHEST) {
            return;
        }

        GLTFNode lid = blockModel.findNode("chest_top");
        if (lid != null) {
            lid.setRotation(new Quaternionf().rotateX(CHEST_OPEN_ANGLE * animationProgress));
            blockModel.updateTransforms();
        }
    }

    private static float moveTowards(float current, float target, float amount) {
        if (current < target) {
            return Math.min(current + amount, target);
        }
        return Math.max(current - amount, target);
    }

    /**
     * Returns the position of the block (x)
     *
     * @return {@link Integer} the position of the block (x)
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the position of the block (y)
     *
     * @return {@link Integer} the position of the block (y)
     */
    public int getY() {
        return y;
    }

    /**
     * Returns the position of the block (z)
     *
     * @return {@link Integer} the position of the block (z)
     */
    public int getZ() {
        return z;
    }

    /**
     * Sets the position of the block
     *
     * @param x the {@code int} argument; the position of the block (x)
     * @param y the {@code int} argument; the position of the block (y)
     * @param z the {@code int} argument; the position of the block (z)
     * @return {@link BlockPos} blockPos new object
     */
    public BlockPos setPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return new BlockPos(type, x, y, z);
    }
}
