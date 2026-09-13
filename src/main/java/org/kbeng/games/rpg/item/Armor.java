package org.kbeng.games.rpg.item;

import org.kbeng.games.rpg.data.ArmorData;
import org.kbeng.games.rpg.data.ArmorSlot;
import org.kbeng.games.rpg.data.Tier;
import org.kbeng.games.rpg.entity.Player;
import org.kbeng.engine.utils.Local;

/**
 * Armor provides armor capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public abstract class Armor implements
        Craftable, Equippable {
    private final byte id;
    private final Tier tier;
    private final ArmorData type;
    private final float defense;

    /** Creates a new {@code Armor} instance. */
    public Armor(byte id, Tier tier, ArmorData type, float defense) {
        this.id = id;
        this.tier = tier;
        this.type = type;
        this.defense = defense;
    }

    /**
     * Creates a new {@code Armor} instance.
     * Which defaults to a {@code Leather} {@link Chestplate}
     */
    public Armor() {
        this((byte) 1, Tier.LEATHER, ArmorData.CHESTPLATE, ArmorData.CHESTPLATE.getDefense() +
                Tier.LEATHER.getDefense());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equip() {
        return Player.plyr != null && Player.plyr.getInventory().equipArmor(this);
    }

    /** {@inheritDoc} */
    @Override
    public boolean unequip() {
        return Player.plyr != null && Player.plyr.getInventory()
                .unequipArmor(ArmorSlot.values()[type.getId()]);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEquipped() {
        if (Player.plyr == null) return false;
        ArmorSlot slot = ArmorSlot.values()[type.getId()];
        return Player.plyr.getInventory().getArmorSlot(slot).getItem() == this;
    }

    /** {@inheritDoc} */
    @Override
    public byte getId() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return getType().getName();
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return Local.lang.item(type.getDisplayName(), tier.getDisplayName());
    }

    /** {@inheritDoc} */
    @Override
    public int getValue() {
        return type.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public Item copy() {
        return this;
    }

    /**
     * Returns the {@code defense} value
     * @return {@link float} value of defense
     */
    public float getDefense() {
        return defense;
    }

    /**
     * Returns the {@code tier} value
     * @return {@link Tier} value of tier
     */
    public Tier getTier() {
        return tier;
    }

    /**
     * Returns the {@code type} value
     * @return {@link ArmorData} value of type
     */
    public ArmorData getType() {
        return type;
    }

    /**
     * Stable location reserved for this piece's future wearable model. The
     * model is intentionally not loaded until armor visuals are implemented.
     */
    public String getModelPath() {
        return "assets/models/armor/" + tier.getName() + "_" + type.getName() + ".gltf";
    }
}
