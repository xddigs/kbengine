package com.isofarm.entity;

import com.isofarm.data.*;
import com.isofarm.item.iBlock;
import com.isofarm.utils.K;
import com.isofarm.wrld.GameMaster;
import com.isofarm.wrld.World;
import org.joml.Vector3f;

/**
 * Abstract base class for all physical entities within the 3D world space.
 *
 * <p>This class serves as the core foundation for actors (players, mobs, NPCs) and provides
 * built-in systems for:
 * <ul>
 *   <li><b>Transform & Bounds:</b> Axis-Aligned Bounding Box (AABB) spatial positioning,
 *       dimensions, and movement velocities via JOML {@link Vector3f}.</li>
 *   <li><b>Physics & Collision:</b> Voxel-based grid collision detection, step resolution,
 *       fluid submersion logic, and gravity integration.</li>
 *   <li><b>Combat & Vitality:</b> Hitpoints, defensive attributes, knockback calculations,
 *       and invulnerability timers.</li>
 *   <li><b>Environmental Hazards:</b> Real-time environmental triggers such as progressive
 *       lava burn damage and out-of-bounds void elimination.</li>
 * </ul>
 *
 * <p>Subclasses must implement {@link #render(GameMaster, RenderPass)} to define visual representation
 * and optionally override {@link #adjustVelocity(float)}, {@link #onDamageTaken(float)}, or
 * {@link #onDeath(Cause)} to hook custom entity behaviors.
 */

@DataClass
public abstract class Entity {
    protected static final float INVULNERABILITY_DURATION = 0.4f;
    private static final float LAVA_DAMAGE_INTERVAL = 1.0f;
    private static final float LAVA_DAMAGE_STEP = 0.1f;
    private static final float VOID_DEATH_Y = -16.0f;
    private static final float KNOCKBACK_STRENGTH = 8.0f;
    private static final float UPWARD_FORCE = 4.0f;

    private final byte id;
    protected Vector3f position;
    protected Vector3f velocity;
    protected Vector3f dimensions;

    protected float hitpoints;
    protected float maxHitpoints;
    protected float defense;
    protected float maxDefense;

    private String name;
    private float standingHeight;
    private float crouchingHeight;
    private boolean onGround;
    private boolean wasOnGround;

    private float speed;
    private float lavaDamageTimer;
    private int lavaDamageTicks;
    private float invulnerabilityTimer = 0.0f;

    /**
     * Creates a new {@code Entity} instance.
     * @param name the {@link String} supplied as {@code name}
     */
    public Entity(String name) {
        this.id = (byte) (Math.floor((Math.random() * Math.random()) * 100));
        this.name = name;
        this.position = new Vector3f();
        this.velocity = new Vector3f();
        this.dimensions = new Vector3f();

        this.standingHeight = 2.0f;
        this.crouchingHeight = 1.4f;

        this.onGround = false;
        this.wasOnGround = onGround;
        this.speed = 1.0f;
    }

    /**
     * Checks whether the alive condition is met.
     * @return {@code true} if alive; otherwise {@code false}
     */
    public boolean isAlive() {
        return hitpoints > 0;
    }

    /**
     * Applies damage to the entity.
     * @param amount the {@code float} argument; the damage amount
     */
    public void damage(float amount) {
        damage(amount, Cause.ENTITY);
    }

    /**
     * Applies damage attributed to a specific cause.
     * @param amount the {@code float} argument; the damage amount
     * @param cause the {@link Cause} argument; the damage cause
     */
    public void damage(float amount, Cause cause) {
        if (!isAlive() || amount <= 0) return;
        float previousHitpoints = hitpoints;
        hitpoints = Math.max(0.0f, hitpoints - amount);
        if (hitpoints < previousHitpoints) onDamageTaken(amount);
        if (hitpoints == 0.0f) onDeath(cause == null ? Cause.NULL : cause);
    }

    /**
     * Applies damage to the entity opposite to the {@code attacker}
     * @param amount amount of damage
     * @param attacker {@link Entity} supplied as {@code attacker}
     */
    public void damage(float amount, Entity attacker) {
        damage(amount, Cause.ENTITY);
        if (attacker != null) {
            float knockbackStrength = KNOCKBACK_STRENGTH;
            float upwardForce = UPWARD_FORCE;
            applyKnockback(attacker.getPosition(), knockbackStrength, upwardForce);
        }
    }

    /** Calculates the force pushed from the attack, upwards */
    public void applyKnockback(Vector3f sourcePosition, float strength, float upwardForce) {
        Vector3f knockbackDir = new Vector3f(this.position).sub(sourcePosition);
        knockbackDir.y = 0.0f;

        if (knockbackDir.lengthSquared() < 0.0001f) {
            knockbackDir.set(0, 0, 1);
        } else {
            knockbackDir.normalize();
        }

        this.velocity.x += knockbackDir.x * strength;
        this.velocity.y += upwardForce;
        this.velocity.z += knockbackDir.z * strength;
    }

    /**
     * Immediately kills the entity with the supplied cause.
     * @param cause the {@link Cause} argument; the death cause
     */
    public void kill(Cause cause) {
        if (!isAlive()) return;
        float previousHitpoints = hitpoints;
        hitpoints = 0.0f;
        onDamageTaken(previousHitpoints);
        onDeath(cause == null ? Cause.NULL : cause);
    }

    /**
     * Handles an applied damage event.
     * @param amount the {@code float} argument; the applied damage amount
     */
    protected void onDamageTaken(float amount) {}

    /**
     * Called once when damage reduces this entity's hitpoints to zero.
     */
    protected void onDeath(Cause cause) {}

    /**
     * Returns the maximum hitpoints used to scale environmental damage.
     * @return {@code float}; the maximum hitpoints
     */
    public float getMaxHitpoints() {
        return maxHitpoints;
    }

    /**
     * Returns the {@code defense} value
     * @return {@link float} value of defense
     */
    public float getDefense() {
        return defense;
    }

    /**
     * Sets the defense value
     * @return {@link Entity} value of defense
     */
    public Entity setDefense(float defense) {
        this.defense = defense;
        return this;
    }

    /**
     * Returns the {@code maxDefense} value
     *
     * @return {@link float} value of maxDefense
     */
    public float getMaxDefense() {
        return maxDefense;
    }

    /**
     * Sets the maxDefense value
     *
     * @return {@link Entity} value of maxDefense
     */
    public Entity setMaxDefense(float maxDefense) {
        this.maxDefense = maxDefense;
        return this;
    }

    /**
     * Applies increasingly severe damage while the entity remains in direct
     * contact with lava. Leaving the lava resets the damage progression.
     * @param world the {@link World} supplied as {@code world}
     * @param delta the {@code float} argument; the elapsed time in seconds
     */
    public final void updateEnvironmentalDamage(World world, float delta) {
        if (isAlive() && position.y < VOID_DEATH_Y) {
            kill(Cause.VOID);
            return;
        }

        if (!isAlive() || delta <= 0 || !isTouchingLava(world)) {
            lavaDamageTimer = 0;
            lavaDamageTicks = 0;
            return;
        }

        if (lavaDamageTicks == 0 && lavaDamageTimer == 0) {
            lavaDamageTimer = LAVA_DAMAGE_INTERVAL;
        } else {
            lavaDamageTimer += delta;
        }
        while (lavaDamageTimer >= LAVA_DAMAGE_INTERVAL && isAlive()) {
            lavaDamageTimer -= LAVA_DAMAGE_INTERVAL;
            lavaDamageTicks++;
            damage(Math.max(1.0f, getMaxHitpoints() * LAVA_DAMAGE_STEP * lavaDamageTicks),
                    Cause.BURN);
        }
    }

    /**
     * Checks whether the entity bounds touch the occupied portion of a lava cell.
     * @param world the {@link World} supplied as {@code world}
     * @return {@code true} when the entity touches lava; otherwise {@code false}
     */
    private boolean isTouchingLava(World world) {
        float epsilon = 0.001f;
        float minX = position.x - dimensions.x / 2.0f + epsilon;
        float maxX = position.x + dimensions.x / 2.0f - epsilon;
        float minY = position.y;
        float maxY = position.y + dimensions.y;
        float minZ = position.z - dimensions.z / 2.0f + epsilon;
        float maxZ = position.z + dimensions.z / 2.0f - epsilon;

        for (int x = (int) Math.floor(minX); x <= (int) Math.floor(maxX); x++) {
            for (int y = (int) Math.floor(minY); y <= (int) Math.floor(maxY); y++) {
                for (int z = (int) Math.floor(minZ); z <= (int) Math.floor(maxZ); z++) {
                    if (world.getBlockTypeAt(x, y, z) != BlockData.LAVA.getId()) continue;
                    float lavaTop = y + world.getFluidLevelAt(x, y, z) / 8.0f;
                    if (maxY > y && minY <= lavaTop + 0.05f) return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the id.
     * @return {@code byte}; the id
     */
    public byte getId() {
        return id;
    }

    /**
     * Returns the name.
     * @return the {@link String} representing the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     * @param name the {@link String} supplied as {@code name}
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the position.
     * @return the {@link Vector3f} representing the position
     */
    public Vector3f getPosition() {
        return position;
    }

    /**
     * Returns the position string.
     * @return the {@link String} representing the position string
     */
    public String getPositionString() {
        return String.format("X:%.2f // Y:%.2f // Z:%.2f",
                position.x, position.y, position.z);
    }

    /**
     * Sets the position.
     * @param position the {@link Vector3f} supplied as {@code position}
     */
    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    /**
     * Sets the position.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param z the {@code float} supplied as {@code z}
     */
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }

    /**
     * Returns the velocity.
     * @return the {@link Vector3f} representing the velocity
     */
    public Vector3f getVelocity() {
        return velocity;
    }

    /**
     * Sets the velocity.
     * @param velocity the {@link Vector3f} supplied as {@code velocity}
     */
    public void setVelocity(Vector3f velocity) {
        this.velocity.set(velocity);
    }

    /**
     * Sets the velocity.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param z the {@code float} supplied as {@code z}
     */
    public void setVelocity(float x, float y, float z) {
        this.velocity.set(x, y, z);
    }

    /**
     * Returns the dimensions.
     * @return the {@link Vector3f} representing the dimensions
     */
    public Vector3f getDimensions() {
        return dimensions;
    }

    /**
     * Sets the dimensions.
     * @param dimensions the {@link Vector3f} supplied as {@code dimensions}
     */
    public void setDimensions(Vector3f dimensions) {
        this.dimensions.set(dimensions);
    }

    /**
     * Sets the dimensions.
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     * @param depth the {@code float} supplied as {@code depth}
     */
    public void setDimensions(float width, float height, float depth) {
        this.dimensions.set(width, height, depth);

        if (standingHeight <= 0.0f) {
            standingHeight = height;
        }
    }

    /**
     * Returns the standing height.
     * @return {@code float}; the standing height
     */
    public float getStandingHeight() {
        return standingHeight;
    }

    /**
     * Sets the standing height.
     * @param standingHeight the {@code float} supplied as {@code standingHeight}
     */
    public void setStandingHeight(float standingHeight) {
        this.standingHeight = standingHeight;
    }

    /**
     * Returns the crouching height.
     * @return {@code float}; the crouching height
     */
    public float getCrouchingHeight() {
        return crouchingHeight;
    }

    /**
     * Sets the crouching height.
     * @param crouchingHeight the {@code float} supplied as {@code crouchingHeight}
     */
    public void setCrouchingHeight(float crouchingHeight) {
        this.crouchingHeight = crouchingHeight;
    }

    /**
     * Checks whether the on ground condition is met.
     * @return {@code true} if on ground; otherwise {@code false}
     */
    public boolean isOnGround() {
        return onGround;
    }

    /**
     * Updates or derives runtime state for was on ground according to the supplied arguments.
     * @return {@code boolean}; the was on ground result
     */
    public boolean wasOnGround() {
        return wasOnGround;
    }

    /**
     * Sets the on ground.
     * @param onGround the {@code boolean} supplied as {@code onGround}
     */
    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    /**
     * Sets the was on ground.
     * @param wasOnGround the {@code boolean} supplied as {@code wasOnGround}
     */
    public void setWasOnGround(boolean wasOnGround) {
        this.wasOnGround = wasOnGround;
    }

    /**
     * Checks whether the in fluid condition is met.
     * @param world the {@link World} supplied as {@code world}
     * @return {@code true} if in fluid; otherwise {@code false}
     */
    public boolean isInFluid(World world) {
        float epsilon = 0.001f;
        float minX = position.x - dimensions.x / 2.0f + epsilon;
        float maxX = position.x + dimensions.x / 2.0f - epsilon;

        float minY = position.y - 0.05f;
        float maxY = position.y + dimensions.y - epsilon;

        float minZ = position.z - dimensions.z / 2.0f + epsilon;
        float maxZ = position.z + dimensions.z / 2.0f - epsilon;

        int blockMinX = (int) Math.floor(minX);
        int blockMaxX = (int) Math.floor(maxX);
        int blockMinY = (int) Math.floor(minY);
        int blockMaxY = (int) Math.floor(maxY);
        int blockMinZ = (int) Math.floor(minZ);
        int blockMaxZ = (int) Math.floor(maxZ);

        for (int x = blockMinX; x <= blockMaxX; x++) {
            for (int y = blockMinY; y <= blockMaxY; y++) {
                for (int z = blockMinZ; z <= blockMaxZ; z++) {
                    byte blockId = world.getBlockTypeAt(x, y, z);
                    BlockData data = BlockData.fromId(blockId);
                    if (data != null && data.isFluid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the fluid submersion.
     * @param world the {@link World} supplied as {@code world}
     * @return {@code float}; the fluid submersion
     */
    private float getFluidSubmersion(World world) {
        float epsilon = 0.001f;

        float minX = position.x - dimensions.x / 2.0f + epsilon;
        float maxX = position.x + dimensions.x / 2.0f - epsilon;

        float minY = position.y - 0.1f;
        float maxY = position.y + dimensions.y;

        float minZ = position.z - dimensions.z / 2.0f + epsilon;
        float maxZ = position.z + dimensions.z / 2.0f - epsilon;

        int blockMinX = (int) Math.floor(minX);
        int blockMaxX = (int) Math.floor(maxX);

        int blockMinY = (int) Math.floor(minY);
        int blockMaxY = (int) Math.floor(maxY);

        int blockMinZ = (int) Math.floor(minZ);
        int blockMaxZ = (int) Math.floor(maxZ);

        float submergedHeight = 0.0f;

        for (int x = blockMinX; x <= blockMaxX; x++) {
            for (int y = blockMinY; y <= blockMaxY; y++) {
                for (int z = blockMinZ; z <= blockMaxZ; z++) {
                    byte blockId = world.getBlockTypeAt(x, y, z);
                    BlockData data = BlockData.fromId(blockId);
                    if (data == null || !data.isFluid()) {
                        continue;
                    }

                    float waterLevel = world.getFluidLevelAt(x, y, z) / 8.0f;
                    float waterTop = y + waterLevel;
                    float overlapMin = Math.max(position.y, y);
                    float overlapMax = Math.min(maxY, waterTop);
                    if (overlapMax > overlapMin || (position.y >= y && position.y <= waterTop + 0.1f)) {
                        submergedHeight += Math.max(0.1f, overlapMax - overlapMin);
                    }
                }
            }
        }

        return submergedHeight;
    }

    /**
     * Updates movement for collide according to the current physics and input state.
     * @param world the {@link World} supplied as {@code world}
     * @param targetVelocity the {@link Vector3f} supplied as {@code targetVelocity}
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void collide(World world, Vector3f targetVelocity, float delta) {
        float smooth = 1.0f - (float) Math.exp(-12.0f * delta);

        velocity.x += (targetVelocity.x - velocity.x) * smooth;
        velocity.z += (targetVelocity.z - velocity.z) * smooth;
        adjustVelocity(delta);

        float submergedHeight = getFluidSubmersion(world);
        boolean inFluid = submergedHeight > 0.0f;

        if (inFluid) {
            velocity.y += K.World.GRAVITY * 0.15f * delta;

            if (velocity.y < -2.0f) {
                velocity.y = -2.0f;
            }
        } else {
            velocity.y += K.World.GRAVITY * delta;
        }

        position.x += velocity.x * delta;
        if (checkCollision(world)) {
            position.x -= velocity.x * delta;
            velocity.x = 0.0f;
        }

        setOnGround(false);
        position.y += velocity.y * delta;
        if (checkCollision(world)) {
            position.y -= velocity.y * delta;

            if (velocity.y < 0.0f) {
                setOnGround(true);
            }

            velocity.y = 0.0f;
        }

        position.z += velocity.z * delta;
        if (checkCollision(world)) {
            position.z -= velocity.z * delta;
            velocity.z = 0.0f;
        }
    }

    /**
     * Updates movement for adjust velocity according to the current physics and input state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    protected void adjustVelocity(float delta) {}

    /**
     * Determines whether collision is satisfied by the current state.
     * @param world the {@link World} supplied as {@code world}
     * @return {@code boolean}; the check collision result
     */
    public boolean checkCollision(World world) {
        float epsilon = 0.001f;

        float minX = position.x - dimensions.x / 2.0f + epsilon;
        float maxX = position.x + dimensions.x / 2.0f - epsilon;

        float minY = position.y + epsilon;
        float maxY = position.y + dimensions.y - epsilon;

        float minZ = position.z - dimensions.z / 2.0f + epsilon;
        float maxZ = position.z + dimensions.z / 2.0f - epsilon;

        int blockMinX = (int) Math.floor(minX);
        int blockMaxX = (int) Math.floor(maxX);

        int blockMinY = (int) Math.floor(minY);
        int blockMaxY = (int) Math.floor(maxY);

        int blockMinZ = (int) Math.floor(minZ);
        int blockMaxZ = (int) Math.floor(maxZ);

        for (int x = blockMinX; x <= blockMaxX; x++) {
            for (int y = blockMinY; y <= blockMaxY; y++) {
                for (int z = blockMinZ; z <= blockMaxZ; z++) {
                    if (world.isFullCubeSolid(x, y, z)) {
                        return true;
                    }
                }
            }
        }

        return world.intersectsShapedBlocks(minX, minY, minZ, maxX, maxY, maxZ)
                || world.intersectsDoor(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Determines whether block is satisfied by the current state.
     * @param blockX the {@code int} supplied as {@code blockX}
     * @param blockY the {@code int} supplied as {@code blockY}
     * @param blockZ the {@code int} supplied as {@code blockZ}
     * @return {@code boolean}; the intersects block result
     */
    public boolean intersectsBlock(int blockX, int blockY, int blockZ) {
        float epsilon = 0.001f;

        float minX = position.x - dimensions.x / 2.0f + epsilon;
        float maxX = position.x + dimensions.x / 2.0f - epsilon;

        float minY = position.y + epsilon;
        float maxY = position.y + dimensions.y - epsilon;

        float minZ = position.z - dimensions.z / 2.0f + epsilon;
        float maxZ = position.z + dimensions.z / 2.0f - epsilon;

        float blockMaxX = blockX + 1.0f;
        float blockMaxY = blockY + 1.0f;
        float blockMaxZ = blockZ + 1.0f;

        return minX < blockMaxX &&
                maxX > blockX &&
                minY < blockMaxY &&
                maxY > blockY &&
                minZ < blockMaxZ &&
                maxZ > blockZ;
    }

    /** Tests this entity against the actual shape of a voxel block. */
    public boolean intersectsBlock(BlockData data, int blockX, int blockY, int blockZ) {
        if (data == null) return false;
        return intersectsBlock(data.getShape(), blockX, blockY, blockZ);
    }

    /** Tests this entity against an explicitly oriented block shape. */
    public boolean intersectsBlock(BlockShape shape, int blockX, int blockY, int blockZ) {
        if (shape == null) return false;
        float epsilon = 0.001f;
        float minX = position.x - dimensions.x / 2.0f + epsilon;
        float maxX = position.x + dimensions.x / 2.0f - epsilon;
        float minY = position.y + epsilon;
        float maxY = position.y + dimensions.y - epsilon;
        float minZ = position.z - dimensions.z / 2.0f + epsilon;
        float maxZ = position.z + dimensions.z / 2.0f - epsilon;
        return shape.intersects(blockX, blockY, blockZ,
                minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Tests this entity's collision box against an interactive model collider.
     */
    public boolean intersects(iBlock block) {
        if (block == null) return false;
        float epsilon = 0.001f;
        float minX = position.x - dimensions.x / 2.0f + epsilon;
        float maxX = position.x + dimensions.x / 2.0f - epsilon;
        float minY = position.y + epsilon;
        float maxY = position.y + dimensions.y - epsilon;
        float minZ = position.z - dimensions.z / 2.0f + epsilon;
        float maxZ = position.z + dimensions.z / 2.0f - epsilon;
        return block.intersects(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Starts a jump when the current movement state permits it.
     */
    public void jump() {
        if (!isOnGround()) return;
        velocity.y = K.World.JUMP_FORCE;
        setOnGround(false);
    }

    /**
     * Returns the speed.
     * @return {@code float}; the speed
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Sets the speed.
     * @param speed the {@code float} supplied as {@code speed}
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * Updates the current state.
     * @param blockPos the {@link BlockPos} supplied as {@code blockPos}
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void update(BlockPos blockPos, float delta) {
        if (invulnerabilityTimer > 0.0f) {
            invulnerabilityTimer = Math.max(0.0f, invulnerabilityTimer - delta);
        }
    }

    /**
     * Renders this object in the requested render pass.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     */
    public void render(GameMaster gameMaster) {
        render(gameMaster, RenderPass.NORMAL);
    }

    /**
     * Renders this object in the requested render pass.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param pass the {@link RenderPass} supplied as {@code pass}
     */
    public abstract void render(GameMaster gameMaster, RenderPass pass);

    /**
     * Transfers or creates the relevant entity or item for drop loot.
     */
    protected void dropLoot() {}

    /**
     * Retrieves {@code invulnerabilityTimer}
     * @return {@link float} value of invulnerabilityTimer
     */
    public float getInvulnerabilityTimer() {
        return invulnerabilityTimer;
    }

    /**
     * Sets the invulnerabilityTimer value
     * @return {@link Entity} value of invulnerabilityTimer
     */
    public Entity setInvulnerabilityTimer(float invulnerabilityTimer) {
        this.invulnerabilityTimer = invulnerabilityTimer;
        return this;
    }
}
