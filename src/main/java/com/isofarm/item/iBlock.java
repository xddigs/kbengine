package com.isofarm.item;

import com.isofarm.data.BlockPos;
import com.isofarm.data.DataClass;
import com.isofarm.data.Inventory;
import com.isofarm.data.BlockData;
import com.isofarm.graphics.ResourceManager;
import com.isofarm.graphics.gltf.GLTFNode;
import com.isofarm.graphics.gltf.GLTFModel;
import com.isofarm.service.SoundService;
import com.isofarm.ui.GameUIService;
import com.isofarm.wrld.GameMaster;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A craftable block with a model and interactive state.
 */
@SuppressWarnings("all")
@DataClass
public class iBlock implements Craftable {
    private static final float ANIMATION_DURATION = 0.10f;
    private static final float CHEST_OPEN_ANGLE = (float) Math.toRadians(35.0);
    private static final float DOOR_OPEN_ANGLE = (float) Math.toRadians(90.0);
    private static final float DOOR_MIN_Z = 0.0625f;
    private static final float DOOR_DEPTH = 0.0625f;
    private static final float DOOR_MODEL_MIN_X = -0.5f;
    private static final float DOOR_MODEL_MIN_Z = -0.4375f;
    private static final float DOOR_MODEL_HINGE_X = DOOR_MODEL_MIN_X + 1.0f;
    private static final float DOOR_MODEL_HINGE_Z = DOOR_MODEL_MIN_Z + DOOR_DEPTH * 0.5f;

    private final BlockData type;
    private final GLTFModel blockModel;
    private final Inventory inventory;
    private int x, y, z;
    private boolean isActivated;
    private boolean isAnimating;
    private float animationProgress;
    private float orientation;

    /**
     * Creates an interactive block at the supplied position.
     * @param type interactive {@link BlockData} type
     * @param x world x coordinate
     * @param y world y coordinate
     * @param z world z coordinate
     */
    public iBlock(BlockData type, int x, int y, int z) {
        this(type, x, y, z, 0.0f);
    }

    /**
     * Creates an interactive block at the supplied position and orientation.
     *
     * @param type the {@link BlockData} argument; the interactive block type
     * @param x the {@code int} argument; the x coordinate
     * @param y the {@code int} argument; the y coordinate
     * @param z the {@code int} argument; the z coordinate
     * @param orientation the {@code float} argument; the rotation around the vertical axis, in radians
     */
    public iBlock(BlockData type, int x, int y, int z, float orientation) {
        if (type == null || !type.isInteractive()) {
            throw new IllegalArgumentException("Interactive BlockData required");
        }
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

    /**
     * Creates an interactive block at a stored block position.
     * @param type interactive {@link BlockData} type
     * @param pos world position
     */
    public iBlock(BlockData type, BlockPos pos) {
        this(type, pos.x(), pos.y(), pos.z());
    }

    /**
     * Creates an unplaced interactive inventory item.
     * @param type interactive {@link BlockData} type
     */
    public iBlock(BlockData type) {
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
     * @return {@link BlockData} the type of block
     */
    public BlockData getType() {
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
     * Uses the interactive block and starts its corresponding interaction.
     */
    public void use() {
        if (GameMaster.game == null) return;

        switch (type) {
            case CHEST -> {
                setActivated(true);
                GameUIService.ui.getInventoryUI().openContainer(this);
            }
            case OAK_DOOR, SPRUCE_DOOR -> {
                setActivated(!isActivated);
                SoundService.fx.playUseSound(type.getSoundGroup(), isActivated ? 0 : 1);
            }
            default -> { }
        }
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

    /**
     * Builds the world transform for this block's model. Door models use their
     * corner origin as a hinge and are kept inside the occupied cell for every
     * cardinal orientation.
     *
     * @param destination matrix to populate
     * @return the populated matrix
     */
    public Matrix4f getModelTransform(Matrix4f destination) {
        if (!type.isDoor()) {
            return destination.identity()
                    .translate(x + 0.5f, y, z + 0.5f)
                    .rotateY(orientation);
        }

        return getClosedDoorTransform(destination)
                .translate(DOOR_MODEL_HINGE_X, 0.0f, DOOR_MODEL_HINGE_Z)
                .rotateY(DOOR_OPEN_ANGLE * animationProgress)
                .translate(-DOOR_MODEL_HINGE_X, 0.0f, -DOOR_MODEL_HINGE_Z);
    }

    /**
     * Builds an outline transform matching the complete animated door model.
     *
     * @param destination matrix to populate
     * @return the populated matrix
     */
    public Matrix4f getSelectionTransform(Matrix4f destination) {
        return getModelTransform(destination)
                .translate(DOOR_MODEL_MIN_X, 0.0f, DOOR_MODEL_MIN_Z)
                .scale(1.0f, 2.0f, DOOR_DEPTH);
    }

    /**
     * Tests an entity AABB against the animated, oriented door panel.
     * @return {@code true} if the entity intersects the door panel; otherwise {@code false}
     */
    public boolean intersects(float minX, float minY, float minZ,
                              float maxX, float maxY, float maxZ) {
        if (!type.isDoor() || maxY <= y || minY >= y + 2.0f) return false;

        Matrix4f inverse = getSelectionTransform(new Matrix4f()).invert();
        float localMinX = Float.POSITIVE_INFINITY;
        float localMinY = Float.POSITIVE_INFINITY;
        float localMinZ = Float.POSITIVE_INFINITY;
        float localMaxX = Float.NEGATIVE_INFINITY;
        float localMaxY = Float.NEGATIVE_INFINITY;
        float localMaxZ = Float.NEGATIVE_INFINITY;

        for (float worldX : new float[]{minX, maxX}) {
            for (float worldY : new float[]{minY, maxY}) {
                for (float worldZ : new float[]{minZ, maxZ}) {
                    Vector3f local = inverse.transformPosition(
                            new Vector3f(worldX, worldY, worldZ));
                    localMinX = Math.min(localMinX, local.x);
                    localMinY = Math.min(localMinY, local.y);
                    localMinZ = Math.min(localMinZ, local.z);
                    localMaxX = Math.max(localMaxX, local.x);
                    localMaxY = Math.max(localMaxY, local.y);
                    localMaxZ = Math.max(localMaxZ, local.z);
                }
            }
        }
        return localMaxX >= 0.0f && localMinX <= 1.0f
                && localMaxY >= 0.0f && localMinY <= 1.0f
                && localMaxZ >= 0.0f && localMinZ <= 1.0f;
    }

    /**
     * Returns the distance along a ray to the animated door model.
     */
    public float rayIntersection(Vector3f origin, Vector3f direction) {
        if (!type.isDoor()) return Float.POSITIVE_INFINITY;
        Matrix4f inverse = getSelectionTransform(new Matrix4f()).invert();
        Vector3f localOrigin = inverse.transformPosition(new Vector3f(origin));
        Vector3f localDirection = inverse.transformDirection(new Vector3f(direction));
        float[] origins = {localOrigin.x, localOrigin.y, localOrigin.z};
        float[] directions = {localDirection.x, localDirection.y, localDirection.z};
        float[] minimums = {0.0f, 0.0f, 0.0f};
        float[] maximums = {1.0f, 1.0f, 1.0f};
        float near = 0.0f;
        float far = Float.POSITIVE_INFINITY;

        for (int axis = 0; axis < 3; axis++) {
            float axisDirection = directions[axis];
            if (Math.abs(axisDirection) < 0.000001f) {
                if (origins[axis] < minimums[axis] || origins[axis] > maximums[axis]) {
                    return Float.POSITIVE_INFINITY;
                }
                continue;
            }

            float first = (minimums[axis] - origins[axis]) / axisDirection;
            float second = (maximums[axis] - origins[axis]) / axisDirection;
            if (first > second) {
                float swap = first;
                first = second;
                second = swap;
            }
            near = Math.max(near, first);
            far = Math.min(far, second);
            if (near > far) return Float.POSITIVE_INFINITY;
        }
        return far >= 0.0f ? near : Float.POSITIVE_INFINITY;
    }

    /**
     * Returns whether this block currently blocks movement.
     */
    public boolean isSolid() {
        return !type.isDoor() || !isActivated;
    }

    /** Returns the closed-door transform before the panel swings around its hinge. */
    private Matrix4f getClosedDoorTransform(Matrix4f destination) {
        return destination.identity()
                .translate(x + 0.5f, y, z + 0.5f)
                .rotateY(orientation);
    }

    /**
     * Applies the current animation progress to the block model. In this case,
     * {@link BlockData#CHEST} blocks only.
     */
    private void applyAnimation() {
        if (type != BlockData.CHEST) {
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
