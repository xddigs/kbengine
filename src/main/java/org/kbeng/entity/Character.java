package org.kbeng.entity;

import org.kbeng.data.*;
import org.kbeng.item.Item;
import org.kbeng.item.Wallet;
import org.kbeng.utils.Local;
import org.kbeng.utils.ToastFactory;

/**
 * RPG-oriented extension of {@link Entity} for living actors with progression and inventory state.
 * Beyond base physics and damage handling, this class adds character attributes, level progression,
 * hunger and survival stats, faction reputation, and dual inventory containers (main inventory + backpack).
 * It is the common behavioral layer for player and AI-controlled humanoid entities.
 */
@DataClass
public abstract class Character extends
        Entity implements Levelable {
    private static final float FRAME_DURATION = 0.15f;
    private Inventory inventory;
    private Inventory backpack;
    private Reputation reputation;
    private Gamemode gamemode;
    private int level;
    private int experience;
    private int experienceForNextLevel;
    private float hunger;
    private float maxHunger;
    private int strength;
    private int intelligence;
    private int dexterity;
    private int constitution;
    private int wisdom;
    private int charisma;
    private int luck;
    private float isOffGroundTimer;
    private float animTimer = 0.0f;

    /**
     * Creates a new {@code Character} instance.
     * @param name the {@link String} supplied as {@code name}
     */
    public Character(String name) {
        this(name, false);
    }

    /**
     * Creates a new {@code Character} instance.
     * @param name the character name
     * @param includeHotbar whether its inventory includes independent hotbar slots
     */
    protected Character(String name, boolean includeHotbar) {
        super(name);
        this.inventory = new Inventory(includeHotbar, this);
        this.backpack = new Inventory(this);
        this.reputation = Reputation.NEUTRAL;

        this.level = 1;
        this.experience = 0;
        this.experienceForNextLevel = 100;

        this.strength = 8;
        this.intelligence = 8;
        this.dexterity = 8;
        this.constitution = 8;
        this.wisdom = 8;
        this.charisma = 8;
        this.luck = 50;
    }

    /** {@inheritDoc} */
    @Override
    public int getLevel() {
        return level;
    }

    /** {@inheritDoc} */
    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    /** {@inheritDoc} */
    @Override
    public int getExperience() {
        return experience;
    }

    /** {@inheritDoc} */
    @Override
    public void setExperience(int experience) {
        this.experience = experience;
    }

    /** {@inheritDoc} */
    @Override
    public int getExperienceForNextLevel() {
        return experienceForNextLevel;
    }

    /** {@inheritDoc} */
    @Override
    public void setExperienceForNextLevel(int experienceForNextLevel) {
        this.experienceForNextLevel = experienceForNextLevel;
    }

    /** {@inheritDoc} */
    @Override
    public void gain(int experience) {
        if (experience <= 0) return;
        this.experience += experience;
        while (this.experience >= experienceForNextLevel) {
            this.experience -= experienceForNextLevel;
            levelUp();

            int levelUpScaling = (int) (level * 0.8f);
            scale(levelUpScaling);
            maxHitpoints += 1;
            experienceForNextLevel = calcNextLevel();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int calcNextLevel() {
        return (int) (100 * Math.pow(1.2, level - 1));
    }

    /** {@inheritDoc} */
    @Override
    public void levelUp() {
        level++;
        ToastFactory.success(Local.lang.f("toast.level_up", level));
    }

    /** {@inheritDoc} */
    @Override
    public void damage(float amount) {
        damage(amount, Cause.ENTITY);
    }

    /** {@inheritDoc} */
    @Override
    public void damage(float amount, Cause cause) {
        if (!isAlive() || amount <= 0) return;
        if (gamemode.isGodmode() || gamemode.isNoClip()) return;
        float mitigation = Math.min(0.75f, getDefense() * 0.03f);
        super.damage(amount * (1.0f - mitigation), cause);
    }

    /** {@inheritDoc} */
    @Override
    protected void onDamageTaken(float amount) {}

    /** {@inheritDoc} */
    @Override
    public float getMaxHitpoints() {
        return maxHitpoints * level;
    }

    /** {@inheritDoc} */
    @Override
    public float getDefense() {
        return defense + (inventory == null ? 0.0f : inventory.getArmorDefense());
    }

    /** {@inheritDoc} */
    @Override
    public float getMaxDefense() {
        return maxDefense + (inventory == null ? 0.0f : inventory.getArmorDefense());
    }

    /** {@inheritDoc} */
    @Override
    public Entity setDefense(float defense) {
        this.maxDefense = defense;
        this.defense = maxDefense;
        return this;
    }

    /** Returns the inventory slot reserved for one armor category. */
    public InventorySlot getArmorSlot(ArmorSlot slot) {
        return inventory == null ? null : inventory.getArmorSlot(slot);
    }

    /**
     * Sets the {@link Character} towards a specific {@link Entity}'s direction
     */
    public void focus(Entity entity) {}

    /** Returns the wallet stored in this character's equipped backpack. */
    public Wallet hasWallet() {
        return getFromBackpack(Wallet.class);
    }

    /**
     * Returns the first item of the requested type stored in the equipped backpack.
     * Items in the normal inventory or in an unequipped backpack are deliberately ignored.
     */
    public <T extends Item> T getFromBackpack(Class<T> type) {
        if (type == null || getInventory() == null
                || !getInventory().hasBackpackEquipped() || getBackpack() == null) {
            return null;
        }
        for (InventorySlot slot : getBackpack().getSlots()) {
            if (type.isInstance(slot.getItem())) {
                return type.cast(slot.getItem());
            }
        }
        return null;
    }

    /** Returns whether this exact item instance is stored in the equipped backpack. */
    public boolean isInBackpack(Item item) {
        if (item == null || getInventory() == null
                || !getInventory().hasBackpackEquipped() || getBackpack() == null) {
            return false;
        }
        return getBackpack().getSlots().stream()
                .anyMatch(slot -> slot.getItem() == item);
    }

    /**
     * Returns the frame duration.
     * @return {@code float}; the frame duration
     */
    public float getFrameDuration() {
        return FRAME_DURATION;
    }

    /**
     * Returns the anim timer.
     * @return {@code float}; the anim timer
     */
    public float getAnimTimer() {
        return animTimer;
    }

    /**
     * Sets the anim timer.
     * @param animTimer the {@code float} supplied as {@code animTimer}
     */
    public void setAnimTimer(float animTimer) {
        this.animTimer = animTimer;
    }

    /**
     * Transforms this object according to the supplied values.
     * @param amount the {@code int} supplied as {@code amount}
     */
    public void scale(int amount) {
        strength += (int) (amount * Math.random());
        intelligence += (int) (amount * Math.random());
        dexterity += (int) (amount * Math.random());
        constitution += (int) (amount * Math.random());
        wisdom += (int) (amount * Math.random());
        charisma += (int) (amount * Math.random());
    }

    /**
     * Updates or derives runtime state for fall damage according to the supplied arguments.
     * @param amount the {@code float} supplied as {@code amount}
     */
    public void fallDamage(float amount) {
        if (!isAlive() || amount <= 0) return;
        if (gamemode.isGodmode() || gamemode.isNoClip()) return;
        damage(amount, Cause.FALL);
    }



    /**
     * Restores the supplied amount of health without exceeding the configured limit.
     * @param amount the {@code float} supplied as {@code amount}
     */
    public void heal(float amount) {
        if (amount <= 0 || !isAlive()) return;
        this.hitpoints = Math.min(getMaxHitpoints(), this.hitpoints + amount);
    }

    /**
     * Restores hunger without exceeding its configured maximum.
     * @param amount the {@code float} supplied as {@code amount}
     */
    public void restoreHunger(float amount) {
        if (amount <= 0) return;
        this.hunger = Math.min(getMaxHunger(), this.hunger + amount);
    }

    /**
     * Consumes hunger without dropping below zero.
     * @param amount the {@code float} supplied as {@code amount}
     */
    public void hungry(float amount) {
        if (amount <= 0) return;
        if (gamemode.isGodmode() || gamemode.isNoClip()) return;
        this.hunger = Math.max(0.0f, this.hunger - amount);
    }

    /**
     * Returns the last character that interacted with this character
     * @return the {@link Character} representing the last character that interacted with this character
     */
    protected Character hasBeenInteractedWith() {
        return this;
    }

    /**
     * Updates the current state.
     */
    public void update() {
        if (!isAlive()) return;
    }

    /**
     * Returns the inventory.
     * @return the {@link Inventory} representing the inventory
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Sets the inventory.
     * @param inventory the {@link Inventory} supplied as {@code inventory}
     */
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Returns the backpack.
     * @return the {@link Inventory} representing the backpack
     */
    public Inventory getBackpack() {
        return backpack;
    }

    /**
     * Sets the backpack.
     * @param backpack the {@link Inventory} supplied as {@code backpack}
     */
    public void setBackpack(Inventory backpack) {
        this.backpack = backpack;
    }

    /**
     * Returns the hitpoints.
     * @return {@code float}; the hitpoints
     */
    public float getHitpoints() {
        return hitpoints;
    }

    /**
     * Sets the hitpoints.
     * @param hitpoints the {@code float} supplied as {@code hitpoints}
     */
    public void setHitpoints(float hitpoints) {
        this.hitpoints = hitpoints;
    }

    /**
     * Sets the max hitpoints.
     * @param maxHitpoints the {@code int} supplied as {@code maxHitpoints}
     */
    public void setMaxHitpoints(int maxHitpoints) {
        this.maxHitpoints = maxHitpoints;
    }

    /**
     * Returns the hunger.
     * @return {@code float}; the hunger
     */
    public float getHunger() {
        return hunger;
    }

    /**
     * Sets the hunger.
     * @param hunger the {@code float} supplied as {@code hunger}
     */
    public void setHunger(float hunger) {
        this.hunger = hunger;
    }

    /**
     * Returns the max hunger.
     * @return {@code float}; the max hunger
     */
    public float getMaxHunger() {
        return maxHunger;
    }

    /**
     * Sets the max hunger.
     * @param maxHunger the {@code float} supplied as {@code maxHunger}
     */
    public void setMaxHunger(float maxHunger) {
        this.maxHunger = maxHunger;
    }

    /**
     * Returns the strength.
     * @return {@code int}; the strength
     */
    public int getStrength() {
        return strength;
    }

    /**
     * Sets the strength.
     * @param strength the {@code int} supplied as {@code strength}
     */
    public void setStrength(int strength) {
        this.strength = strength;
    }

    /**
     * Returns the intelligence.
     * @return {@code int}; the intelligence
     */
    public int getIntelligence() {
        return intelligence;
    }

    /**
     * Sets the intelligence.
     * @param intelligence the {@code int} supplied as {@code intelligence}
     */
    public void setIntelligence(int intelligence) {
        this.intelligence = intelligence;
    }

    /**
     * Returns the dexterity.
     * @return {@code int}; the dexterity
     */
    public int getDexterity() {
        return dexterity;
    }

    /**
     * Sets the dexterity.
     * @param dexterity the {@code int} supplied as {@code dexterity}
     */
    public void setDexterity(int dexterity) {
        this.dexterity = dexterity;
    }

    /**
     * Returns the constitution.
     * @return {@code int}; the constitution
     */
    public int getConstitution() {
        return constitution;
    }

    /**
     * Sets the constitution.
     * @param constitution the {@code int} supplied as {@code constitution}
     */
    public void setConstitution(int constitution) {
        this.constitution = constitution;
    }

    /**
     * Returns the wisdom.
     * @return {@code int}; the wisdom
     */
    public int getWisdom() {
        return wisdom;
    }

    /**
     * Sets the wisdom.
     * @param wisdom the {@code int} supplied as {@code wisdom}
     */
    public void setWisdom(int wisdom) {
        this.wisdom = wisdom;
    }

    /**
     * Returns the charisma.
     * @return {@code int}; the charisma
     */
    public int getCharisma() {
        return charisma;
    }

    /**
     * Sets the charisma.
     * @param charisma the {@code int} supplied as {@code charisma}
     */
    public void setCharisma(int charisma) {
        this.charisma = charisma;
    }

    /**
     * Returns the luck.
     * @return {@code int}; the luck
     */
    public int getLuck() {
        return luck;
    }

    /**
     * Sets the luck.
     * @param luck the {@code int} supplied as {@code luck}
     */
    public void setLuck(int luck) {
        this.luck = luck;
    }

    /**
     * Returns the reputation.
     * @return the {@link Reputation} representing the reputation
     */
    public Reputation getReputation() {
        return reputation;
    }

    /**
     * Sets the reputation.
     * @param reputation the {@link Reputation} supplied as {@code reputation}
     */
    public void setReputation(Reputation reputation) {
        this.reputation = reputation;
    }

    /**
     * Returns the gamemode.
     * @return the {@link Gamemode} representing the gamemode
     */
    public Gamemode getGamemode() {
        return gamemode;
    }

    /**
     * Sets the gamemode.
     * @param gamemode the {@link Gamemode} supplied as {@code gamemode}
     */
    public void setGamemode(Gamemode gamemode) {
        this.gamemode = gamemode;
    }

    /**
     * Returns whether the character is in godmode
     * @return {@code boolean}; whether the character is in godmode
     */
    public boolean isInGodMode() {
        return gamemode.isGodmode();
    }

    /**
     * Returns whether the character is alive and in survival mode.
     * @return {@code true} if alive and in survival mode; otherwise {@code false}
     */
    public boolean isInSurvival() {
        return gamemode.isSurvival();
    }

    /**
     * Checks whether the no clip condition is met.
     * @return {@code true} if no clip; otherwise {@code false}
     */
    public boolean isNoClip() {
        return gamemode.isNoClip();
    }

    /**
     * Returns the is off ground timer.
     * @return {@code float}; the is off ground timer
     */
    public float getIsOffGroundTimer() {
        return isOffGroundTimer;
    }

    /**
     * Sets the is off ground timer.
     * @param isOffGroundTimer the {@code float} supplied as {@code isOffGroundTimer}
     */
    public void setIsOffGroundTimer(float isOffGroundTimer) {
        this.isOffGroundTimer = isOffGroundTimer;
    }
}
