package org.kbeng.data;

import org.kbeng.item.Item;
import org.kbeng.utils.Local;

import java.util.Locale;
import java.util.Objects;

/**
 * Represents the seed component of the Isofarm runtime.
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
@DataClass
public class Seed implements Item, Plantable {
    private final CropType type;
    private final byte id;
    private final String displayName;
    private final String name;
    private final int value;

    /**
     * Creates a new {@code Seed} instance.
     * @param type the {@link CropType} supplied as {@code type}
     */
    public Seed(CropType type) {
        this.type = type;
        this.id = type.getId();
        this.displayName = getDisplayName(type.getName().toLowerCase(Locale.ROOT));
        this.name = type.getName() + "_seed";
        this.value = type.getValue();
    }

    /**
     * Creates a new {@code Seed} instance.
     */
    public Seed() {
        this(CropType.WHEAT);
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
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    public String getDisplayName() {
        return displayName;
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
     * Returns the type.
     * @return the {@link CropType} representing the type
     */
    @Override
    public CropType getType() {
        return type;
    }

    /**
     * Returns the display name.
     * @param name the {@link String} supplied as {@code name}
     * @return the {@link String} representing the display name
     */
    private String getDisplayName(String name) {
        return Local.lang.t("crop." + name + ".seed");
    }

    /**
     * {@inheritDoc}
     * Determines whether equals satisfies the required comparison or validity rules.
     * @param o the {@link Object} supplied as {@code o}
     * @return {@code boolean}; the equals result
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seed seed)) return false;
        return type == seed.type;
    }

    /**
     * {@inheritDoc}
     * Checks whether hash code.
     * @return {@code int}; the hash code result
     */
    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Seed(type);
    }
}
