package org.kbeng.rpg.data;

/**
 * SoundGroup declares the canonical sound group set for the data subsystem.
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public enum SoundGroup {
    SOIL(new String[]{"fx/steps/steps_grass_01.ogg", "fx/steps/steps_grass_02.ogg", "fx/steps/steps_grass_03.ogg", "fx/steps/steps_grass_04.ogg"}, new String[]{"fx/blocks/dig.ogg", "fx/blocks/dirt.ogg"}, new String[]{"fx/blocks/digging.ogg"}, new String[]{"fx/blocks/dirt.ogg"}, new String[]{}, new String[]{}, new String[]{"fx/blocks/dirt.ogg"}, new String[]{}, new String[]{}),
    SNOW(new String[]{"fx/steps/steps_snow_01.ogg", "fx/steps/steps_snow_02.ogg", "fx/steps/steps_snow_03.ogg", "fx/steps/steps_snow_04.ogg"}, new String[]{"fx/blocks/snow_break.ogg"}, new String[]{"fx/blocks/snow_tumbling.ogg"}, new String[]{"fx/blocks/snow_place.ogg"}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}),
    STONE(new String[]{}, new String[]{"fx/blocks/mining.ogg"}, new String[]{"fx/blocks/stone_tumbling.ogg"},new String[]{}, new String[]{}, new String[]{}, new String[]{"fx/blocks/mining.ogg"}, new String[]{}, new String[]{}),
    WOOD(new String[]{"fx/steps/steps_wood_01.ogg", "fx/steps/steps_wood_02.ogg", "fx/steps/steps_wood_03.ogg", "fx/steps/steps_wood_04.ogg"}, new String[]{"fx/blocks/wood.ogg"}, new String[]{"fx/blocks/wood_tumbling.ogg"}, new String[]{"fx/blocks/wood.ogg"}, new String[]{},new String[]{}, new String[]{}, new String[]{}, new String[]{}),
    GLASS(new String[]{}, new String[]{"fx/blocks/fragile_break.ogg", "fx/blocks/fragile_break2.ogg"}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}),
    ITEMS(new String[]{}, new String[]{"fx/items/broke.ogg"}, new String[]{}, new String[]{}, new String[]{"fx/items/drop.ogg", "fx/items/pickup.ogg"}, new String[]{}, new String[]{}, new String[]{"fx/items/equip.ogg"}, new String[]{}),
    BOOKS(new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{"fx/items/page.ogg"}, new String[]{}),
    ENTITY(new String[]{}, new String[]{"fx/entity/fall.ogg"}, new String[]{}, new String[]{}, new String[]{"fx/entity/hurt.ogg", "fx/entity/hit.ogg"}, new String[]{}, new String[]{}, new String[]{}, new String[]{}),
    RAIN(new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{"fx/ambient/rain.ogg"}, new String[]{}, new String[]{}, new String[]{}),
    NATURE(new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{"fx/ambient/nature.ogg"}, new String[]{}, new String[]{}, new String[]{}),
    FOOD(new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{"fx/entity/eat_01.ogg", "fx/entity/eat_02.ogg"}, new String[]{}),
    CHEST(new String[]{}, new String[]{"fx/blocks/wood.ogg"}, new String[]{"fx/blocks/wood_tumbling.ogg"}, new String[]{"fx/blocks/wood.ogg"}, new String[]{}, new String[]{}, new String[]{}, new String[]{"fx/blocks/chest_open.ogg", "fx/blocks/chest_close.ogg"}, new String[]{}),
    DOOR(new String[]{}, new String[]{"fx/blocks/wood.ogg"}, new String[]{"fx/blocks/wood_tumbling.ogg"}, new String[]{"fx/blocks/wood.ogg"}, new String[]{}, new String[]{}, new String[]{}, new String[]{"fx/blocks/chest_open.ogg", "fx/blocks/chest_close.ogg"}, new String[]{}),
    CASH(new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{"fx/ui/cash.ogg"}, new String[]{}),
    BUTTON(new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{"fx/ui/button.ogg"}, new String[]{}),
    WATER(new String[]{"fx/steps/steps_water_01.ogg", "fx/steps/steps_water_02.ogg", "fx/steps/steps_water_03.ogg", "fx/steps/steps_water_04.ogg"}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}),
    LAVA(new String[]{"fx/steps/steps_lava_01.ogg", "fx/steps/steps_lava_02.ogg"}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{"fx/ambient/lava.ogg"}, new String[]{}, new String[]{}, new String[]{}),
    SILENT(new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{}, new String[]{});

    private final String[] stepSounds;
    private final String[] breakSounds;
    private final String[] breakingSounds;
    private final String[] placeSounds;
    private final String[] entitySounds;
    private final String[] backgroundSounds;
    private final String[] loopingSounds;
    private final String[] useSounds;
    private final String[] genderSounds;

    /**
     * Creates a new {@code SoundGroup} instance.
     * @param stepSounds       an array of {@link String} values supplied as {@code stepSounds}
     * @param breakSounds      an array of {@link String} values supplied as {@code breakSounds}
     * @param breakingSounds   an array of {@link String} values supplied as {@code breakingSounds}
     * @param placeSounds      an array of {@link String} values supplied as {@code placeSounds}
     * @param entitySounds     an array of {@link String} values supplied as {@code entitySounds}
     * @param backgroundSounds an array of {@link String} values supplied as {@code backgroundSounds}
     * @param loopingSounds    an array of {@link String} values supplied as {@code loopingSounds}
     * @param useSounds        an array of {@link String} values supplied as {@code useSounds}
     * @param genderSounds    an array of gender-specific voice effects
     */
    SoundGroup(String[] stepSounds, String[] breakSounds, String[] breakingSounds, String[] placeSounds,
               String[] entitySounds, String[] backgroundSounds, String[] loopingSounds, String[] useSounds, String[] genderSounds) {
        this.stepSounds = stepSounds;
        this.breakSounds = breakSounds;
        this.breakingSounds = breakingSounds;
        this.placeSounds = placeSounds;
        this.entitySounds = entitySounds;
        this.backgroundSounds = backgroundSounds;
        this.loopingSounds = loopingSounds;
        this.useSounds = useSounds;
        this.genderSounds = genderSounds;
    }

    /**
     * Returns the step sounds.
     * @return an array of {@link String} values; the step sounds
     */
    public String[] getStepSounds() {
        return stepSounds;
    }

    /**
     * Returns the break sounds.
     * @return an array of {@link String} values; the break sounds
     */
    public String[] getBreakSounds() {
        return breakSounds;
    }

    /**
     * Returns the breaking sounds.
     * @return an array of {@link String} values; the breaking sounds
     */
    public String[] getBreakingSounds() {
        return breakingSounds;
    }

    /**
     * Returns the place sounds.
     * @return an array of {@link String} values; the place sounds
     */
    public String[] getPlaceSounds() {
        return placeSounds;
    }

    /**
     * Returns the entity sounds.
     * @return an array of {@link String} values; the entity sounds
     */
    public String[] getEntitySounds() {
        return entitySounds;
    }

    /**
     * Returns the background sounds.
     * @return an array of {@link String} values; the background sounds
     */
    public String[] getBackgroundSounds() {
        return backgroundSounds;
    }

    /**
     * Returns the looping sounds.
     * @return an array of {@link String} values; the looping sounds
     */
    public String[] getLoopingSounds() {
        return loopingSounds;
    }

    /**
     * Returns the use sounds.
     * @return an array of {@link String} values; the use sounds
     */
    public String[] getUseSounds() {
        return useSounds;
    }

    /**
     * Returns {@code genderSounds}
     * @return {@link String[]} value of genderSounds
     */
    public String[] getGenderSounds() {
        return genderSounds;
    }
}
