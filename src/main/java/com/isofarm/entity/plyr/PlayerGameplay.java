package com.isofarm.entity.plyr;

import com.isofarm.data.Gamemode;
import com.isofarm.data.Cause;
import com.isofarm.data.Difficulty;
import com.isofarm.data.InventorySlot;
import com.isofarm.data.Reputation;
import com.isofarm.data.Seed;
import com.isofarm.data.SoundGroup;
import com.isofarm.data.StartingKit;
import com.isofarm.entity.Player;
import com.isofarm.entity.WorldItem;
import com.isofarm.item.*;
import com.isofarm.pathfinding.GridPos;
import com.isofarm.service.SoundService;
import com.isofarm.utils.Local;
import com.isofarm.utils.Settings;
import com.isofarm.utils.ToastFactory;
import com.isofarm.wrld.GameMaster;
import com.isofarm.wrld.World;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Owns player rules concerning life, attributes, inventory, loot and currency.
 */
public final class PlayerGameplay {
    private static final Logger log = LoggerFactory.getLogger(PlayerGameplay.class);
    private static final float WIDTH = 0.5f, HEIGHT = 2.0f, SPAWN_X = 0.5f, SPAWN_Z = 0.5f;
    private static final float SPEED = 6.0f, RESPAWN_DELAY = 5.0f;
    private static final int MAX_HITPOINTS = 20, MAX_HUNGER = 20;
    private Player player;
    private int damageSequence;
    private float respawnTimer = -1.0f;

    /**
     * Creates the shared player's gameplay component.
     */
    public PlayerGameplay() {}

    /**
     * Initializes the player from the shared world.
     */
    public void initialize() {
        player = Player.plyr;
        World world = World.wrld;
        GridPos altitude = world.getHighestY(SPAWN_X, SPAWN_Z);
        player.setPosition(new Vector3f(SPAWN_X, altitude.y(), SPAWN_Z));
        player.setVelocity(new Vector3f());
        player.setDimensions(new Vector3f(WIDTH, HEIGHT, WIDTH));
        player.setMaxHitpoints(MAX_HITPOINTS); player.setHitpoints(MAX_HITPOINTS);
        player.setMaxHunger(MAX_HUNGER); player.setHunger(MAX_HUNGER);
        player.setSpeed(SPEED); player.setReputation(Reputation.NEUTRAL);
        player.setGamemode(Gamemode.SURVIVAL);
        setUpInventory();
    }

    /**
     * Advances death and respawn handling.
     * @param delta the {@code float} argument; frame time
     * @return {@code true} if normal player updates should continue; otherwise {@code false}
     */
    public boolean updateLifeCycle(float delta) {
        if (player.isAlive()) return true;
        if (respawnTimer <= 0.0f) {
            respawnTimer = RESPAWN_DELAY;
            dropLoot();
            GameMaster.game.toggleHUD();
            player.setGamemode(Gamemode.NO_CLIP);
        }
        respawnTimer -= delta;
        if (respawnTimer <= 0.0f) respawn();
        return false;
    }

    /**
     * Updates this object for the current simulation step.
     * @param delta the {@code float} argument; frame time
     */
    public void update(float delta) {
        player.setAnimTimer(player.getAnimTimer() + delta);
        player.heal(((0.5f + player.getLevel()) * delta) / getDifficultyRegen());
        updateHunger(delta);
        checkDurability();
    }

    private void updateHunger(float delta) {
        if (!player.isInSurvival()) return;

        Difficulty difficulty = GameMaster.game.getDifficulty();
        player.hungry(player.getLevel() * delta * difficulty.getMultiplier());
        if (difficulty == Difficulty.NIGHTMARE && player.getHunger() <= 0.0f) {
            player.kill(Cause.STARVATION);
        }
    }

    /** Kills the survival player as soon as their body enters generated ocean. */
    public boolean checkOceanDrowning() {
        if (player.getGamemode() != Gamemode.SURVIVAL) return false;
        Vector3f position = player.getPosition();
        Vector3f size = player.getDimensions();
        int minX = (int) Math.floor(position.x - size.x * 0.5f + 0.001f);
        int maxX = (int) Math.floor(position.x + size.x * 0.5f - 0.001f);
        int minY = (int) Math.floor(position.y + 0.001f);
        int maxY = (int) Math.floor(position.y + size.y - 0.001f);
        int minZ = (int) Math.floor(position.z - size.z * 0.5f + 0.001f);
        int maxZ = (int) Math.floor(position.z + size.z * 0.5f - 0.001f);

        World world = World.wrld;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!world.isGeneratedOceanWaterAt(x, y, z)) continue;
                    float waterTop = y + world.getFluidLevelAt(x, y, z) / 8.0f;
                    if (position.y < waterTop) {
                        player.kill(Cause.DROWN);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Handles damage taken and updates the affected state.
     * @param amount the {@code float} argument; received damage
     */
    public void onDamageTaken(float amount) {
        damageSequence++;
        SoundService.fx.playEntitySound(SoundGroup.ENTITY);
    }

    /**
     * Drops all droppable inventory stacks into the world.
     */
    public void dropLoot() {
        for (Item item : List.copyOf(player.getInventory().getItems().keySet())) {
            if (item == null || item instanceof Undroppable) continue;
            int amount = player.getInventory().getAmount(item);
            if (amount <= 0) continue;
            GameMaster.game.addEntity(new WorldItem(item, amount, new Vector3f(player.getPosition())));
            remove(item, amount);
        }
    }

    /**
     * Restores spawn position, core stats and attributes.
     */
    public void respawn() {
        if (!Settings.doKeepInventory()) clear();
        GridPos altitude = GameMaster.game.getWorld().getHighestY(SPAWN_X, SPAWN_Z);
        player.setPosition(new Vector3f(SPAWN_X, altitude.y() + 1.0f, SPAWN_Z));
        player.setVelocity(new Vector3f()); player.setDimensions(new Vector3f(WIDTH, HEIGHT, WIDTH));
        player.setSpeed(SPEED); player.setReputation(Reputation.NEUTRAL); player.setGamemode(Gamemode.SURVIVAL);
        player.setIsOffGroundTimer(0.0f); player.setWasOnGround(false);
        player.setMaxHitpoints(MAX_HITPOINTS); player.setHitpoints(MAX_HITPOINTS);
        player.setMaxHunger(MAX_HUNGER); player.setHunger(MAX_HUNGER);
        player.setExperience(0); player.setLevel(1); resetAttributes();
        respawnTimer = -1.0f;
        GameMaster.game.toggleHUD();
    }

    /**
     * Resets all six character attributes to their base value.
     */
    public void resetAttributes() {
        player.setStrength(1); player.setDexterity(1); player.setConstitution(1);
        player.setIntelligence(1); player.setWisdom(1); player.setCharisma(1);
    }

    private void setUpInventory() {
        if (player.getGamemode() != Gamemode.SURVIVAL) return;

        Backpack starterBackpack = null;
        for (Item item : new StartingKit().getItems()) {
            add(item);
            if (item instanceof Backpack backpack) starterBackpack = backpack;
        }
        if (starterBackpack == null) return;

        player.getBackpack().add(new CraftingBook(), 1);
        player.getBackpack().add(new Wallet(), 1);
    }

    private void checkDurability() {
        for (InventorySlot slot : player.getInventory().getSlots()) {
            if (slot.getItem() instanceof Tool tool) {
                if (tool.getDurability() == tool.getDurability() % 25) {
                    ToastFactory.warning(Local.lang.f("toast.item_warning", tool.getDisplayName()));
                    return;
                }
                if (tool.getDurability() <= 0) {
                    remove(tool);
                    SoundService.fx.playBreakSound(SoundGroup.ITEMS);
                }
            }
        }
    }

    /**
     * Processes sell and updates the affected inventory or currency balances.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} argument; quantity
     */
    public void sell(Item item, int amount) {
        if (item == null || amount <= 0) return;
        int current = player.getInventory().getAmount(item);
        if (current <= 0) { log.warn("No {} in inventory to sell", item.getName()); return; }
        int sold = Math.min(current, amount);
        player.getInventory().remove(item, sold);
        int earnings = sold * item.getValue();
        ToastFactory.sell(Local.lang.f("toast.item_sold", amount, item.getDisplayName(), earnings));
        earn(earnings);
    }

    /**
     * Adds the supplied element to the corresponding collection or processing queue.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} argument; quantity
     */
    public void add(Item item, int amount) { player.getInventory().add(item, amount); log.trace("Added x{} of {} to inventory", amount, item.getName()); }
    /**
     * Adds the supplied element to the corresponding collection or processing queue.
     * @param item the {@link Item} supplied as {@code item}
     */
    public void add(Item item) { add(item, 1); log.trace("Added x1 of {} to inventory", item.getName()); }
    /**
     * Adds to backpack to the corresponding collection or processing queue.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} argument; quantity
     */
    public void addToBackpack(Item item, int amount) { if (player.getBackpack().hasBackpackEquipped()) player.getBackpack().add(item, amount); log.info("Added x{} of {} to backpack", amount, item.getName()); }
    /**
     * Adds to backpack to the corresponding collection or processing queue.
     * @param item the {@link Item} supplied as {@code item}
     */
    public void addToBackpack(Item item) { addToBackpack(item, 1); log.trace("Added x1 of {} to backpack", item.getName()); }
    /**
     * Removes from backpack and updates any dependent state.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} argument; quantity
     */
    public void removeFromBackpack(Item item, int amount) { if (player.getBackpack().hasBackpackEquipped()) player.getBackpack().remove(item, amount); log.info("Removed x{} of {} from backpack", amount, item.getName()); }
    /**
     * Removes from backpack and updates any dependent state.
     * @param item the {@link Item} supplied as {@code item}
     */
    public void removeFromBackpack(Item item) { removeFromBackpack(item, 1); log.info("Removed x1 of {} from backpack", item.getName()); }
    /**
     * Sorts both storage containers.
     */
    public void sort() { player.getInventory().sort(); player.getBackpack().sort(); }
    /**
     * Removes the supplied element and updates any dependent state.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} argument; quantity
     */
    public void remove(Item item, int amount) { if (!player.getGamemode().isGodmode()) { player.getInventory().remove(item, amount); log.info("Removed x{} of {} to inventory", amount, item.getName()); } }
    /**
     * Removes the supplied element and updates any dependent state.
     * @param item the {@link Item} supplied as {@code item}
     */
    public void remove(Item item) { if (!player.getGamemode().isGodmode()) { player.getInventory().remove(item, 1); log.info("Removed x1 of {} from inventory", item.getName()); } }
    /**
     * Clears non-backpack inventory items.
     */
    public void clear() { for (Item item :List.copyOf(player.getInventory().getItems().keySet())) if (item != null && !(item instanceof CraftingBook)) remove(item); log.info("Cleared inventory"); }
    /**
     * Determines whether this object contains no elements or active content.
     * @return {@code true} if inventory is empty; otherwise {@code false}
     */
    public boolean isEmpty() { return player.getInventory().isEmpty(); }
    /**
     * Returns the number or extent represented by size.
     * @return {@code int}; inventory size
     */
    public int size() { return player.getInventory().size(); }
    /**
     * Returns the value identified by the supplied key, index, or current object state.
     * @param index the {@code int} supplied as {@code index}
     * @return the {@link Item} result; indexed item
     */
    public Item get(int index) { return player.getInventory().get(index); }
    /**
     * Returns the value identified by the supplied key, index, or current object state.
     * @param item the {@link Item} argument; key
     * @return the {@link Item} result; matching item
     */
    public Item get(Item item) { return player.getInventory().get(item); }
    /**
     * Returns amount according to the current object state.
     * @param item the {@link Item} supplied as {@code item}
     * @return {@code int}; quantity
     */
    public int getAmount(Item item) { return player.getInventory().getAmount(item); }
    /**
     * Processes earn and updates the affected inventory or currency balances.
     * @param amount the {@code int} argument; currency amount
     */
    public void earn(int amount) {
        if (amount <= 0 || player.hasWallet() == null) return;
        log.info("Earned ${}", amount);
        player.hasWallet().earn(amount);
    }
    /**
     * Processes spend and updates the affected inventory or currency balances.
     * @param amount the {@code int} argument; currency amount
     */
    public void spend(int amount) {
        if (amount <= 0 || player.hasWallet() == null
                || player.hasWallet().coins() < amount) return;
        log.info("Spent ${}", amount);
        player.hasWallet().spend(amount);
    }
    /**
     * Determines whether space is satisfied by the current state.
     * @return {@code true} if storage has space; otherwise {@code false}
     */
    public boolean hasSpace() { return !player.getInventory().isFull() || player.getBackpack().hasBackpackEquipped() && !player.getBackpack().isFull(); }
    /**
     * Determines whether seeds is satisfied by the current state.
     * @return {@code true} if seeds are present; otherwise {@code false}
     */
    public boolean hasSeeds() { return player.getInventory().hasItemOfType(Seed.class); }
    /**
     * Returns damage sequence according to the current object state.
     * @return {@code int}; damage event sequence
     */
    public int getDamageSequence() { return damageSequence; }
    /**
     * Returns respawn timer according to the current object state.
     * @return {@code float}; respawn timer
     */
    public float getRespawnTimer() { return respawnTimer; }
    /**
     * Sets respawn timer and updates the associated state.
     * @param value the {@code float} argument; respawn timer
     */
    public void setRespawnTimer(float value) { respawnTimer = value; }
    /**
     * Returns difficulty regen according to the current object state.
     * @return {@code float}; regeneration multiplier
     */
    public float getDifficultyRegen() { return GameMaster.game.getDifficulty().getMultiplier(); }
}
