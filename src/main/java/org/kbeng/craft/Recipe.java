package org.kbeng.craft;

import org.kbeng.data.MaterialID;
import org.kbeng.data.Produce;
import org.kbeng.item.*;

import java.util.List;
import java.util.Map;

/**
 * Recipe provides recipe capabilities within the craft subsystem.
 *
 * It supports recipe composition, ingredient alternatives, and deterministic crafting resolution used by gameplay systems.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public final class Recipe {
    private final Item result;
    private final int resultAmount;
    private final List<Ingredient> ingredients;
    private boolean isFavorite;

    /**
     * Creates a new {@code Recipe} instance.
     * @param result the {@link Item} supplied as {@code result}
     * @param resultAmount the {@code int} supplied as {@code resultAmount}
     * @param ingredients the {@link List} of {@link Ingredient} values supplied as {@code ingredients}
     */
    public Recipe(Item result, int resultAmount, List<Ingredient> ingredients) {
        this(result, resultAmount, ingredients, false);
    }

    /**
     * Creates a new {@code Recipe} instance.
     * @param result the {@link Item} supplied as {@code result}
     * @param resultAmount the {@code int} supplied as {@code resultAmount}
     * @param ingredients the {@link List} of {@link Ingredient} values supplied as {@code ingredients}
     * @param isFavorite the {@code boolean} supplied as {@code isFavorite}
     */
    public Recipe(Item result, int resultAmount, List<Ingredient> ingredients, boolean isFavorite) {
        this.result = result;
        this.resultAmount = resultAmount;
        this.ingredients = List.copyOf(ingredients);
        this.isFavorite = isFavorite;
    }

    public Item result() { return result; }
    public int resultAmount() { return resultAmount; }
    public List<Ingredient> ingredients() { return ingredients; }
    public boolean isFavorite() { return isFavorite; }
    public void toggleFavorite() { isFavorite = !isFavorite; }

    /**
     * Creates or returns of from the supplied arguments.
     * @param result the {@link Item} supplied as {@code result}
     * @param resultAmount the {@code int} supplied as {@code resultAmount}
     * @param ingredients an array of {@link Ingredient} values supplied as {@code ingredients}
     * @return the {@link Recipe} representing the of result
     */
    public static Recipe of(Item result, int resultAmount, Ingredient... ingredients) {
        return new Recipe(result, resultAmount, List.of(ingredients));
    }

    /**
     * Determines whether match satisfies the required comparison or validity rules.
     * @param inputIngredients the {@link Map} supplied as {@code inputIngredients}
     * @return {@code boolean}; the match result
     */
    public boolean match(Map<Craftable, Integer> inputIngredients) {
        if (inputIngredients.size() != ingredients.size()) {
            return false;
        }

        for (Ingredient req : ingredients) {
            boolean found = false;
            for (Map.Entry<Craftable, Integer> entry : inputIngredients.entrySet()) {
                for (Ingredient option : req.options()) {
                    if (isSameCraftable(option.craftable(), entry.getKey())) {
                        if (entry.getValue() != option.amount()) return false;
                        found = true;
                        break;
                    }
                }
                if (found) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    /**
     * Checks whether the craft with condition is met.
     * @param availableMaterials the {@link Map} supplied as {@code availableMaterials}
     * @return {@code true} if craft with; otherwise {@code false}
     */
    public boolean canCraftWith(Map<Craftable, Integer> availableMaterials) {
        for (Ingredient req : ingredients) {
            boolean available = false;
            for (Ingredient option : req.options()) {
                int amount = 0;
                for (Map.Entry<Craftable, Integer> entry : availableMaterials.entrySet()) {
                    if (isSameCraftable(option.craftable(), entry.getKey())) {
                        amount += entry.getValue();
                    }
                }
                if (amount >= option.amount()) {
                    available = true;
                    break;
                }
            }
            if (!available) return false;
        }
        return true;
    }

    /**
     * Checks whether the same craftable condition is met.
     * @param a the {@link Craftable} supplied as {@code a}
     * @param b the {@link Craftable} supplied as {@code b}
     * @return {@code true} if same craftable; otherwise {@code false}
     */
    public static boolean isSameCraftable(Craftable a, Craftable b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        return switch (a) {
            case MaterialID mid1 when b instanceof MaterialID mid2 -> mid1 == mid2;
            case MiningComponent mc1 when b instanceof MiningComponent mc2 -> mc1.getTier() == mc2.getTier()
                    && mc1.getId() == mc2.getId();
            case Block blk1 when b instanceof Block blk2 -> blk1.getType() == blk2.getType();
            case Food food1 -> b instanceof Food food2 && food1.type() == food2.type();
            case Produce produce1 -> b instanceof Produce produce2
                    && produce1.getType() == produce2.getType();
            case Material mat1 when b instanceof Material mat2 -> mat1.getId() == mat2.getId();
            default -> a.getId() == b.getId();
        };

    }

    /**
     * {@inheritDoc}
     * Produces the textual or converted representation for to string.
     * @return the {@link String} representing the to string result
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(result.getName())
                .append(" x ")
                .append(resultAmount)
                .append("]");

        for (Ingredient ingredient : ingredients) {
            List<Ingredient> options = ingredient.options();
            for (int index = 0; index < options.size(); index++) {
                if (index > 0) sb.append(" or ");
                Ingredient option = options.get(index);
                sb.append(option.craftable().getName())
                        .append(" x ")
                        .append(option.amount());
            }
            sb
                    .append(", ");
        }
        return sb.toString();
    }
}
