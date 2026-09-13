package org.kbeng.rpg.entity;

import org.kbeng.rpg.data.BlockPos;
import org.kbeng.rpg.data.DataClass;
import org.kbeng.rpg.data.RenderPass;
import org.kbeng.rpg.graphics.ResourceManager;
import org.kbeng.engine.graphics.SpriteSheet;
import org.kbeng.rpg.item.Item;
import org.kbeng.rpg.wrld.GameMaster;
import org.kbeng.rpg.wrld.World;
import org.joml.Vector3f;

/**
 * WorldItem provides world item capabilities within the entity subsystem.
 * It participates in actor simulation, state transitions, and per-frame world interaction contracts.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Entity, inheriting shared behavior while specializing subsystem-specific logic.
 */
@DataClass
public class WorldItem extends Entity {
    private static final float GRAVITY = -20.0f;
    private static final float GROUND_OFFSET = 0.02f;
    private static final float ITEM_HEIGHT = 0.45f;
    private static final float PICKUP_DELAY = 0.75f;
    private static final float AIR_DRAG = 2.5f;
    private static final float GROUND_FRICTION = 8.0f;
    private static final float BOUNCE_FACTOR = 0.20f;
    private static final float MIN_BOUNCE_VELOCITY = 1.2f;
    private static final float ROTATION_SPEED = 60.0f;
    private static final float GROUND_BOB_SPEED = 3.0f;
    private static final float GROUND_BOB_HEIGHT = 0.08f;
    private final Item item;
    private int amount;
    private float rotation;
    private float bobTime;
    private float pickupTimer;
    private float groundY;
    private boolean isAttracting = false;

    /**
     * Creates a new {@code WorldItem} instance.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} supplied as {@code amount}
     * @param position the {@link Vector3f} supplied as {@code position}
     */
    public WorldItem(Item item, int amount, Vector3f position) {
        super(item.getName());
        this.item = item;
        this.amount = amount;
        setPosition(new Vector3f(position));
        setVelocity(new Vector3f());
        setOnGround(false);
        this.rotation = 0.0f;
        this.bobTime = 0.0f;
        this.pickupTimer = PICKUP_DELAY;
        this.groundY = position.y;
        this.maxHitpoints = 1;
        this.hitpoints = maxHitpoints;
    }

    /**
     * {@inheritDoc}
     * Updates the current state.
     * @param blockPos the {@link BlockPos} supplied as {@code blockPos}
     * @param delta the {@code float} supplied as {@code delta}
     */
    @Override
    public void update(BlockPos blockPos, float delta) {
        if (delta <= 0.0f) {
            return;
        }
        if (pickupTimer > 0.0f) {
            pickupTimer -= delta;
        }
        rotation = (rotation + ROTATION_SPEED * delta) % 360.0f;
        if (isAttracting) return;
        if (!isOnGround()) {
            velocity.y += GRAVITY * delta;
            float airDamping = Math.max(0.0f, 1.0f - AIR_DRAG * delta);
            velocity.x *= airDamping;
            velocity.z *= airDamping;
            position.x += velocity.x * delta;
            position.y += velocity.y * delta;
            position.z += velocity.z * delta;
            float targetGroundY = findGroundY();
            if (position.y <= targetGroundY) {
                position.y = targetGroundY;
                if (Math.abs(velocity.y) > MIN_BOUNCE_VELOCITY) {
                    velocity.y = -velocity.y * BOUNCE_FACTOR;
                } else {
                    velocity.set(0.0f, 0.0f, 0.0f);
                    setOnGround(true);
                    groundY = targetGroundY;
                    bobTime = 0.0f;
                }
            }
        } else {
            position.y = groundY + (float) Math.sin(bobTime) * GROUND_BOB_HEIGHT;
            bobTime += delta * GROUND_BOB_SPEED;
            float friction = Math.max(0.0f, 1.0f - GROUND_FRICTION * delta);
            velocity.x *= friction;
            velocity.z *= friction;
        }
    }

    /**
     * Finds and returns the ground y.
     * @return {@code float}; the located ground y
     */
    private float findGroundY() {
        int currentX = (int) Math.floor(position.x);
        int currentZ = (int) Math.floor(position.z);
        int startY = (int) Math.floor(position.y);
        for (int y = startY; y >= 0; y--) {
            float surfaceY = World.wrld.getBlockSurfaceY(
                    currentX, y, currentZ, position.x, position.z);
            if (surfaceY != Float.NEGATIVE_INFINITY)
                return surfaceY + GROUND_OFFSET + ITEM_HEIGHT * 0.5f;
        }
        return 0.0f;
    }

    /**
     * {@inheritDoc}
     * Renders this object in the requested render pass.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param pass the {@link RenderPass} supplied as {@code pass}
     */
    @Override
    public void render(GameMaster gameMaster, RenderPass pass) {
        SpriteSheet spriteSheet = ResourceManager.getItemSpriteSheet(item);
        if (spriteSheet == null) return;
        gameMaster.getItemRenderer().renderWorldItem(gameMaster,
                this, gameMaster.getCelestialLighting());
    }

    /**
     * Returns the item.
     * @return the {@link Item} representing the item
     */
    public Item getItem() {
        return item;
    }

    /**
     * Returns the amount.
     * @return {@code int}; the amount
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Sets the amount.
     * @param amount the {@code int} supplied as {@code amount}
     */
    public void setAmount(int amount) {
        this.amount = Math.max(0, amount);
    }

    /**
     * Adds the velocity.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param z the {@code float} supplied as {@code z}
     */
    public void addVelocity(float x, float y, float z) {
        this.velocity.add(x, y, z);
    }

    /**
     * Checks whether the be picked up condition is met.
     * @return {@code true} if be picked up; otherwise {@code false}
     */
    public boolean canBePickedUp() {
        return pickupTimer <= 0.0f;
    }

    /**
     * Returns the rotation.
     * @return {@code float}; the rotation
     */
    public float getRotation() {
        return rotation;
    }

    /**
     * Sets the rotation.
     * @param rotation the {@code float} supplied as {@code rotation}
     */
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    /**
     * Checks whether the attracting condition is met.
     * @return {@code true} if attracting; otherwise {@code false}
     */
    public boolean isAttracting() {
        return isAttracting;
    }

    /**
     * Sets the attracting.
     * @param attracting the {@code boolean} supplied as {@code attracting}
     */
    public void setAttracting(boolean attracting) {
        this.isAttracting = attracting;
    }
}
