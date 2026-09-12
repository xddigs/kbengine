package org.kbeng.rpg.item;

import org.kbeng.rpg.data.DataClass;
import org.kbeng.rpg.data.Enchantment;
import org.kbeng.rpg.data.Tier;
import org.kbeng.rpg.data.ToolType;
import org.kbeng.rpg.entity.Player;
import org.kbeng.engine.utils.Local;

/**
 * Tool provides tool capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It implements Craftable, providing a concrete strategy for this subsystem contract.
 */
@DataClass
public abstract class Tool implements Craftable,
        Enchantable {
    private final byte id;
    private final String name;
    private final int value;

    private final ToolType type;
    private Tier tier;
    private int durability;
    private final Enchantment[] enchantments;
    private final float baseDamage;

    /**
     * Creates a new {@code Tool} instance.
     * @param id the {@code byte} supplied as {@code id}
     * @param name the {@link String} supplied as {@code name}
     * @param value the {@code int} supplied as {@code value}
     * @param type the {@link ToolType} supplied as {@code type}
     * @param tier the {@link Tier} supplied as {@code tier}
     * @param durability the {@code int} supplied as {@code durability}
     */
    public Tool(byte id, String name, int value,
                ToolType type, Tier tier, int durability) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.type = type;
        this.tier = tier;
        this.durability = durability;
        this.baseDamage = type.getBaseDamage();
        this.enchantments = new Enchantment[4];
    }

    /**
     * {@inheritDoc}
     * Returns the id.
     * @return {@code byte}; the id
     */
    @Override
    public byte getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     * Returns the name.
     * @return the {@link String} representing the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    @Override
    public String getDisplayName() {
        return Local.lang.item(type.getDisplayName(),
                tier == Tier.NONE ? null : tier.getDisplayName()
        );
    }

    /** {@inheritDoc} */
    @Override
    public boolean enchanting(Enchantment enchantment) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Item copy() {
        return this;
    }

    /**
     * {@inheritDoc}
     * Returns the value.
     * @return {@code int}; the value
     */
    @Override
    public int getValue() {
        return value;
    }

    /**
     * Returns the col.
     * @return {@code int}; the col
     */
    public int getCol() {
        return getId();
    }

    /**
     * Returns the row.
     * @return {@code int}; the row
     */
    public int getRow() {
        return tier.getId();
    }

    /**
     * Returns the base damage.
     * @return {@code float}; the base damage
     */
    public float getBaseDamage() {
        return baseDamage;
    }

    /**
     * Returns the type.
     * @return the {@link ToolType} representing the type
     */
    public ToolType getType() {
        return type;
    }

    /**
     * Returns the tier.
     * @return the {@link Tier} representing the tier
     */
    public Tier getTier() {
        return tier;
    }

    /**
     * Applies upgrade and updates the affected character or item state.
     * @param tier the {@link Tier} supplied as {@code tier}
     */
    public void upgrade(Tier tier) {
        Tier[] tiers = Tier.values();
        int index = tier.ordinal();
        if (index < tiers.length - 1) {
            this.tier = tiers[index + 1];
        }
    }

    /**
     * Returns the durability.
     * @return {@code int}; the durability
     */
    public int getDurability() {
        return durability;
    }

    /**
     * Returns the max durability.
     * @return {@code float}; the max durability
     */
    public float getMaxDurability() {
        return tier.getDurability() + type.getBaseDurability();
    }

    /**
     * Sets the durability.
     * @param durability the {@code int} supplied as {@code durability}
     */
    public void setDurability(int durability) {
        this.durability = durability;
    }

    /**
     * Handles use and applies its effect to the current interaction state.
     */
    public void use() {
        if (Player.plyr.getGamemode().isGodmode()) return;
        if (canBeUsed()) {
            durability--;
        }
    }

    /**
     * Publishes the notification represented by misuse.
     */
    public void misuse() {
        if (Player.plyr.getGamemode().isGodmode()) return;
        if (canBeUsed()) {
            durability -= 2;
        }
    }

    /**
     * Applies repair and updates the affected character or item state.
     */
    public void repair() {
        if (Player.plyr.getGamemode().isGodmode()) return;
        durability += Math.clamp(durability,
                0, tier.getDurability());
    }

    /**
     * Returns the enchantments.
     * @return an array of {@link Enchantment} values; the enchantments
     */
    public Enchantment[] getEnchantments() {
        return enchantments;
    }

    /**
     * Applies enchant and updates the affected character or item state.
     * @param enchantment the {@link Enchantment} supplied as {@code enchantment}
     */
    public void enchant(Enchantment enchantment) {
        enchantments[enchantment.getType().ordinal()] = enchantment;
    }

    /**
     * Applies unenchant and updates the affected character or item state.
     * @param enchantment the {@link Enchantment} supplied as {@code enchantment}
     */
    public void unenchant(Enchantment enchantment) {
        enchantments[enchantment.getType().ordinal()] = null;
    }

    /**
     * Checks whether the be used condition is met.
     * @return {@code true} if be used; otherwise {@code false}
     */
    public boolean canBeUsed() {
        return durability > 0;
    }
}
