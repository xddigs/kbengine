package org.kbeng.games.rpg.craft;

import org.kbeng.games.rpg.data.InventorySlot;
import org.kbeng.games.rpg.item.Craftable;
import org.kbeng.games.rpg.item.Item;

import java.util.*;

/**
 * RecipeMatcher provides recipe matcher capabilities within the craft subsystem.
 * It supports recipe composition, ingredient alternatives, and deterministic crafting resolution used by gameplay systems.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public class RecipeMatcher {

    /**
     * Updates or derives runtime state for summarize slots according to the supplied arguments.
     * @param slots an array of {@link InventorySlot} values supplied as {@code slots}
     * @return the {@link Map} representing the summarize slots result
     */
    public static Map<Craftable, Integer> summarizeSlots(InventorySlot[] slots) {
        Map<Craftable, Integer> summary = new HashMap<>();
        for (InventorySlot slot : slots) {
            if (slot != null && !slot.isEmpty()) {
                Craftable key = extract(slot.getItem());
                if (key != null) {
                    summary.put(key, summary.getOrDefault(key, 0) + slot.getAmount());
                }
            }
        }
        return summary;
    }

    /**
     * Determines whether match satisfies the required comparison or validity rules.
     * @param slots an array of {@link InventorySlot} values supplied as {@code slots}
     * @param registeredRecipes the {@link List} supplied as {@code registeredRecipes}
     * @return the {@link Optional} representing the match result
     */
    public static Optional<Recipe> match(InventorySlot[] slots, List<Recipe> registeredRecipes) {
        Map<Craftable, Integer> input = summarizeSlots(slots);
        if (input.isEmpty()) return Optional.empty();

        return registeredRecipes.stream()
                .filter(recipe -> recipe.match(input))
                .findFirst();
    }

    /**
     * Returns find.
     * @param availableMaterials the {@link Map} supplied as {@code availableMaterials}
     * @param registeredRecipes the {@link List} supplied as {@code registeredRecipes}
     * @return the {@link List} representing the find result
     */
    public static List<Recipe> find(Map<Craftable, Integer> availableMaterials,
                                    List<Recipe> registeredRecipes) {
        return registeredRecipes.stream()
                .filter(recipe -> recipe.canCraftWith(availableMaterials))
                .sorted(Comparator.comparingInt((Recipe r) -> r.ingredients().size()).reversed())
                .toList();
    }

    /**
     * Updates or derives runtime state for extract according to the supplied arguments.
     * @param item the {@link Item} supplied as {@code item}
     * @return the {@link Craftable} representing the extract result
     */
    private static Craftable extract(Item item) {
        if (item instanceof Craftable c) return c;
        return null;
    }
}
