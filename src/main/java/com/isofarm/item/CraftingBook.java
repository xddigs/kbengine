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
public class CraftingBook extends Book implements Equippable,
        Undroppable {
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

    /** {@inheritDoc} */
    @Override
    public boolean use(GameMaster gameMaster, boolean isCtrlHeld) {
        Inventory inventory = Player.plyr.getInventory();
        if (inventory == null) return false;
        if (isCtrlHeld) {
            if (!inventory.hasBookEquipped()) {
                if (this.equip()) {
                    GameUIService.ui.resetHotbarPosition();
                }
            } else {
                this.unequip();
            }
        } else {
            super.use(gameMaster, isCtrlHeld);
        }
        return true;
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

    /** {@inheritDoc} */
    @Override
    public boolean equip() {
        if (!Player.plyr.getInventory().hasBookEquipped()) {
            Player.plyr.getInventory().equipBook(this);
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean unequip() {
        if (Player.plyr.getInventory().hasBookEquipped()) {
            Player.plyr.getInventory().unequipBook();
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEquipped() {
        return Player.plyr.getInventory().hasBookEquipped();
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

    /** Returns whether the favorite-only filter is enabled. */
    public boolean isShowingOnlyFavoriteRecipes() {
        return areOnlyFavoriteRecipes;
    }

    /**
     * Returns the comparator used to sort recipes.
     * @return the {@link Comparator} used to sort recipes
     */
    private Comparator<Recipe> recipeComparator() {
        return Comparator.comparing(Recipe::result, Inventory.sorter(recipeOrder));
    }
}
