package org.kbeng.games.rpg.craft;

import org.kbeng.games.rpg.data.*;
import org.kbeng.games.rpg.item.*;

import java.util.*;
import java.util.function.Function;

/**
 * RecipeRegistry provides recipe registry capabilities within the craft subsystem.
 * It supports recipe composition, ingredient alternatives, and deterministic crafting resolution used by gameplay systems.
 * The registry maintains canonical lookup structures and resolves identifiers to runtime instances.
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
        registerFoodRecipes();
        registerBlocksRecipes();
        registerSmeltingRecipes();
        registerMaterialRecipes();

        create().result(new Material(MaterialID.STICK), 4)
                .with(BlockData.fromIdTo(BlockData.OAK_PLANK.getId()), 2)
                .or(BlockData.fromIdTo(BlockData.SPRUCE_PLANK.getId()), 2).add();

        create().result(new Material(MaterialID.CHARCOAL), 1)
                .with(new Voxel(BlockData.OAK_LOG), 6)
                .or(new Voxel(BlockData.SPRUCE_LOG), 6)
                .with(new Voxel(BlockData.DIRT), 2).add();

        create().result(new Voxel(BlockData.TORCH), 4)
                .with(new Material(MaterialID.STICK), 1)
                .with(new Material(MaterialID.CHARCOAL), 2).add();

        create().result(new Book(false), 1)
                .with(MaterialID.LEATHER, 3)
                .with(MaterialID.PAPER, 2).add();

        create().result(new Backpack(), 1)
                .with(MaterialID.LEATHER, 3).add();

        create().result(new Wallet(), 1)
                .with(MaterialID.LEATHER, 2).add();

        create().result(new Bucket(), 1)
                .with(new MiningComponent(Tier.STEEL, MaterialID.INGOT), 3).add();

        registerToolSet(BlockData.fromIdTo(BlockData.OAK_PLANK.getId()));
        registerToolSet(BlockData.fromIdTo(BlockData.STONE.getId()));
        registerShieldSet();
        registerArmorSets();

        /* Required tier, Crafted Tier */
        Map<Tier, Tier> metalProgression = Map.of(
                Tier.STONE, Tier.WOODEN,
                Tier.COPPER, Tier.STONE,
                Tier.IRON, Tier.COPPER,
                Tier.STEEL, Tier.IRON,
                Tier.GOLDEN, Tier.STEEL,
                Tier.PLATINUM, Tier.GOLDEN,
                Tier.DIAMOND, Tier.PLATINUM);

        metalProgression.forEach((toolTier, requiredStationTier) -> {
            if (toolTier.isInvalidTier() || toolTier.equals(Tier.STONE)) return;
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
        registerTool(primaryMat, 4, 2, Axe::new);
        registerTool(primaryMat, 2, 2, Hoe::new);
        registerTool(primaryMat, 1, 2, Shovel::new);
    }

    /** Adds shield set to the corresponding collection or processing queue. */
    private void registerShieldSet() {
        registerShields(Tier.WOODEN, null);
        Tier.forEach(tier -> {
            if (tier.isInvalidTier()) return;
            registerShields(tier, new MiningComponent(tier, MaterialID.INGOT));
        });
    }

    /** Registers every craftable armor tier and its three wearable pieces. */
    private void registerArmorSets() {
        Tier.forEach(tier -> {
            if (tier == Tier.NONE || tier == Tier.WOODEN) return;
            Craftable material = getArmorMaterial(tier);
            registerArmor(material, 5, Helmet::new);
            registerArmor(material, 8, Chestplate::new);
            registerArmor(material, 5, Boots::new);
        });
    }

    /** Resolves the source material represented by an armor tier. */
    private Craftable getArmorMaterial(Tier tier) {
        return switch (tier) {
            case LEATHER -> new Material(MaterialID.LEATHER);
            case STONE -> new Voxel(BlockData.STONE);
            default -> new MiningComponent(tier, MaterialID.INGOT);
        };
    }

    /** Registers one armor recipe using its tier-specific source material. */
    private void registerArmor(Craftable material, int amount,
                               Function<Tier, Item> constructor) {
        create().result(constructor.apply(getTierFromMaterial(material)), 1)
                .with(material, amount)
                .add();
    }

    /**
     * Adds shields to the corresponding collection or processing queue.
     * @param tier the {@link Tier} supplied as {@code tier}
     * @param primaryMat the {@link Craftable} supplied as {@code primaryMat}
     */
    private void registerShields(Tier tier, Craftable primaryMat) {
        RecipeBuilder recipe = create().result(new Shield(tier), 1);
        if (primaryMat != null) recipe.with(primaryMat, 1);

        boolean hasPlanks = false;
        for (BlockData block : BlockData.all()) {
            if (!block.isPlanks()) continue;
            if (hasPlanks) {
                recipe.or(new Voxel(block), 6);
            } else {
                recipe.with(new Voxel(block), 6);
                hasPlanks = true;
            }
        }
        recipe.add();
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
        if (mat instanceof MiningComponent mc) return mc.getTier();
        if (mat instanceof Voxel block && block.getType() == BlockData.STONE) return Tier.STONE;
        if (mat instanceof Material material && material.getMaterialID() == MaterialID.LEATHER) {
            return Tier.LEATHER;
        }
        return Tier.WOODEN;
    }

    private void registerFoodRecipes() {
        create().result(new Food(FoodData.BREAD), 1)
                .with(new Produce(CropType.WHEAT), 3).add();

        create().result(new Food(FoodData.CARROT_CAKE), 1)
                .with(new Produce(CropType.CARROT), 4)
                .with(new Material(MaterialID.SUGAR), 2).add();

        create().result(new Food(FoodData.FRIES), 1)
                .with(new Produce(CropType.POTATO), 4).add();

        create().result(new Food(FoodData.POTATO_CAKE), 1)
                .with(new Produce(CropType.POTATO), 4)
                .with(new Material(MaterialID.SUGAR), 2).add();
    }

    /**
     * Registers the recipes contributed by blocks.
     */
    private void registerBlocksRecipes() {
        registerSpecialBlocks(BlockData.OAK_LOG);
        registerSpecialBlocks(BlockData.OAK_PLANK);
        registerSpecialBlocks(BlockData.SPRUCE_LOG);
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
                    .with(new MiningComponent(tier, MaterialID.RAW_ORE), 1)
                    .with(new Material(MaterialID.CHARCOAL), 2)
                    .add();
        }

        create().result(new Voxel(BlockData.STONE), 1)
                .with(new Voxel(BlockData.COBBLESTONE), 1)
                .with(new Material(MaterialID.CHARCOAL), 1).add();

        create().result(new Voxel(BlockData.GLASS), 4)
                .with(new Voxel(BlockData.SAND), 1)
                .with(new Material(MaterialID.CHARCOAL), 1).add();
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

    /**
     * Adds the {@code primaryMat} to the list and builds with it whatever
     * special blocks they have
     * @param primaryMat {@link BlockData} supplied as {@code primaryMat}
     */
    private void registerSpecialBlocks(BlockData primaryMat) {
        if (primaryMat.isLog()) {
            create().result(new Voxel(BlockData.toPlanks(primaryMat)), 4)
                    .with(BlockData.fromIdTo(primaryMat.getId()),1).add();
            return;
        }

        create().result(new Voxel(BlockData.toSlab(primaryMat, false)), 6)
                .with(new Voxel(primaryMat), 3).add();

        create().result(new Voxel(BlockData.toSlab(primaryMat, true)), 6)
                .with(new Voxel(primaryMat), 3).add();

        create().result(new Voxel(BlockData.toStaircase(primaryMat)), 4)
                .with(new Voxel(primaryMat), 7).add();

        if (!primaryMat.equals(BlockData.STONE)) {
            create().result(new Voxel(BlockData.toDoor(primaryMat)), 2)
                    .with(new Voxel(primaryMat), 6).add();

            create().result(new Voxel(BlockData.toChest(primaryMat)), 1)
                    .with(new Voxel(primaryMat), 8).add();

            create().result(new Voxel(BlockData.toFence(primaryMat)), 4)
                    .with(new Voxel(primaryMat), 6).add();
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
     * Represents the recipe builder component of the kbengine runtime.
     * <p>This type centralizes the state, lifecycle and behavior required by its callers,
     * keeping domain rules together with the data they operate on.
     * <p><b>Responsibilities:</b>
     * <ul>
     *   <li><b>State:</b> Owns the data needed to represent the component consistently.</li>
     *   <li><b>Lifecycle:</b> Exposes the operations used to create, update and release its state.</li>
     *   <li><b>Integration:</b> Coordinates with the surrounding game, rendering or UI systems through its public API.</li>
     * </ul>
     * <p>Callers should use the documented public operations and allow this type to preserve
     * its invariants rather than modifying implementation details directly.
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
         * Adds an alternative for the immediately preceding required ingredient.
         * It does not addEnemy another required ingredient to the recipe.
         * @param craftable the {@link Craftable} supplied as {@code craftable}
         * @param count the {@code int} supplied as {@code count}
         * @return the {@link RecipeBuilder} representing the with result
         */
        public RecipeBuilder or(Craftable craftable, int count) {
            if (ingredients.isEmpty()) {
                throw new IllegalStateException("or(...) requires a preceding with(...)");
            }
            int lastIndex = ingredients.size() - 1;
            Ingredient required = ingredients.get(lastIndex);
            List<Ingredient> alternatives = new ArrayList<>(required.alternatives());
            alternatives.add(new Ingredient(craftable, count));
            ingredients.set(lastIndex, new Ingredient(required.craftable(), required.amount(), alternatives));
            return this;
        }

        /**
         * Adds addEnemy.
         * @return the {@link Recipe} representing the addEnemy result
         */
        public Recipe add() {
            Recipe recipe = new Recipe(result, amount, List.copyOf(ingredients));
            recipes.add(recipe);
            return recipe;
        }
    }
}
