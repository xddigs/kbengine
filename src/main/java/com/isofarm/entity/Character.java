package com.isofarm.entity;

import com.isofarm.data.*;
import com.isofarm.utils.ToastFactory;

/**
 * Encapsulates the state and operations required by character within the game runtime.
 */
@DataClass
public abstract class Character extends Entity implements Levelable {
    private static final float FRAME_DURATION = 0.15f;
    private final Purse purse;
    private Inventory inventory;
    private Inventory backpack;
    private Reputation reputation;
    private Gamemode gamemode;
    private int level;
    private int experience;
    private int experienceForNextLevel;
    private float stamina;
    private float maxStamina;
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
        this.purse = new Purse();
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

    /**
     * {@inheritDoc}
     * Returns the level.
     * @return {@code int}; the level
     */
    @Override
    public int getLevel() {
        return level;
    }

    /**
     * {@inheritDoc}
     * Sets the level.
     * @param level the {@code int} supplied as {@code level}
     */
    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * {@inheritDoc}
     * Returns the experience.
     * @return {@code int}; the experience
     */
    @Override
    public int getExperience() {
        return experience;
    }

    /**
     * {@inheritDoc}
     * Sets the experience.
     * @param experience the {@code int} supplied as {@code experience}
     */
    @Override
    public void setExperience(int experience) {
        this.experience = experience;
    }

    /**
     * {@inheritDoc}
     * Returns the experience for next level.
     * @return {@code int}; the experience for next level
     */
    @Override
    public int getExperienceForNextLevel() {
        return experienceForNextLevel;
    }

    /**
     * {@inheritDoc}
     * Sets the experience for next level.
     * @param experienceForNextLevel the {@code int} supplied as {@code experienceForNextLevel}
     */
    @Override
    public void setExperienceForNextLevel(int experienceForNextLevel) {
        this.experienceForNextLevel = experienceForNextLevel;
    }

    /**
     * {@inheritDoc}
     * Adds the supplied amount to accumulated progression and applies any resulting transitions.
     * @param experience the {@code int} supplied as {@code experience}
     */
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
            maxStamina += 1;
            experienceForNextLevel = calcNextLevel();
        }
    }

    /**
     * {@inheritDoc}
     * Calculates next level from the current inputs.
     * @return {@code int}; the calc next level result
     */
    @Override
    public int calcNextLevel() {
        return (int) (100 * Math.pow(1.2, level - 1));
    }

    /**
     * {@inheritDoc}
     * Advances this object to the next progression level and updates dependent statistics.
     */
    @Override
    public void levelUp() {
        level++;
        ToastFactory.success("Level up! You're now level " + level);
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
     * {@inheritDoc}
     * Applies the supplied damage amount and triggers the associated health-state changes.
     * @param amount the {@code float} supplied as {@code amount}
     */
    @Override
    public void damage(float amount) {
        damage(amount, Cause.ENTITY);
    }

    /**
     * {@inheritDoc}
     * Applies damage attributed to a specific cause.
     * @param amount the {@code float} argument; the damage amount
     * @param cause the {@link Cause} argument; the damage cause
     */
    @Override
    public void damage(float amount, Cause cause) {
        if (!isAlive() || amount <= 0) return;
        if (gamemode.isGodmode() || gamemode.isNoClip()) return;
        super.damage(amount, cause);
    }

    /**
     * {@inheritDoc}
     * Handles damage taken and updates the affected state.
     * @param amount the {@code float} supplied as {@code amount}
     */
    @Override
    protected void onDamageTaken(float amount) {
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
     * Applies restore stamina and updates the affected character or item state.
     * @param amount the {@code float} supplied as {@code amount}
     */
    public void restoreStamina(float amount) {
        if (amount <= 0) return;
        this.stamina = Math.min(getMaxStamina(), this.stamina + amount);
    }

    /**
     * Applies consume stamina and updates the affected character or item state.
     * @param amount the {@code float} supplied as {@code amount}
     */
    public void consumeStamina(float amount) {
        if (amount <= 0) return;
        if (gamemode.isGodmode() || gamemode.isNoClip()) return;
        this.stamina = Math.max(0.0f, this.stamina - amount);
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
     * Returns the purse.
     * @return the {@link Purse} representing the purse
     */
    public Purse getPurse() {
        return purse;
    }

    /**
     * Returns the purse associated with this character.
     * @return {@code int}; the purse result
     */
    public int purse() {
        return purse.getBalance();
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
     * {@inheritDoc}
     * Returns the max hitpoints.
     * @return {@code float}; the max hitpoints
     */
    @Override
    public float getMaxHitpoints() {
        return maxHitpoints * level;
    }

    /**
     * Sets the max hitpoints.
     * @param maxHitpoints the {@code int} supplied as {@code maxHitpoints}
     */
    public void setMaxHitpoints(int maxHitpoints) {
        this.maxHitpoints = maxHitpoints;
    }

    /**
     * Returns the stamina.
     * @return {@code float}; the stamina
     */
    public float getStamina() {
        return stamina;
    }

    /**
     * Sets the stamina.
     * @param stamina the {@code float} supplied as {@code stamina}
     */
    public void setStamina(float stamina) {
        this.stamina = stamina;
    }

    /**
     * Returns the max stamina.
     * @return {@code float}; the max stamina
     */
    public float getMaxStamina() {
        return maxStamina * level;
    }

    /**
     * Sets the max stamina.
     * @param maxStamina the {@code float} supplied as {@code maxStamina}
     */
    public void setMaxStamina(float maxStamina) {
        this.maxStamina = maxStamina;
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
