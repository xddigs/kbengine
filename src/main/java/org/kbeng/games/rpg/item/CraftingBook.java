package org.kbeng.games.rpg.item;

import org.kbeng.games.rpg.craft.Recipe;
import org.kbeng.games.rpg.craft.RecipeRegistry;
import org.kbeng.games.rpg.data.Inventory;
import org.kbeng.engine.input.ControlAction;
import org.kbeng.engine.input.Controls;
import org.kbeng.games.rpg.service.CraftingService;
import org.kbeng.games.rpg.wrld.GameMaster;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CraftingBook provides crafting book capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Book and implements Undroppable, combining inherited behavior with explicit runtime contracts.
 */
public class CraftingBook extends Book implements Undroppable {
    private static final int LINES_PER_PAGE = 16;
    private Inventory.SortOrder recipeOrder = Inventory.SortOrder.CREATIVE;
    private boolean areOnlyCraftableRecipes;
    private boolean areOnlyFavoriteRecipes;
    private boolean isSortedByType;
    private boolean isSortedByName;

    /**
     * Creates a new {@code CraftingBook} instance.
     */
    public CraftingBook() {
        super();
        if (!hasContent()) {
            return;
        }

        reload();
    }

    /** Opens the book only while this instance is stored in the equipped backpack. */
    @Override
    public boolean use(GameMaster gameMaster, boolean isCtrlHeld) {
        return super.use(gameMaster, isCtrlHeld);
    }

    /**
     * {@inheritDoc}
     * Reloads this object from its authoritative source.
     */
    @Override
    public void reload() {
        clearPages();
        resetCurrentPage();
        List<Recipe> recipes = RecipeRegistry.reg.getRecipes();
        if (areOnlyCraftableRecipes) {
            recipes = recipes.stream()
                    .filter(CraftingService.cs::canCraft)
                    .collect(Collectors.toList());
        }
        if (areOnlyFavoriteRecipes) {
            recipes = recipes.stream().filter(Recipe::isFavorite).collect(Collectors.toList());
        }
        recipes.sort(recipeComparator());
        if (recipes.isEmpty()) return;

        Page page = new Page();
        addPage(page);
        int lineCount = 0;
        for (Recipe recipe : recipes) {
            if (recipe.result() == null) continue;
            if (lineCount >= LINES_PER_PAGE) {
                page = new Page();
                addPage(page);
                lineCount = 0;
            }

            String ingredients = recipe.ingredients()
                    .stream()
                    .map(ingredient -> ingredient.options().stream()
                            .map(option -> option.craftable().getDisplayName()
                                    + " x " + option.amount())
                            .collect(Collectors.joining(" || ")))
                            .collect(Collectors.joining("\n"));
            String tooltip = recipe.result().getDisplayName()
                    + (ingredients.isEmpty() ? "" : "\n" + ingredients);

            page.addItem(recipe.result(), line -> {
                        if (Controls.isDown(ControlAction.SMART_SHIFT)) {
                            recipe.toggleFavorite();
                            reload();
                        } else {
                            CraftingService.cs.craft(recipe);
                        }
                    }, tooltip).setFavorite(recipe.isFavorite());
            lineCount++;
        }
    }

    /**
     * Sorts recipes alphabetically by their localized result name.
     */
    public void sortByName() {
        isSortedByName = !isSortedByName;
        if (isSortedByName) {
            recipeOrder = Inventory.SortOrder.CREATIVE;
        } else {
            recipeOrder = Inventory.SortOrder.NAME;
        }
        reload();
    }

    /**
     * Sorts recipes by their shared item type order.
     */
    public void sortByType() {
        isSortedByType = !isSortedByType;
        if (isSortedByType) {
            recipeOrder = Inventory.SortOrder.CREATIVE;
        } else {
            recipeOrder = Inventory.SortOrder.TYPE;
        }
        reload();
    }

    /**
     * Alternates between every registered recipe and only recipes the player can craft.
     */
    public void toggleCraftableRecipes() {
        areOnlyCraftableRecipes = !areOnlyCraftableRecipes;
        reload();
    }

    /**
     * Checks whether this book currently filters to craftable recipes.
     * @return {@code true} when only craftable recipes are shown
     */
    public boolean isShowingOnlyCraftableRecipes() {
        return areOnlyCraftableRecipes;
    }

    /** Toggles the in-memory filter that shows only favorited recipes. */
    public void toggleFavoriteRecipes() {
        areOnlyFavoriteRecipes = !areOnlyFavoriteRecipes;
        reload();
    }

    /**
     * Returns the comparator used to sort recipes.
     * @return the {@link Comparator} used to sort recipes
     */
    private Comparator<Recipe> recipeComparator() {
        return Comparator.comparing(Recipe::result, Inventory.sorter(recipeOrder));
    }
}
