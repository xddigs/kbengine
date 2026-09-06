package com.isofarm.craft;

import com.isofarm.data.*;
import com.isofarm.item.*;

import java.util.*;
import java.util.function.Function;

/**
 * Encapsulates the state and operations required by recipe registry within the game runtime.
 */
public class RecipeRegistry {
    public static final RecipeRegistry reg = new RecipeRegistry();
    private static final List<Recipe> recipes = new LinkedList<>();

    /**
     * Initializes the component.
     * @return the {@link List} representing the init result
     */
    public List<Recipe> init() {
        recipes.clear();
        registerBlocksRecipes();
        registerSmeltingRecipes();
        registerMaterialRecipes();

        create().result(new Block(BlockData.OAK_PLANK), 4).with(BlockData.fromIdTo(BlockData.OAK_LOG.getId()),1).add();
        create().result(new Block(BlockData.SPRUCE_PLANK), 4).with(BlockData.fromIdTo(BlockData.SPRUCE_LOG.getId()),1).add();
        create().with(new Block(BlockData.OAK_PLANK), 8).result(new iBlock(InteractiveBlocks.CHEST), 1).add();

        create().result(new Material(Tier.NONE, MaterialID.STICK), 4).with(BlockData.fromIdTo(BlockData.OAK_PLANK.getId()), 1).add();
        create().result(new Book(false), 1).with(MaterialID.LEATHER, 3).with(MaterialID.PAPER, 2).add();
        create().result(new Backpack(), 1).with(MaterialID.LEATHER, 3).add();
        create().result(new Bucket(), 1).with(new MiningComponent(Tier.STEEL, MaterialID.INGOT), 3).add();
        registerToolSet(BlockData.fromIdTo(BlockData.OAK_PLANK.getId()));

        Map<Tier, Tier> metalProgression = Map.of(
                Tier.COPPER, Tier.COPPER,
                Tier.IRON, Tier.COPPER,
                Tier.STEEL, Tier.IRON,
                Tier.GOLDEN, Tier.STEEL,
                Tier.PLATINUM, Tier.GOLDEN,
                Tier.DIAMOND, Tier.PLATINUM);

        metalProgression.forEach((toolTier, requiredStationTier) -> {
            Craftable mainMaterial = new MiningComponent(toolTier, MaterialID.INGOT);
            registerToolSet(mainMaterial);
        });

        return recipes;
    }

    /**
     * Adds tool set to the corresponding collection or processing queue.
     * @param primaryMat the {@link Craftable} supplied as {@code primaryMat}
     */
    private void registerToolSet(Craftable primaryMat) {
        registerTool(primaryMat, 2, 1, Sword::new);
        registerTool(primaryMat, 3, 2, Pickaxe::new);
        registerTool(primaryMat, 3, 3, Axe::new);
        registerTool(primaryMat, 2, 2, Hoe::new);
        registerTool(primaryMat, 1, 2, Shovel::new);
    }

    /**
     * Adds tool to the corresponding collection or processing queue.
     * @param mat the {@link Craftable} supplied as {@code mat}
     * @param matAmount the {@code int} supplied as {@code matAmount}
     * @param stickAmount the {@code int} supplied as {@code stickAmount}
     * @param constructor the {@link Function} supplied as {@code constructor}
     */
    private void registerTool(Craftable mat, int matAmount, int stickAmount,
                                     Function<Tier, Item> constructor) {
        create().result(constructor.apply(getTierFromMaterial(mat)), 1)
                .with(mat, matAmount)
                .with(MaterialID.STICK, stickAmount)
                .add();
    }

    /**
     * Returns the tier from material.
     * @param mat the {@link Craftable} supplied as {@code mat}
     * @return the {@link Tier} representing the tier from material
     */
    private Tier getTierFromMaterial(Craftable mat) {
        return (mat instanceof MiningComponent mc) ? mc.getTier() : Tier.WOODEN;
    }

    /**
     * Registers the recipes contributed by blocks.
     */
    private void registerBlocksRecipes() {
        registerSpecialBlocks(BlockData.OAK_PLANK);
        registerSpecialBlocks(BlockData.SPRUCE_PLANK);
        registerSpecialBlocks(BlockData.STONE);
    }

    /**
     * Adds smelting recipes to the corresponding collection or processing queue.
     */
    private void registerSmeltingRecipes() {
        Tier[] metalTiers = {Tier.COPPER, Tier.IRON, Tier.STEEL, Tier.GOLDEN, Tier.PLATINUM, Tier.DIAMOND};
        for (Tier tier : metalTiers) {
            create().result(new MiningComponent(tier, MaterialID.INGOT), 1)
                    .with(new MiningComponent(tier, MaterialID.RAW_ORE), 1).add();
        }
    }

    /**
     * Adds material recipes to the corresponding collection or processing queue.
     */
    private void registerMaterialRecipes() {
        Tier tier = Tier.NONE;
        create().with(new Material(tier, MaterialID.SUGAR_CANE), 1)
                .result(new Material(tier, MaterialID.PAPER), 2).add();
        create().with(new Material(tier, MaterialID.SUGAR_CANE), 1)
                .result(new Material(tier, MaterialID.SUGAR), 4).add();
    }

    private void registerSpecialBlocks(BlockData primaryMat) {
        create().with(new Block(primaryMat), 3)
                .result(new Block(BlockData.toSlab(primaryMat, false)), 6).add();

        create().with(new Block(primaryMat), 3)
                .result(new Block(BlockData.toSlab(primaryMat, true)), 6).add();

        create().with(new Block(primaryMat), 7)
                .result(new Block(BlockData.toStaircase(primaryMat)), 4).add();

        if (!primaryMat.equals(BlockData.STONE)) {
            create().with(new Block(primaryMat), 6)
                    .result(new iBlock(InteractiveBlocks.toDoor(primaryMat)), 1).add();

            create().with(new Block(primaryMat), 6)
                .result(new Block(BlockData.toFence(primaryMat)), 4).add();
        }
    }

    /**
     * Returns the recipes.
     * @return the {@link List} representing the recipes
     */
    public List<Recipe> getRecipes() {
        List<Recipe> sortedRecipes = new ArrayList<>(recipes);
        sortedRecipes.sort(Comparator.comparing(
                recipe -> recipe.result().getDisplayName(),
                String.CASE_INSENSITIVE_ORDER
        ));
        return sortedRecipes;
    }

    /**
     * Returns create.
     * @return the {@link RecipeBuilder} representing the create result
     */
    public RecipeBuilder create() {
        return new RecipeBuilder();
    }

    /**
     * Encapsulates the state and operations required by recipe builder within the game runtime.
     */
    public static class RecipeBuilder {
        private final List<Ingredient> ingredients = new ArrayList<>();
        private Item result;
        private int amount = 1;

        /**
         * Creates a new {@code RecipeBuilder} instance.
         */
        public RecipeBuilder() {}

        /**
         * Creates or returns result from the supplied arguments.
         * @param result the {@link Item} supplied as {@code result}
         * @param amount the {@code int} supplied as {@code amount}
         * @return the {@link RecipeBuilder} representing the result result
         */
        public RecipeBuilder result(Item result, int amount) {
            this.result = result;
            this.amount = amount;
            return this;
        }

        /**
         * Updates or derives runtime state for with according to the supplied arguments.
         * @param craftable the {@link Craftable} supplied as {@code craftable}
         * @param count the {@code int} supplied as {@code count}
         * @return the {@link RecipeBuilder} representing the with result
         */
        public RecipeBuilder with(Craftable craftable, int count) {
            this.ingredients.add(new Ingredient(craftable, count));
            return this;
        }

        /**
         * Adds add.
         * @return the {@link Recipe} representing the add result
         */
        public Recipe add() {
            Recipe recipe = new Recipe(result, amount, List.copyOf(ingredients));
            recipes.add(recipe);
            return recipe;
        }
    }
}
