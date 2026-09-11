package org.kbeng.data;

import org.kbeng.utils.Local;

import java.util.Locale;

/**
 * Enumerates the supported enchantments values.
 */
public enum Enchantments {
    EFFICIENCY(0, new ToolType[]{ToolType.HOE, ToolType.AXE, ToolType.PICKAXE, ToolType.SHOVEL}, 3, false),
    DURABILITY(1, new ToolType[]{ToolType.HOE, ToolType.AXE, ToolType.PICKAXE, ToolType.SHOVEL, ToolType.SWORD}, 3, false),
    FORTUNE(2, new ToolType[]{ToolType.HOE, ToolType.AXE, ToolType.PICKAXE, ToolType.SHOVEL}, 3, false),
    SHARPNESS(3, new ToolType[]{ToolType.SWORD, ToolType.AXE}, 3, false),
    HARVEST(4, new ToolType[]{ToolType.HOE}, 2, false),
    SWIFT(5, new ToolType[]{ToolType.HOE, ToolType.AXE, ToolType.PICKAXE, ToolType.SHOVEL, ToolType.SWORD}, 2, false),
    REPAIR(6, new ToolType[]{ToolType.HOE, ToolType.AXE, ToolType.PICKAXE, ToolType.SHOVEL, ToolType.SWORD}, 1, true),
    LUCKY(7, new ToolType[]{ToolType.HOE, ToolType.AXE, ToolType.PICKAXE, ToolType.SHOVEL, ToolType.SWORD}, 1, true);

    private static final Enchantments[] BY_ID;
    static {
        BY_ID = new Enchantments[values().length];
        for (Enchantments enchantment : values()) {
            BY_ID[enchantment.getId()] = enchantment;
        }
    }

    private final int id;
    private final ToolType[] applicableTools;
    private final int maxLevel;
    private final boolean singleLevel;

    /**
     * Creates a new {@code Enchantments} instance.
     * @param id the {@code int} supplied as {@code id}
     * @param applicableTools an array of {@link ToolType} values supplied as {@code applicableTools}
     * @param maxLevel the {@code int} supplied as {@code maxLevel}
     * @param singleLevel the {@code boolean} supplied as {@code singleLevel}
     */
    Enchantments(int id, ToolType[] applicableTools,
                 int maxLevel, boolean singleLevel) {
        this.id = id;
        this.applicableTools = applicableTools;
        this.maxLevel = maxLevel;
        this.singleLevel = singleLevel;
    }

    /**
     * Returns the id.
     * @return {@code int}; the id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the name.
     * @return the {@link String} result; {@code String} the name
     */
    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the display name.
     * @return the {@link String} representing the display name
     */
    public String getDisplayName() {
        return Local.lang.t(getTranslationKey() + ".name");
    }

    /**
     * Returns the localized adjective used to decorate enchanted item names.
     * @return the {@link String} representing the localized adjective
     */
    public String getAdjective() {
        return Local.lang.t(getTranslationKey() + ".adjective");
    }

    /**
     * Returns the description.
     * @return the {@link String} representing the description
     */
    public String getDescription() {
        return Local.lang.t(getTranslationKey() + ".description");
    }

    /**
     * Returns the common localization-key prefix for this enchantment.
     * @return the {@link String} representing the localization-key prefix
     */
    public String getTranslationKey() {
        return "enchantment." + getName();
    }

    /**
     * Returns the applicable tools.
     * @return an array of {@link ToolType} values; the applicable tools
     */
    public ToolType[] getApplicableTools() {
        return applicableTools;
    }

    /**
     * Returns the max level.
     * @return {@code int}; the max level
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Checks whether the single level condition is met.
     * @return {@code true} if single level; otherwise {@code false}
     */
    public boolean isSingleLevel() {
        return singleLevel;
    }

    /**
     * Creates or returns from id from the supplied arguments.
     * @param id the {@code int} supplied as {@code id}
     * @return the {@link Enchantments} representing the from id result
     */
    public static Enchantments fromId(int id) {
        return BY_ID[id];
    }
}
