package org.kbeng.entity;

import org.kbeng.data.*;
import org.kbeng.graphics.gltf.GLTFLoader;
import org.kbeng.graphics.gltf.GLTFModel;
import org.kbeng.item.Item;
import org.kbeng.item.Armor;
import org.kbeng.item.Axe;
import org.kbeng.item.Backpack;
import org.kbeng.item.Boots;
import org.kbeng.item.Chestplate;
import org.kbeng.item.Helmet;
import org.kbeng.item.Hoe;
import org.kbeng.item.Material;
import org.kbeng.item.MiningComponent;
import org.kbeng.item.Pickaxe;
import org.kbeng.item.Shield;
import org.kbeng.item.Shovel;
import org.kbeng.item.Sword;
import org.kbeng.item.Tool;
import org.kbeng.item.Wallet;
import org.kbeng.service.SoundService;
import org.kbeng.service.TimeService;
import org.kbeng.utils.K;
import org.kbeng.utils.Naming;
import org.kbeng.wrld.GameMaster;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a non-player character (NPC) driven by artificial intelligence.
 *
 * <p>Key Responsibilities:</p>
 * <ul>
 *   <li>Executes AI behavior logic, including field-of-view target detection, threat evaluation,
 *       and autonomous pathfinding/wandering within specified bounds.</li>
 *   <li>Manages interactive dialogue state trees, branching narrative triggers, and quest assignment.</li>
 *   <li>Handles merchant trade mechanics, inventory exchange, and currency transactions
 *       with the player.</li>
 * </ul>
 */
@DataClass
public class NPC extends Character {
    private static final Logger log = LoggerFactory.getLogger(NPC.class);
    private static final float WALK_SPEED = 1.5f;
    private static final float MIN_IDLE_TIME = 2.0f;
    private static final float IDLE_TIME_VARIATION = 4.0f;
    private static final float WANDER_RADIUS = 5.0f;
    private static final float KNOCKBACK_DAMPING = 12.0f;
    private static final float DEFAULT_KNOCKBACK_TIMER = 0.25f;
    private static final float HEIGHT = 1.0f;
    private static final float WIDTH = 0.25f;
    private static final int MAX_HITPOINTS = 20;
    private static final int MAX_HUNGER = 20;
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
        setDimensions(WIDTH, HEIGHT, WIDTH);
        setMaxHitpoints(MAX_HITPOINTS);
        setHitpoints(MAX_HITPOINTS);
        setMaxHunger(MAX_HUNGER);
        setHunger(MAX_HUNGER);
        setReputation(Reputation.FRIENDLY);
        setGamemode(Gamemode.SURVIVAL);
        setSpeed(WALK_SPEED);
        chooseIdleDuration();

        if (job == Job.TRADER) {
            getInventory().equipBackpack(new Backpack(), false);
            getBackpack().add(new Wallet(), 1);
            resetShop();
        }
    }

    /**
     * Creates a farmer NPC with the supplied gender.
     * @param gender the voice and localized gender of the character
     */
    public NPC(NPCGender gender) {
        this(gender, Job.TRADER);
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

    /** Returns the disapproving voice for this NPC, with a concrete recording for every gender. */
    private NPCVoice disapprovingVoice() {
        return switch (gender) {
            case FEMALE -> NPCVoice.DISAPPROVING_FEMALE;
            case MALE -> NPCVoice.DISAPPROVING_MALE;
            case NON_BINARY -> Math.random() < 0.5
                    ? NPCVoice.DISAPPROVING_FEMALE : NPCVoice.DISAPPROVING_MALE;
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

    /** Adds currency to this NPC's wallet. */
    public void earn(int amount) {
        hasWallet().earn(amount);
    }

    /**
     * Dynamically generates a random trader stock, with random amounts and types
     */
    public void setUpStock() {
        if (job != Job.TRADER) return;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        MaterialID.forEach(material -> {
            if (material.equals(MaterialID.INGOT) || material.equals(MaterialID.RAW_ORE)) return;
            if (random.nextFloat() < 0.70f) {
                int amount = random.nextInt(5, 33);
                add(new Material(material), amount);
            }
        });

        Tier.forEach(tier -> {
            if (tier.isInvalidTier()) return;
            if (random.nextBoolean()) {
                int amount = random.nextInt(4, 21);
                add(new MiningComponent(tier, MaterialID.RAW_ORE), amount);
            }
        });

        ToolType.forEach(toolType -> {
            if (random.nextBoolean()) {
                add(createTool(toolType, randomToolTier(random)), 1);
            }
        });

        ArmorData.forEach(armorData -> {
            if (random.nextBoolean()) {
                add(createArmor(armorData, randomArmorTier(random)), 1);
            }
        });

        add(new Material(MaterialID.CHARCOAL), random.nextInt(16, 65));
    }

    /** Selects one tier that is registered for tradeable weapons and tools. */
    private static Tier randomToolTier(ThreadLocalRandom random) {
        Tier[] tiers = Tier.values();
        Tier tier;
        do {
            tier = tiers[random.nextInt(tiers.length)];
        } while (tier == Tier.NONE || tier == Tier.LEATHER);
        return tier;
    }

    /** Selects one tier that is registered for tradeable armor. */
    private static Tier randomArmorTier(ThreadLocalRandom random) {
        Tier[] tiers = Tier.values();
        Tier tier;
        do {
            tier = tiers[random.nextInt(tiers.length)];
        } while (tier == Tier.NONE || tier == Tier.WOODEN);
        return tier;
    }

    /** Creates the concrete stock item for a tool or weapon type. */
    private static Tool createTool(ToolType type, Tier tier) {
        return switch (type) {
            case SWORD -> new Sword(tier);
            case PICKAXE -> new Pickaxe(tier);
            case AXE -> new Axe(tier);
            case HOE -> new Hoe(tier);
            case SHOVEL -> new Shovel(tier);
            case SHIELD -> new Shield(tier);
        };
    }

    /** Creates the concrete stock item for an armor slot type. */
    private static Armor createArmor(ArmorData type, Tier tier) {
        return switch (type) {
            case HELMET -> new Helmet(tier);
            case CHESTPLATE -> new Chestplate(tier);
            case BOOTS -> new Boots(tier);
        };
    }

    /** Resets the trader's stock on the configured schedule. */
    public void updateShop(TimeService timeService) {
        if (job == Job.TRADER && timeService.getDay() % 3 == 0) {
            resetShop();
        }
    }

    /** Adds an item to the trader's stock. */
    public void add(Item item, int amount) {
        if (item == null || amount <= 0) return;
        getInventory().add(item, amount);
        log.trace("Added x{} of {} to {}'s stock", amount, item.getName(), getOwner());
    }

    /** Processes this trader selling an item to the player. */
    public boolean sell(Item item, int amount) {
        if (item == null || amount <= 0
                || getInventory().getAmount(item) < amount) return false;
        getInventory().remove(item, amount);
        earn(item.getValue() * amount);

        if (item.getValue() < 50) {
            SoundService.fx.playNPCVoice(disapprovingVoice());
        } else {
            SoundService.fx.playNPCVoice(normalVoice());
        }

        log.info("Sold x{} of {} to player", amount, item.getName());
        return true;
    }

    /**
     * Processes this trader buying an item from the player.
     * @return {@link Boolean} true if successful, false if not enough money or stock space
     */
    public boolean buy(Item item, int amount) {
        if (item == null || amount <= 0) return false;
        int totalPrice = item.getValue() * amount;

        if (hasWallet().coins() < totalPrice) {
            log.warn("Not enough money to buy x{} of {}", amount, item.getName());
            return false;
        }

        if (!canStore(item, amount)) {
            log.warn("Not enough stock space to buy x{} of {}", amount, item.getName());
            return false;
        }
        getInventory().add(item, amount);
        hasWallet().spend(totalPrice);

        if (item.getValue() > 100) {
            SoundService.fx.playNPCVoice(disapprovingVoice());
        } else {
            SoundService.fx.playNPCVoice(normalVoice());
        }
        log.info("Bought x{} of {} from player", amount, item.getName());
        return true;
    }

    /** Checks stock capacity before a purchase is committed. */
    private boolean canStore(Item item, int amount) {
        int remaining = amount;
        int maxStack = getInventory().getMaxStack(item);
        for (InventorySlot slot : getInventory().getSlots()) {
            if (!slot.isEmpty() && sameStackType(slot.getItem(), item)) {
                remaining -= Math.max(0, maxStack - slot.getAmount());
                if (remaining <= 0) return true;
            }
        }
        for (InventorySlot slot : getInventory().getSlots()) {
            if (slot.isEmpty()) {
                remaining -= maxStack;
                if (remaining <= 0) return true;
            }
        }
        return false;
    }

    /**
     * Checks if two items are of the same type.
     * @param first 1st item to compare, or {@code null}
     * @param second 2nd item to compare or {@code null}
     * @return {@link Boolean} true if the items are of the same type, false otherwise
     */
    private boolean sameStackType(Item first, Item second) {
        return first != null && second != null
                && first.getClass() == second.getClass()
                && first.getId() == second.getId();
    }

    /** Removes all items from the trader's stock. */
    public void clear() {
        getInventory().clear();
        log.info("Cleared {}'s stock", getOwner());
    }

    /** Returns whether this trader has coins available. */
    public boolean hasMoney() {
        return hasWallet().coins() > 0;
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
        hasWallet().empty();
        setUpStock();
        long stockValue = getInventory().getItems().entrySet().stream()
                .mapToLong(entry -> (long) entry.getKey().getValue() * entry.getValue())
                .sum();
        earn(Math.clamp(stockValue, K.World.STARTING_COINS, Integer.MAX_VALUE));
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
