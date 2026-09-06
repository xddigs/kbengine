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
     * Groups recipes as blocks, tools, usables, crafting materials and interactive
     * blocks, in that order. Tools are additionally grouped by ascending tier
     * before sorting by localized name.
     */
    public void sortByType() {
        recipeOrder = RecipeOrder.TYPE;
        reload();
    }

    /**
     * Returns the shared item ordering used by the crafting book and creative inventory.
     * Supported categories are blocks, tools, usables, crafting materials and interactive
     * blocks, in that order. Tools are ordered by ascending tier within their category.
     * @return the category-first item comparator
     */
    public static Comparator<Item> itemTypeComparator() {
        return Comparator
                .comparingInt(CraftingBook::itemTypeOrder)
                .thenComparingInt(CraftingBook::toolTierOrder)
                .thenComparing(Item::getDisplayName, String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Returns the tier order for tools without affecting any other item category.
     * @param item the item whose tool tier is inspected
     * @return the ascending tool-tier position, or {@code 0} for non-tools
     */
    private static int toolTierOrder(Item item) {
        return item instanceof Tool tool ? tool.getTier().ordinal() : 0;
    }

    /**
     * Returns the ordering of the item type used by the crafting book and creative inventory.
     * @param item the {@link Item} supplied as {@code item}
     * @return {@link Integer} the item type order
     */
    private static int itemTypeOrder(Item item) {
        return switch (item) {
            case Block ignored -> 0;
            case iBlock ignored -> 1;
            case Tool ignored -> 2;
            case Usable ignored -> 3;
            case Material ignored -> 4;
            case null, default -> 5;
        };
    }

    /**
     * Returns the comparator used to sort recipes.
     * @return the {@link Comparator} used to sort recipes
     */
    private Comparator<Recipe> recipeComparator() {
        Comparator<Recipe> byName = Comparator.comparing(
                recipe -> recipe.result().getDisplayName(),
                String.CASE_INSENSITIVE_ORDER);
        if (recipeOrder == RecipeOrder.NAME) {
            return byName;
        }

        return Comparator.comparing(Recipe::result, itemTypeComparator());
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
