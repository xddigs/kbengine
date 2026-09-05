package com.isofarm.item;

import com.isofarm.craft.Recipe;
import com.isofarm.craft.RecipeRegistry;
import com.isofarm.data.Inventory;
import com.isofarm.entity.Player;
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
    private RecipeOrder recipeOrder = RecipeOrder.NAME;

    private enum RecipeOrder {
        NAME,
        TYPE
    }

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
        List<Recipe> recipes = RecipeRegistry.reg.getRecipes();
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
                    .map(ingredient -> ingredient.craftable()
                            .getDisplayName() + " x " + ingredient.amount())
                    .collect(Collectors.joining("\n"));
            String tooltip = recipe.result().getDisplayName()
                    + (ingredients.isEmpty() ? "" : "\n" + ingredients);

            page.addItem(recipe.result(),
                    line -> CraftingService.cs.craft(recipe), tooltip);
            lineCount++;
        }
    }

    /**
     * Sorts the recipes alphabetically by their localized result name.
     */
    public void sortByName() {
        recipeOrder = RecipeOrder.NAME;
        reload();
    }

    /**
     * Groups recipes by result type and crafting tier, then sorts each group by name.
     */
    public void sortByType() {
        recipeOrder = RecipeOrder.TYPE;
        reload();
    }

    private Comparator<Recipe> recipeComparator() {
        Comparator<Recipe> byName = Comparator.comparing(
                recipe -> recipe.result().getDisplayName(),
                String.CASE_INSENSITIVE_ORDER);
        if (recipeOrder == RecipeOrder.NAME) {
            return byName;
        }

        return Comparator
                .comparing((Recipe recipe) -> recipe.result().getClass().getSimpleName())
                .thenComparingInt(recipe -> recipe.tier().getId())
                .thenComparing(byName);
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
