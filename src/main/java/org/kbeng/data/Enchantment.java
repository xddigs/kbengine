package org.kbeng.data;

/**
 * Represents the enchantment component of the Isofarm runtime.
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
public class Enchantment {
    private final Enchantments type;
    private final String name;
    private final int cost;
    private int level;

    /**
     * Creates a new {@code Enchantment} instance.
     * @param type the {@link Enchantments} supplied as {@code type}
     * @param level the {@code int} supplied as {@code level}
     * @param cost the {@code int} supplied as {@code cost}
     */
    public Enchantment(Enchantments type, int level, int cost) {
        this.type = type;
        this.name = type.getName();
        this.level = level;
        this.cost = cost;
    }

    /**
     * Returns the type.
     * @return the {@link Enchantments} representing the type
     */
    public Enchantments getType() {
        return type;
    }

    /**
     * Returns the name.
     * @return the {@link String} representing the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the level.
     * @return {@code int}; the level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Applies upgrade and updates the affected character or item state.
     */
    public void upgrade() {
        level++;
    }

    /**
     * Returns the cost.
     * @return {@code int}; the cost
     */
    public int getCost() {
        return cost;
    }
}
