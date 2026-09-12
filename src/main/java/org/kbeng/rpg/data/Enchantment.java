package org.kbeng.rpg.data;

/**
 * Enchantment provides enchantment capabilities within the data subsystem.
 * It represents strongly typed domain data shared between simulation, rendering, input, and persistence boundaries.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
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
