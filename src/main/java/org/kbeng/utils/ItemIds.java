package org.kbeng.utils;

import org.kbeng.data.Tier;
import org.kbeng.item.Armor;
import org.kbeng.item.Block;
import org.kbeng.item.Bucket;
import org.kbeng.item.Item;
import org.kbeng.item.Material;
import org.kbeng.item.Tool;
import org.kbeng.item.Usable;
import org.kbeng.service.Library;

/** Provides the stable, non-localized identifier shown in item tooltips. */
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
