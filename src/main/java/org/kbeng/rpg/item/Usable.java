package org.kbeng.rpg.item;

import org.kbeng.rpg.data.Usables;
import org.kbeng.engine.utils.Local;
import org.kbeng.rpg.wrld.GameMaster;

/**
 * Usable provides usable capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It implements Craftable, providing a concrete strategy for this subsystem contract.
 */
public abstract class Usable implements Craftable,
        Enchantable {
    private final Usables usablesID;
    private final byte id;
    private final int value;
    private String name;

    /**
     * Creates a new {@code Usable} instance.
     * @param usablesID the {@link Usables} supplied as {@code usablesID}
     */
    public Usable(Usables usablesID) {
        this.usablesID = usablesID;
        this.id = usablesID.getId();
        this.value = usablesID.getValue();
    }

    /**
     * Creates a new {@code Usable} instance.
     * @param usablesID the {@link Usables} supplied as {@code usablesID}
     * @param name the {@link String} supplied as {@code name}
     */
    public Usable(Usables usablesID, String name) {
        this(usablesID);
        this.name = name;
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
     * Returns the value.
     * @return {@code int}; the value
     */
    @Override
    public int getValue() {
        return value;
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
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    public String getDisplayName() {
        return Local.lang.t("item.usable." + usablesID.name().toLowerCase());
    }

    /**
     * Returns the usables id.
     * @return the {@link Usables} representing the usables id
     */
    public Usables getUsablesID() {
        return usablesID;
    }

    /**
     * Sets the name.
     * @param name the {@link String} supplied as {@code name}
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Handles use and applies its effect to the current interaction state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param isCtrlHeld the {@code boolean} supplied as {@code isCtrlHeld}
     * @return {@code boolean}; the use result
     */
    public abstract boolean use(GameMaster gameMaster, boolean isCtrlHeld);
    /**
     * Updates the current state.
     */
    public abstract void update();
}
