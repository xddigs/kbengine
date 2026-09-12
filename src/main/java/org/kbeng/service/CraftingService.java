package org.kbeng.service;

import org.kbeng.craft.Ingredient;
import org.kbeng.craft.Recipe;
import org.kbeng.data.*;
import org.kbeng.entity.Player;
import org.kbeng.item.*;
import org.kbeng.utils.ToastFactory;

/**
 * Represents the crafting service component of the kbengine runtime.
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
@SuppressWarnings("all")
@Singleton
public class CraftingService {
    public static final CraftingService cs = new CraftingService();

    /**
     * Checks whether the craft condition is met.
     * @param recipe the {@link Recipe} supplied as {@code recipe}
     * @return {@code true} if craft; otherwise {@code false}
     */
    public boolean canCraft(Recipe recipe) {
        Player player = Player.plyr;
        if (recipe == null) return false;
        Inventory inventory = player.getInventory();
        if (inventory == null) return false;

        for (Ingredient ingredient : recipe.ingredients()) {
            if (selectAvailableOption(inventory, ingredient) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Applies the world or inventory action represented by craft.
     * @param recipe the {@link Recipe} supplied as {@code recipe}
     * @return {@code boolean}; the craft result
     */
    public boolean craft(Recipe recipe) {
        Player player = Player.plyr;
        if (recipe == null) return false;

        if (!canCraft(recipe)) {
            ToastFactory.error("toast.no_ingredients");
            return false;
        }

        Inventory inventory = player.getInventory();
        consume(inventory, recipe);
        give(recipe);
        SoundService.fx.playEntitySound(SoundGroup.ITEMS);
        return true;
    }

    /**
     * Applies consume and updates the affected character or item state.
     * @param inputSlots an array of {@link InventorySlot} values supplied as {@code inputSlots}
     * @param recipe the {@link Recipe} supplied as {@code recipe}
     */
    private void consume(InventorySlot[] inputSlots, Recipe recipe) {
        for (Ingredient ingredient : recipe.ingredients()) {
            Ingredient selectedOption = selectAvailableOption(inputSlots, ingredient);
            if (selectedOption == null) continue;
            int remainingToDeduct = selectedOption.amount();
            for (InventorySlot slot : inputSlots) {
                if (slot == null || slot.isEmpty()) {
                    continue;
                }

                if (!matchesIngredient(selectedOption, slot.getItem())) {
                    continue;
                }
                int amountInSlot = slot.getAmount();
                int toTake = Math.min(remainingToDeduct, amountInSlot);
                slot.setAmount(amountInSlot - toTake);

                if (slot.getAmount() <= 0) {
                    slot.clear();
                }

                remainingToDeduct -= toTake;
                if (remainingToDeduct <= 0) {
                    break;
                }
            }
        }
    }

    /**
     * Applies consume and updates the affected character or item state.
     * @param inventory the {@link Inventory} supplied as {@code inventory}
     * @param recipe the {@link Recipe} supplied as {@code recipe}
     */
    private void consume(Inventory inventory, Recipe recipe) {
        InventorySlot[] slots = inventory.getSlots().toArray(new InventorySlot[0]);
        consume(slots, recipe);
    }

    /**
     * Returns the number or extent represented by count.
     * @param inventory the {@link Inventory} supplied as {@code inventory}
     * @param ingredient the {@link Ingredient} supplied as {@code ingredient}
     * @return {@code int}; the count result
     */
    private int count(Inventory inventory, Ingredient ingredient) {
        if (inventory == null || ingredient == null) {

            return 0;
        }

        int amount = 0;
        for (InventorySlot slot : inventory.getSlots()) {
            if (slot == null || slot.isEmpty()) {
                continue;
            }
            if (matchesIngredient(ingredient, slot.getItem())) {
                amount += slot.getAmount();
            }
        }
        return amount;
    }

    /** Returns an option that the inventory can fully satisfy, if any. */
    private Ingredient selectAvailableOption(Inventory inventory, Ingredient ingredient) {
        if (inventory == null) return null;
        return selectAvailableOption(inventory.getSlots().toArray(new InventorySlot[0]), ingredient);
    }

    /** Returns an option that the supplied slots can fully satisfy, if any. */
    private Ingredient selectAvailableOption(InventorySlot[] slots, Ingredient ingredient) {
        if (slots == null || ingredient == null) return null;
        for (Ingredient option : ingredient.options()) {
            int available = 0;
            for (InventorySlot slot : slots) {
                if (slot != null && !slot.isEmpty() && matchesIngredient(option, slot.getItem())) {
                    available += slot.getAmount();
                }
            }
            if (available >= option.amount()) return option;
        }
        return null;
    }

    /**
     * Updates or derives runtime state for matches ingredient according to the supplied arguments.
     * @param ingredient the {@link Ingredient} supplied as {@code ingredient}
     * @param item the {@link Item} supplied as {@code item}
     * @return {@code boolean}; the matches ingredient result
     */
    public boolean matchesIngredient(Ingredient ingredient, Item item) {
        if (ingredient == null || item == null) {
            return false;
        }

        for (Ingredient option : ingredient.options()) {
            Craftable craftable = option.craftable();
            if (craftable instanceof MaterialID materialID && item instanceof Material material
                    && material.getId() == materialID.getId()) return true;
            if (craftable instanceof MiningComponent miningComponent) {
                if (item instanceof MiningComponent itemMiningComponent
                        && miningComponent.getTier() == itemMiningComponent.getTier()
                        && miningComponent.getId() == itemMiningComponent.getId()) return true;
                continue;
            }
            if (craftable instanceof Item craftableItem && isSameType(craftableItem, item)) return true;
        }
        return false;
    }

    /**
     * Processes give and updates the affected inventory or currency balances.
     * @param recipe the {@link Recipe} supplied as {@code recipe}
     */
    private void give(Recipe recipe) {
        Player player = Player.plyr;
        Item result = recipe.result().copy();
        Inventory inventory = player.getInventory();
        int remaining = inventory.add(result, recipe.resultAmount());
        if (!player.hasSpace()) {
            player.addToBackpack(result, remaining);
        }

        if (!player.hasSpace()) {
            ToastFactory.error("toast.no_space");
        }
    }

    /**
     * Checks whether the same type condition is met.
     * @param a the {@link Item} supplied as {@code a}
     * @param b the {@link Item} supplied as {@code b}
     * @return {@code true} if same type; otherwise {@code false}
     */
    public boolean isSameType(Item a, Item b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.getClass() != b.getClass()) {
            return false;
        }

        return switch (a) {
            case Produce p1 when b instanceof Produce p2 -> p1.getType() == p2.getType();
            case Seed s1 when b instanceof Seed s2 -> s1.getType() == s2.getType();
            case Crop c1 when b instanceof Crop c2 -> c1.getCropType() == c2.getCropType();
            case Block b1 when b instanceof Block b2 -> b1.getType() == b2.getType();
            case Tool t1 when b instanceof Tool t2 -> t1.getId() == t2.getId() && t1.getType() == t2.getType();
            default -> a.getName().equals(b.getName());
        };
    }
}
