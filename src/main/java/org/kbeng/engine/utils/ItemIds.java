package org.kbeng.engine.utils;

import org.kbeng.rpg.data.Tier;
import org.kbeng.rpg.item.Armor;
import org.kbeng.rpg.item.Block;
import org.kbeng.rpg.item.Bucket;
import org.kbeng.rpg.item.Item;
import org.kbeng.rpg.item.Material;
import org.kbeng.rpg.item.Tool;
import org.kbeng.rpg.item.Usable;
import org.kbeng.rpg.service.Library;

/**
 * ItemIds provides item ids capabilities within the utils subsystem.
 * It centralizes reusable utilities such as constants, localization, settings, and small cross-cutting helpers.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@Utils
public final class ItemIds {
    private ItemIds() {}

    /**
     * Returns the item's stable code identifier.
     * @param item the item to identify
     * @return the non-localized item identifier, or an empty string for null
     */
    public static String of(Item item) {
        return switch (item) {
            case null -> "";
            case Armor armor -> tiered(armor.getTier(), armor.getType().getName());
            case Tool tool -> tiered(tool.getTier(), tool.getType().getName());
            case Material material -> tiered(material.getTier(), material.getMaterialID().getName());
            case Block block -> block.getType().getName();
            case Bucket bucket -> bucket.isFull() ? bucket.getBlockType().getName() + "_bucket" : "bucket";
            case Usable usable -> usable.getUsablesID().getName();
            default -> item.getName();
        };
    }

    /**
     * Adds the item's code identifier as the final tooltip line.
     * @param tooltip the existing tooltip text
     * @param item the item represented by the tooltip
     * @return the completed tooltip text
     */
    public static String appendToTooltip(String tooltip, Item item) {
        if (!Settings.doEnableIds()) return tooltip;
        String id = of(item);
        if (id.isBlank()) return tooltip;
        if (tooltip == null || tooltip.isBlank()) return id;
        return tooltip + "\n" + Library.NAMESPACE_ID + id;
    }

    private static String tiered(Tier tier, String name) {
        return tier != null && tier != Tier.NONE ? tier.getName() + "_" + name : name;
    }
}
