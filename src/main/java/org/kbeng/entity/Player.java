package org.kbeng.entity;

import org.kbeng.data.*;
import org.kbeng.entity.plyr.PlayerGameplay;
import org.kbeng.entity.plyr.PlayerManager;
import org.kbeng.graphics.ResourceManager;
import org.kbeng.graphics.gltf.GLTFModel;
import org.kbeng.item.Item;
import org.kbeng.item.Shield;
import org.kbeng.pathfinding.GridPos;
import org.kbeng.service.SoundService;
import org.kbeng.service.NPCService;
import org.kbeng.utils.DeathManager;
import org.kbeng.utils.Settings;
import org.kbeng.wrld.GameMaster;
import org.kbeng.wrld.World;
import org.joml.Vector3f;

import java.util.List;

/**
 * Local controllable {@link Character} implementation that binds input, combat, and interaction systems.
 * The player coordinates movement and camera-driven orientation, auto-jump and collision traversal,
 * inventory/equipment actions, and attack execution through dedicated helper components
 * ({@link PlayerGameplay} and {@link PlayerManager}). A singleton instance ({@link #plyr}) is kept as
 * the authoritative runtime avatar accessed by gameplay and rendering subsystems.
 */
@Singleton
public class Player extends Character {
    private static final float AUTO_JUMP_CLEARANCE = 1.05f;
    private static final float WALKABLE_STEP_HEIGHT = 0.5f;
    private static final float COLLISION_EPSILON = 0.001f;
    public static final Player plyr;
    static {
        plyr = new Player();
        plyr.initialize();
    }

    private final GLTFModel playerModel;
    private final CharacterAnimator animator;
    private final PlayerGameplay gameplay;
    private final PlayerManager manager;
    private NPC focusTarget;

    /**
     * Creates and initializes a player.
     */
    private Player() {
        super(null, true);
        this.playerModel = ResourceManager.rem.getPlayerModel();
        this.gameplay = new PlayerGameplay();
        this.manager = new PlayerManager();
        this.animator = new CharacterAnimator(this);
    }

    /**
     * Initializes singleton components after the shared instance is assigned.
     */
    private void initialize() {
        gameplay.initialize();
        manager.initialize();
        animator.initialize(playerModel);
    }

    /**
     * Updates this object for the current simulation step.
     * {@inheritDoc}
     */
    @Override
    public void update(BlockPos blockPos, float delta) {
        super.update(blockPos, delta);
        if (!gameplay.updateLifeCycle(delta)) {
            animator.update(playerModel, delta, isFocusing());
            return;
        }

        if (GameMaster.game != null && GameMaster.game.isInventoryOpen()) {
            animator.update(playerModel, delta, isFocusing());
            return;
        }

        if (gameplay.checkOceanDrowning()) return;
        manager.update(delta);
        animator.update(playerModel, delta, isFocusing());
        gameplay.update(delta);
    }

    /**
     * Renders this object using the active graphics state.
     * {@inheritDoc}
     */
    @Override
    public void render(GameMaster game, RenderPass pass) {
        animator.render(game, playerModel, pass);
    }

    @Override
    public void focus(Entity entity) {
        NPC npc = entity instanceof NPC candidate && candidate.isAlive() ? candidate : null;
        focusTarget = npc == focusTarget ? null : npc;
    }

    /** Returns the currently locked NPC, clearing stale targets automatically. */
    public NPC getFocusTarget() {
        if (focusTarget != null
                && (!focusTarget.isAlive() || !NPCService.npcs.contains(focusTarget))) {
            focusTarget = null;
        }
        return focusTarget;
    }

    /** Returns whether the player currently has a valid target lock. */
    public boolean isFocusing() {
        return getFocusTarget() != null;
    }

    /** Returns whether the supplied NPC is the player's current focus target. */
    public boolean isFocusedOn(NPC npc) {
        return npc != null && getFocusTarget() == npc;
    }

    /**
     * Handles damage taken and updates the affected state.
     * {@inheritDoc}
     */
    @Override
    public void onDamageTaken(float amount) {
        gameplay.onDamageTaken(amount);
        if (GameMaster.game != null && GameMaster.game.getCamera() != null) {
            GameMaster.game.getCamera().applyDamageTilt(amount);
        }
    }

    /** Applies shield defense before entity-originated damage and knockback. */
    @Override
    public void damage(float amount, Entity attacker) {
        float remaining = gameplay.absorbWithShield(amount);
        if (remaining > 0.0f) super.damage(remaining, attacker);
    }

    /**
     * Records the cause attached to the lethal damage event.
     * Handles death and updates the affected state.
     * {@inheritDoc}
     */
    @Override
    protected void onDeath(Cause cause) {
        DeathManager.dth.setCauseOfDeath(cause);
    }

    /**
     * Transfers or creates the relevant entity or item for drop loot.
     * {@inheritDoc}
     */
    @Override
    protected void dropLoot() {
        gameplay.dropLoot();
    }

    /**
     * Updates movement for adjust velocity according to the current physics and input state.
     * {@inheritDoc}
     */
    @Override
    protected void adjustVelocity(float delta) {
        manager.adjustVelocity(delta);
    }

    /** Toggles the dedicated left-hand shield equipment slot. */
    public boolean toggleShield(Item selectedItem) {
        return gameplay.toggleShield(selectedItem);
    }

    /** Sets whether the equipped shield is being actively used. */
    public void setShieldRaised(boolean isRaised) {
        gameplay.setShieldRaised(isRaised);
    }

    /** Returns whether the shield is currently raised. */
    public boolean isShieldRaised() {
        return gameplay.isShieldRaised();
    }

    /** Returns the shield mounted on the left arm, if present. */
    public Shield getEquippedShield() {
        return getInventory().getShield();
    }

    /**
     * Starts the attack animation.
     */
    public void interact() {
        animator.interact();
    }

    /**
     * Determines whether attacking is satisfied by the current state.
     * @return {@code true} if an attack animation is active; otherwise {@code false}
     */
    public boolean isAttacking() {
        return animator.isAttacking();
    }

    /**
     * Determines whether aiming is satisfied by the state and damage
     * @return {@code true} if aiming has damage; otherwise {@code false}
     */
    public float getAttack() {
        Item selectedItem = Settings.selectedItem;
        if (selectedItem == null) return 0.0f;
        if (!(selectedItem instanceof ToolType toolType)) return 1.0f;
        if (toolType.getBaseDamage() <= 0) return 1.0f;
        return getStrength() * toolType.getBaseDamage();
    }

    /**
     * Updates or derives runtime state for change state according to the supplied arguments.
     * @param state the {@link PlayerState} argument; state to enter
     */
    public void changeState(PlayerState state) {
        manager.changeState(state);
    }

    /**
     * Updates movement for auto jump according to the current physics and input state.
     * @param velocity the {@link Vector3f} supplied as {@code velocity}
     * @param delta the {@code float} argument; frame time
     */
    public void autoJump(Vector3f velocity, float delta) {
        if (!isOnGround() || (velocity.x == 0.0f && velocity.z == 0.0f)) return;

        World world = World.wrld;
        Vector3f originalPosition = new Vector3f(getPosition());
        getPosition().add(velocity.x * delta, 0.0f, velocity.z * delta);
        if (!checkCollision(world)) {
            setPosition(originalPosition);
            return;
        }

        if (collidesBlock(world)) {
            float stepSurface = findSurface(world, originalPosition.y);
            if (stepSurface != Float.NEGATIVE_INFINITY) {
                getPosition().y = stepSurface;
                boolean clear = !checkCollision(world);
                setPosition(originalPosition);
                if (clear) getPosition().y = stepSurface;
            } else {
                setPosition(originalPosition);
            }
            return;
        }

        getPosition().y += AUTO_JUMP_CLEARANCE;
        boolean clear = !checkCollision(world);
        setPosition(originalPosition);
        if (clear) jump();
    }

    /**
     * Returns whether the current prospective collision is with a slab, a
     * staircase, or a fence. These shapes are never auto-jump targets.
     */
    private boolean collidesBlock(World world) {
        float halfWidth = getDimensions().x / 2.0f - COLLISION_EPSILON;
        float halfDepth = getDimensions().z / 2.0f - COLLISION_EPSILON;
        int minX = (int) Math.floor(getPosition().x - halfWidth);
        int maxX = (int) Math.floor(getPosition().x + halfWidth);
        int minY = (int) Math.floor(getPosition().y + COLLISION_EPSILON);
        int maxY = (int) Math.floor(getPosition().y + getDimensions().y - COLLISION_EPSILON);
        int minZ = (int) Math.floor(getPosition().z - halfDepth);
        int maxZ = (int) Math.floor(getPosition().z + halfDepth);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockData block = BlockData.fromId(world.getBlockTypeAt(x, y, z));
                    if (block != null && (block.isSlab() || block.isStaircase() || block.isFence())
                            && intersectsBlock(world.getBlockShapeAt(x, y, z), x, y, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Finds a nearby special-block surface that can be walked up without a
     * jump. A player may rise at most half a block per movement step.
     */
    private float findSurface(World world, float currentY) {
        float halfWidth = getDimensions().x / 2.0f - COLLISION_EPSILON;
        float halfDepth = getDimensions().z / 2.0f - COLLISION_EPSILON;
        float[] xs = {getPosition().x - halfWidth, getPosition().x + halfWidth};
        float[] zs = {getPosition().z - halfDepth, getPosition().z + halfDepth};
        float highestSurface = Float.NEGATIVE_INFINITY;
        int minY = (int) Math.floor(currentY - COLLISION_EPSILON);
        int maxY = (int) Math.floor(currentY + WALKABLE_STEP_HEIGHT);
        for (float x : xs) {
            for (float z : zs) {
                int blockX = (int) Math.floor(x);
                int blockZ = (int) Math.floor(z);
                for (int y = minY; y <= maxY; y++) {
                    BlockData block = BlockData.fromId(world.getBlockTypeAt(blockX, y, blockZ));
                    if (block == null || !(block.isSlab() || block.isStaircase())) continue;
                    highestSurface = Math.max(highestSurface,
                            world.getBlockSurfaceY(blockX, y, blockZ, x, z));
                }
            }
        }
        return highestSurface > currentY + COLLISION_EPSILON
                && highestSurface <= currentY + WALKABLE_STEP_HEIGHT + COLLISION_EPSILON
                ? highestSurface : Float.NEGATIVE_INFINITY;
    }

    /**
     * Returns current state according to the current object state.
     * @return the {@link PlayerState} result; active state
     */
    public PlayerState getCurrentState() {
        return manager.getCurrentState();
    }

    /**
     * Sets current state and updates the associated state.
     * @param state the {@link PlayerState} argument; state to store
     */
    public void setCurrentState(PlayerState state) {
        manager.setCurrentState(state);
    }

    /**
     * Respawns the player.
     */
    public void respawn() {
        gameplay.respawn();
    }

    /**
     * Resets attributes.
     */
    public void resetAttributes() {
        gameplay.resetAttributes();
    }

    /**
     * Returns damage sequence according to the current object state.
     * @return {@code int}; damage event sequence
     */
    public int getDamageSequence() {
        return gameplay.getDamageSequence();
    }

    /**
     * Updates movement for move according to the current physics and input state.
     * @param delta the {@code float} argument; frame time
     */
    public void move(float delta) {
        manager.move(delta);
    }

    /**
     * Updates movement for wasd according to the current physics and input state.
     * @param delta the {@code float} argument; frame time
     * @param yaw the {@code float} argument; camera yaw
     * @param flying the {@code boolean} argument; flight flag
     */
    public void wasd(float delta, float yaw, boolean flying) {
        manager.wasd(delta, yaw, flying);
    }

    /**
     * Updates movement for fly according to the current physics and input state.
     * @param delta the {@code float} argument; frame time
     * @param yaw the {@code float} argument; camera yaw
     * @param flying the {@code boolean} argument; flight flag
     */
    public void fly(float delta, float yaw, boolean flying) {
        manager.fly(delta, yaw, flying);
    }

    /**
     * Determines whether ground below is satisfied by the current state.
     * @return {@code true} if support exists below the proposed position; otherwise {@code false}
     */
    public boolean hasGroundBelow(float x, float z) {
        return manager.hasGroundBelow(x, z);
    }

    /**
     * Processes sell and updates the affected inventory or currency balances.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} argument; quantity
     */
    public void sell(Item item, int amount) {
        gameplay.sell(item, amount);
    }

    /**
     * Adds the supplied element to the corresponding collection or processing queue.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} argument; quantity
     */
    public void add(Item item, int amount) {
        gameplay.add(item, amount);
    }

    /**
     * Adds the supplied element to the corresponding collection or processing queue.
     * @param item the {@link Item} supplied as {@code item}
     */
    public void add(Item item) {
        gameplay.add(item);
    }

    /**
     * Adds to backpack to the corresponding collection or processing queue.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} argument; quantity
     */
    public void addToBackpack(Item item, int amount) {
        gameplay.addToBackpack(item, amount);
    }

    /**
     * Adds to backpack to the corresponding collection or processing queue.
     * @param item the {@link Item} supplied as {@code item}
     */
    public void addToBackpack(Item item) {
        gameplay.addToBackpack(item);
    }

    /**
     * Removes from backpack and updates any dependent state.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} argument; quantity
     */
    public void removeFromBackpack(Item item, int amount) {
        gameplay.removeFromBackpack(item, amount);
    }

    /**
     * Removes from backpack and updates any dependent state.
     * @param item the {@link Item} supplied as {@code item}
     */
    public void removeFromBackpack(Item item) {
        gameplay.removeFromBackpack(item);
    }

    /**
     * Sorts inventory storage.
     */
    public void sort() {
        gameplay.sort();
    }

    /**
     * Removes the supplied element and updates any dependent state.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} argument; quantity
     */
    public void remove(Item item, int amount) {
        gameplay.remove(item, amount);
    }

    /**
     * Removes the supplied element and updates any dependent state.
     * @param item the {@link Item} supplied as {@code item}
     */
    public void remove(Item item) {
        gameplay.remove(item);
    }

    /**
     * Clears droppable inventory items.
     */
    public void clear() {
        gameplay.clear();
    }

    /**
     * Determines whether this object contains no elements or active content.
     * @return {@code true} if inventory is empty; otherwise {@code false}
     */
    public boolean isEmpty() {
        return gameplay.isEmpty();
    }

    /**
     * Returns the number or extent represented by size.
     * @return {@code int}; inventory size
     */
    public int size() {
        return gameplay.size();
    }

    /**
     * Returns the value identified by the supplied key, index, or current object state.
     * @param index the {@code int} supplied as {@code index}
     * @return the {@link Item} result; indexed item
     */
    public Item get(int index) {
        return gameplay.get(index);
    }

    /**
     * Returns the value identified by the supplied key, index, or current object state.
     * @param item the {@link Item} argument; key
     * @return the {@link Item} result; matching item
     */
    public Item get(Item item) {
        return gameplay.get(item);
    }

    /**
     * Returns amount according to the current object state.
     * @param item the {@link Item} supplied as {@code item}
     * @return {@code int}; quantity
     */
    public int getAmount(Item item) {
        return gameplay.getAmount(item);
    }

    /**
     * Processes earn and updates the affected inventory or currency balances.
     * @param amount the {@code int} argument; currency amount
     */
    public void earn(int amount) {
        if (hasWallet() == null) return;
        gameplay.earn(amount);
        SoundService.fx.playUseSound(SoundGroup.CASH);
    }

    /**
     * Processes spend and updates the affected inventory or currency balances.
     * @param amount the {@code int} argument; currency amount
     */
    public void spend(int amount) {
        if (hasWallet() == null) return;
        gameplay.spend(amount);
    }

    /**
     * Determines whether space is satisfied by the current state.
     * @return {@code true} if storage has space; otherwise {@code false}
     */
    public boolean hasSpace() {
        return gameplay.hasSpace();
    }

    /**
     * Determines whether seeds is satisfied by the current state.
     * @return {@code true} if seeds are available; otherwise {@code false}
     */
    public boolean hasSeeds() {
        return gameplay.hasSeeds();
    }

    /**
     * Returns current eye height according to the current object state.
     * @return {@code float}; interpolated eye height
     */
    public float getCurrentEyeHeight() {
        return manager.getCurrentEyeHeight();
    }

    /**
     * Returns forward according to the current object state.
     * @return {@code float}; forward angle
     */
    public float getForward() {
        return manager.getForward();
    }

    /**
     * Returns direction according to the current object state.
     * @return the {@link Direction} result; facing direction
     */
    public Direction getDirection() {
        return animator.getDirection();
    }

    /**
     * Determines whether following path is satisfied by the current state.
     * @return {@code true} if a path remains; otherwise {@code false}
     */
    public boolean isFollowingPath() {
        return manager.isFollowingPath();
    }

    /**
     * Returns path according to the current object state.
     * @return the {@link List} result; active path
     */
    public List<GridPos> getPath() {
        return manager.getPath();
    }

    /**
     * Sets path and updates the associated state.
     * @param path the {@link List} argument; path to follow
     */
    public void setPath(List<GridPos> path) {
        manager.setPath(path);
    }

    /**
     * Returns path index according to the current object state.
     * @return {@code int}; path index
     */
    public int getPathIndex() {
        return manager.getPathIndex();
    }

    /**
     * Sets path index and updates the associated state.
     * @param index the {@code int} argument; path index
     */
    public void setPathIndex(int index) {
        manager.setPathIndex(index);
    }

    /**
     * Clears the path.
     */
    public void clearPath() {
        manager.clearPath();
    }

    /**
     * Returns respawn timer according to the current object state.
     * @return {@code float}; respawn timer
     */
    public float getRespawnTimer() {
        return gameplay.getRespawnTimer();
    }

    /**
     * Sets respawn timer and updates the associated state.
     * @param timer the {@code float} argument; respawn timer
     */
    public void setRespawnTimer(float timer) {
        gameplay.setRespawnTimer(timer);
    }

    /**
     * Returns target eye height according to the current object state.
     * @return {@code float}; target eye height
     */
    public float getTargetEyeHeight() {
        return manager.getTargetEyeHeight();
    }

    /**
     * Sets target eye height and updates the associated state.
     * @param height the {@code float} argument; target eye height
     */
    public void setTargetEyeHeight(float height) {
        manager.setTargetEyeHeight(height);
    }

    /**
     * Determines whether falling is satisfied by the current state.
     * @return {@code true} if falling; otherwise {@code false}
     */
    public boolean isFalling() {
        return manager.isFalling();
    }

    /**
     * Sets falling and updates the associated state.
     * @param falling the {@code boolean} argument; falling flag
     */
    public void setFalling(boolean falling) {
        manager.setFalling(falling);
    }

    /**
     * Returns difficulty regen according to the current object state.
     * @return {@code float}; regeneration multiplier
     */
    public float getDifficultyRegen() {
        return gameplay.getDifficultyRegen();
    }
}
