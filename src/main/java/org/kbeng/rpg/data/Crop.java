package org.kbeng.rpg.data;

import org.kbeng.rpg.item.Block;
import org.kbeng.rpg.wrld.Chunk;
import org.kbeng.rpg.wrld.World;

/**
 * Crop provides crop capabilities within the data subsystem.
 * It represents strongly typed domain data shared between simulation, rendering, input, and persistence boundaries.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Block, inheriting shared behavior while specializing subsystem-specific logic.
 */
@DataClass
public class Crop extends Block {
    private static final int MAX_STACK_HEIGHT = 3;
    private final CropType type;
    private final Block block;
    private final Season season;
    private final int value;
    private final float targetGrowthTime = 8.0f;
    private GrowthStage stage;
    private float currentGrowthTime = 0.0f;
    private boolean wasHarvested;

    /**
     * Creates a new {@code Crop} instance.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @param type the {@link CropType} supplied as {@code type}
     * @param block the {@link Block} supplied as {@code block}
     * @param season the {@link Season} supplied as {@code season}
     */
    public Crop(int x, int y, int z,
                CropType type, Block block, Season season) {
        super(block.getType(), x, y, z);
        this.type = type;
        this.block = block;
        this.season = season;
        this.value = type.getValue();
        this.stage = GrowthStage.SEED;
        this.wasHarvested = false;
    }

    /**
     * Returns the crop type.
     * @return the {@link CropType} representing the crop type
     */
    public CropType getCropType() {
        return type;
    }

    /**
     * Returns the block.
     * @return the {@link Block} representing the block
     */
    public Block getBlock() {
        return block;
    }

    /**
     * Returns the season.
     * @return the {@link Season} representing the season
     */
    public Season getSeason() {
        return season;
    }

    /**
     * Returns the value.
     * @return {@code int}; the value
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns the stage.
     * @return the {@link GrowthStage} representing the stage
     */
    public GrowthStage getStage() {
        return stage;
    }

    /**
     * Checks whether the ready to harvest condition is met.
     * @return {@code true} if ready to harvest; otherwise {@code false}
     */
    public boolean isReadyToHarvest() {
        return stage == GrowthStage.HARVESTABLE;
    }

    /**
     * Calculates the value represented by was harvested from the current state.
     * @return {@code boolean}; the was harvested result
     */
    public boolean wasHarvested() {
        return wasHarvested;
    }

    /**
     * Sets the harvested.
     * @param wasHarvested the {@code boolean} supplied as {@code wasHarvested}
     */
    public void setHarvested(boolean wasHarvested) {
        this.wasHarvested = wasHarvested;
    }

    /**
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     * @param weather the {@link WeatherType} supplied as {@code weather}
     */
    public void update(float delta, WeatherType weather) {
        if (isReadyToHarvest() && !type.equals(CropType.SUGAR_CANE_CROP)) {
            return;
        }

        float soilBonus = block != null && block.hasWater() ? 2.5f : 0.8f;
        float effectiveDelta = delta * weather.getGrowthMultiplier() * soilBonus;
        currentGrowthTime += effectiveDelta;
        float progress = currentGrowthTime / targetGrowthTime;

        if (progress >= 1.0f) {
            stage = GrowthStage.HARVESTABLE;
            growStack();
        } else if (progress >= 0.75f) {
            stage = GrowthStage.MATURE;
        } else if (progress >= 0.50f) {
            stage = GrowthStage.GROWING;
        } else if (progress >= 0.25f) {
            stage = GrowthStage.BUD;
        } else {
            stage = GrowthStage.SEED;
        }
    }

    /**
     * Adds one harvestable segment to a stackable crop column, up to three blocks.
     * A segment is never created before the crop that triggered the growth is harvestable.
     * @return the {@link Crop} representing the new segment, or {@code null} when the column cannot grow
     */
    public Crop growStack() {
        if (!type.isStackable() || !isReadyToHarvest()) {
            return null;
        }

        World world = World.wrld;
        int baseY = getY();
        while (baseY > 0) {
            Crop below = world.getCropAt(getX(), baseY - 1, getZ());
            if (below == null || below.getCropType() != type) break;
            baseY--;
        }

        int topY = baseY;
        while (world.getCropAt(getX(), topY + 1, getZ()) instanceof Crop above
                && above.getCropType() == type) {
            topY++;
        }

        int height = topY - baseY + 1;
        int newY = topY + 1;
        if (height >= MAX_STACK_HEIGHT || newY >= Chunk.SIZE_Y
                || world.getBlockTypeAt(getX(), newY, getZ()) != BlockData.AIR.getId()) {
            return null;
        }

        Crop segment = new Crop(getX(), newY, getZ(), type, block, season);
        segment.stage = GrowthStage.HARVESTABLE;
        segment.currentGrowthTime = targetGrowthTime;
        world.addCrop(segment);
        return segment;
    }
}
