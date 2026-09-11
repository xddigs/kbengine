package org.kbeng.item;

import org.kbeng.data.Usables;
import org.kbeng.utils.Local;
import org.kbeng.wrld.GameMaster;

/**
 * Represents the usable component of the Isofarm runtime.
 *
 * <p>This type centralizes the state, lifecycle and behavior required by its callers,
 * keeping domain rules together with the data they operate on.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li><b>State:</b> Owns the data needed to represent the component consistently.</li>
 *   <li><b>Lifecycle:</b> Exposes the operations used to create, update and release its state.</li>
 *   <li><b>Integration:</b> Coordinates with the surrounding game, rendering or UI systems through its public API.</li>
 * </ul>
 *
 * <p>Callers should use the documented public operations and allow this type to preserve
 * its invariants rather than modifying implementation details directly.
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
