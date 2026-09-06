package com.isofarm.item;

import com.isofarm.craft.Recipe;
import com.isofarm.craft.RecipeRegistry;
import com.isofarm.data.Inventory;
import com.isofarm.entity.Player;
import com.isofarm.input.ControlAction;
import com.isofarm.input.Controls;
import com.isofarm.service.CraftingService;
import com.isofarm.ui.GameUIService;
import com.isofarm.wrld.GameMaster;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Encapsulates the state and operations required by crafting book within the game runtime.
 */
public class CraftingBook extends Book implements Undroppable {
    private static final int LINES_PER_PAGE = 16;
    private boolean areOnlyCraftableRecipes;
    private boolean areOnlyFavoriteRecipes;

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
                    }, tooltip)
                    .setFavorite(recipe.isFavorite());
            lineCount++;
        }
    }

    /**
     * Reloads recipes in their canonical ID/type order.
     */
    public void sortByName() {
        reload();
    }

    /**
     * Reloads recipes in their canonical ID/type order.
     */
    public void sortByType() {
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

    /** Returns whether the favorite-only filter is enabled. */
    public boolean isShowingOnlyFavoriteRecipes() {
        return areOnlyFavoriteRecipes;
    }

    /**
     * Returns the comparator used to sort recipes.
     * @return the {@link Comparator} used to sort recipes
     */
    private Comparator<Recipe> recipeComparator() {
        return Comparator.comparingInt((Recipe recipe) -> recipe.result().getId())
                .thenComparing(recipe -> recipe.result().getClass().getSimpleName());
    }

    /**
     * {@inheritDoc}
     * Handles use and applies its effect to the current interaction state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param isCtrlHeld the {@code boolean} supplied as {@code isCtrlHeld}
     * @return {@code boolean}; the use result
     */
    @Override
    public boolean use(GameMaster gameMaster, boolean isCtrlHeld) {
        Inventory inventory = Player.plyr.getInventory();
        if (inventory == null) return false;
        if (isCtrlHeld) {
            if (!inventory.hasBookEquipped()) {
                inventory.equipBook(this);
                GameUIService.ui.resetHotbarPosition();
            } else {
                inventory.unequipBook();
            }
        } else {
            super.use(gameMaster, isCtrlHeld);
        }
        return true;
    }
}
