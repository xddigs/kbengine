package com.isofarm.entity;

import com.isofarm.data.*;
import com.isofarm.graphics.gltf.GLTFLoader;
import com.isofarm.graphics.gltf.GLTFModel;
import com.isofarm.item.Item;
import com.isofarm.service.SoundService;
import com.isofarm.service.TimeService;
import com.isofarm.utils.K;
import com.isofarm.utils.Naming;
import com.isofarm.wrld.GameMaster;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an NPC, with their own state and behavior, in contrast to {@link Player},
 * who starts with their own {@link Singleton} instance. Inherits from {@link Character}.
 */
@DataClass
public class NPC extends Character {
    private static final Logger log = LoggerFactory.getLogger(NPC.class);
    private static final float WALK_SPEED = 1.25f;
    private static final float MIN_IDLE_TIME = 2.0f;
    private static final float IDLE_TIME_VARIATION = 4.0f;
    private static final float WANDER_RADIUS = 5.0f;
    private static final float KNOCKBACK_DAMPING = 12.0f;
    private static final float DEFAULT_KNOCKBACK_TIMER = 0.25f;
    private final CharacterAnimator animator = new CharacterAnimator(this);
    private final NPCGender gender;
    private final Job job;
    private final GLTFModel npcModel;
    private final Vector3f home = new Vector3f();
    private final Vector3f destination = new Vector3f();
    private float idleTimer;
    private boolean isWalking;
    private float knockbackTimer = DEFAULT_KNOCKBACK_TIMER;

    private Character lastInteractor = null;
    private float interactionTimer = 0.0f;
    private static final float INTERACTION_DURATION = 4.0f;

    /**
     * Creates an NPC and loads the model registered by its job.
     * @param gender the voice and localized gender of the character
     * @param job the job that supplies its model and interaction type
     */
    public NPC(NPCGender gender, Job job) {
        super(Naming.nm.fullName());
        this.gender = gender;
        this.job = job;
        this.npcModel = GLTFLoader.load(job.getModelPath());
        animator.initialize(npcModel);
        setDimensions(0.6f, 1.8f, 0.6f);
        setMaxHitpoints(20);
        setHitpoints(20);
        setMaxStamina(100);
        setStamina(100);
        setGamemode(Gamemode.SURVIVAL);
        setSpeed(WALK_SPEED);
        chooseIdleDuration();
        if (job == Job.TRADER) {
            resetShop();
        }
    }

    /**
     * Creates a farmer NPC with the supplied gender.
     * @param gender the voice and localized gender of the character
     */
    public NPC(NPCGender gender) {
        this(gender, Job.FARMER);
    }

    /** Creates a female farmer NPC. */
    public NPC() {
        this(NPCGender.FEMALE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(BlockPos blockPos, float delta) {
        super.update(blockPos, delta);
        super.update();
        updateBehavior(delta);
        animator.update(npcModel, delta);
    }

    /** {@inheritDoc} */
    @Override
    public void render(GameMaster gameMaster, RenderPass pass) {
        if (!isAlive() || npcModel == null) return;
        animator.render(gameMaster, npcModel, pass);
    }

    /** {@inheritDoc} */
    @Override
    public void applyKnockback(Vector3f sourcePosition, float strength, float upwardForce) {
        super.applyKnockback(sourcePosition, strength, upwardForce);
        this.knockbackTimer = DEFAULT_KNOCKBACK_TIMER;
    }

    /** {@inheritDoc} */
    @Override
    public void damage(float amount, Entity attacker) {
        if (getInvulnerabilityTimer() > 0.0f) {
            return;
        }

        super.damage(amount, attacker);
        setInvulnerabilityTimer(INVULNERABILITY_DURATION);
        grunt();

        if (attacker instanceof Character character) {
            interactWith(character);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Character hasBeenInteractedWith() {
        if (interactionTimer > 0.0f) {
            return lastInteractor;
        }
        return null;
    }

    /**
     * Updates the NPC's behavior.
     * @param delta the {@code float} argument; frame time in seconds
     */
    private void updateBehavior(float delta) {
        if (interactionTimer > 0.0f) {
            interactionTimer -= delta;
            if (interactionTimer <= 0.0f) {
                lastInteractor = null;
            }
        }

        if (knockbackTimer > 0.0f) {
            knockbackTimer = Math.max(0.0f, knockbackTimer - delta);
            collide(GameMaster.game.getWorld(), new Vector3f(velocity.x, velocity.y, velocity.z), delta);

            float damping = (float) Math.exp(-KNOCKBACK_DAMPING * delta);
            velocity.x *= damping;
            velocity.z *= damping;
            return;
        }

        if (isWalking) {
            Vector3f offset = new Vector3f(destination).sub(position);
            offset.y = 0.0f;
            if (offset.lengthSquared() < 0.16f) {
                isWalking = false;
                setVelocity(0.0f, getVelocity().y, 0.0f);
                chooseIdleDuration();
            } else {
                offset.normalize(WALK_SPEED);
                setVelocity(offset.x, getVelocity().y, offset.z);
            }
        } else {
            idleTimer -= delta;
            setVelocity(0.0f, getVelocity().y, 0.0f);
            if (idleTimer <= 0.0f) chooseDestination();
        }
        collide(GameMaster.game.getWorld(), new Vector3f(velocity.x, velocity.y, velocity.z), delta);
    }

    /** Wanders randomly around the NPC's home point. */
    private void chooseDestination() {
        float angle = (float) (Math.random() * Math.PI * 2.0);
        float radius = (float) Math.sqrt(Math.random()) * WANDER_RADIUS;
        destination.set(home).add((float) Math.sin(angle) * radius, 0.0f,
                (float) Math.cos(angle) * radius);
        isWalking = true;
    }

    /**
     * Chooses a random duration for the NPC to idle.
     */
    private void chooseIdleDuration() {
        idleTimer = MIN_IDLE_TIME + (float) Math.random() * IDLE_TIME_VARIATION;
    }

    /**
     * Sets the center point used by the NPC's wandering behavior.
     * @param position the new world-space home and current position
     */
    public void setHome(Vector3f position) {
        setPosition(position);
        home.set(position);
        destination.set(position);
    }

    /**
     * Returns the distance where a ray enters this NPC's collision box.
     * @param origin the world-space ray origin
     * @param direction the normalized world-space ray direction
     * @return the ray distance, or positive infinity when it misses
     */
    public float rayIntersection(Vector3f origin, Vector3f direction) {
        float minX = position.x - dimensions.x * 0.5f;
        float maxX = position.x + dimensions.x * 0.5f;
        float minY = position.y;
        float maxY = position.y + dimensions.y;
        float minZ = position.z - dimensions.z * 0.5f;
        float maxZ = position.z + dimensions.z * 0.5f;
        float tMin = 0.0f;
        float tMax = Float.POSITIVE_INFINITY;
        float[] origins = {origin.x, origin.y, origin.z};
        float[] directions = {direction.x, direction.y, direction.z};
        float[] mins = {minX, minY, minZ};
        float[] maxes = {maxX, maxY, maxZ};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(directions[axis]) < 0.00001f) {
                if (origins[axis] < mins[axis] || origins[axis] > maxes[axis]) {
                    return Float.POSITIVE_INFINITY;
                }
                continue;
            }
            float first = (mins[axis] - origins[axis]) / directions[axis];
            float second = (maxes[axis] - origins[axis]) / directions[axis];
            if (first > second) {
                float swap = first;
                first = second;
                second = swap;
            }
            tMin = Math.max(tMin, first);
            tMax = Math.min(tMax, second);
            if (tMax < tMin) return Float.POSITIVE_INFINITY;
        }
        return tMin;
    }

    /** Plays this character's normal gender-specific voice response. */
    public void speak() {
        SoundService.fx.playNPCVoice(normalVoice());
    }

    /** Plays this character's gender-specific hurt response. */
    public void grunt() {
        SoundService fx = SoundService.fx;
        fx.playHitSound();
        fx.playNPCVoice(hurtVoice());
    }

    /** Returns the normal voice for this NPC, with non-binary voices alternating. */
    private NPCVoice normalVoice() {
        return switch (gender) {
            case FEMALE -> NPCVoice.HMM_FEMALE;
            case MALE -> NPCVoice.HMM_MALE;
            case NON_BINARY -> Math.random() < 0.5
                    ? NPCVoice.HMM_FEMALE : NPCVoice.HMM_MALE;
        };
    }

    /** Returns the hurt voice for this NPC, with a concrete recording for every gender. */
    private NPCVoice hurtVoice() {
        return switch (gender) {
            case FEMALE -> NPCVoice.HURT_FEMALE;
            case MALE -> NPCVoice.HURT_MALE;
            case NON_BINARY -> Math.random() < 0.5
                    ? NPCVoice.HURT_FEMALE : NPCVoice.HURT_MALE;
        };
    }

    /**
     * Retrieves {@code job}
     * @return {@link Job} value of job
     */
    public Job getJob() {
        return job;
    }

    /**
     * Returns this NPC's gender.
     * @return the NPC gender used for localization and voice selection
     */
    public NPCGender getGender() {
        return gender;
    }

    /**
     * Retrieves {@code npcModel}
     * @return {@link GLTFModel} value of npcModel
     */
    public GLTFModel getNpcModel() {
        return npcModel;
    }

    /** Returns this NPC's display name as the shop owner name. */
    public String getOwner() {
        return getName();
    }

    /** Returns the trader inventory, which is also the shop stock. */
    public Inventory getStock() {
        return getInventory();
    }

    /** Adds currency to this NPC's purse. */
    public void earn(int amount) {
        getPurse().add(amount);
    }

    /** Resets the trader's stock on the configured schedule. */
    public void updateShop(TimeService timeService) {
        if (job == Job.TRADER && timeService.getDay() % 10 == 0) {
            resetShop();
        }
    }

    /** Adds an item to the trader's stock. */
    public void add(Item item, int amount) {
        if (item == null || amount <= 0) return;
        getInventory().add(item, amount);
        log.info("Added x{} of {} to {}'s stock", amount, item.getName(), getOwner());
    }

    /** Processes a player selling an item to this trader. */
    public void sell(Item item, int amount) {
        if (item == null || amount <= 0) return;
        getInventory().remove(item, amount);
        earn(amount);
        log.info("Sold x{} of {} to player", amount, item.getName());
    }

    /** Processes a player buying an item from this trader. */
    public void buy(Item item, int amount) {
        if (item == null || amount <= 0) return;
        int totalPrice = item.getValue() * amount;
        if (!hasMoney() || purse() < totalPrice) {
            log.warn("Not enough money to buy x{} of {}", amount, item.getName());
            return;
        }
        getInventory().add(item, amount);
        getPurse().remove(totalPrice);
        log.info("Bought x{} of {} from player", amount, item.getName());
    }

    /** Removes all items from the trader's stock. */
    public void clear() {
        getInventory().clear();
        log.info("Cleared {}'s stock", getOwner());
    }

    /** Returns whether this trader has coins available. */
    public boolean hasMoney() {
        return purse() > 0;
    }

    /** Returns the number of non-empty stock slots. */
    public int size() {
        return getInventory().size();
    }

    /** Returns the item in a stock slot. */
    public Item get(int index) {
        return getInventory().get(index);
    }

    /** Returns the amount of an item in the stock. */
    public int getAmount(Item item) {
        return getInventory().getAmount(item);
    }

    /** Returns whether this trader's stock is empty. */
    public boolean isEmpty() {
        return getInventory().isEmpty();
    }

    /** Restores the trader's initial stock and currency state. */
    public void resetShop() {
        clear();
        earn(K.World.STARTING_COINS);
    }

    /**
     * Interacts with the NPC.
     * @param interactor the {@link Character} interacting with this NPC
     */
    public void interactWith(Character interactor) {
        this.lastInteractor = interactor;
        this.interactionTimer = INTERACTION_DURATION;
    }
}
