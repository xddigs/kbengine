package com.isofarm.utils;

import com.isofarm.data.Tier;
import com.isofarm.item.Armor;
import com.isofarm.item.Block;
import com.isofarm.item.Bucket;
import com.isofarm.item.Item;
import com.isofarm.item.Material;
import com.isofarm.item.Tool;
import com.isofarm.item.Usable;

/** Provides the stable, non-localized identifier shown in item tooltips. */
public final class ItemIds {
    private ItemIds() {}

    /**
     * Returns the item's stable code identifier.
     * @param item the item to identify
     * @return the non-localized item identifier, or an empty string for null
     */
    public static String of(Item item) {
        if (item == null) return "";

        if (item instanceof Armor armor) {
            return tiered(armor.getTier(), armor.getType().getName());
        }
        if (item instanceof Tool tool) {
            return tiered(tool.getTier(), tool.getType().getName());
        }
        if (item instanceof Material material) {
            return tiered(material.getTier(), material.getMaterialID().getName());
        }
        if (item instanceof Block block) {
            return block.getType().getName();
        }
        if (item instanceof Bucket bucket) {
            return bucket.isFull()
                    ? bucket.getBlockType().getName() + "_bucket"
                    : "bucket";
        }
        if (item instanceof Usable usable) {
            return usable.getUsablesID().getName();
        }
        return item.getName();
    }

    /**
     * Adds the item's code identifier as the final tooltip line.
     * @param tooltip the existing tooltip text
     * @param item the item represented by the tooltip
     * @return the completed tooltip text
     */
    public static String appendToTooltip(String tooltip, Item item) {
        String id = of(item);
        if (id.isBlank()) return tooltip;
        if (tooltip == null || tooltip.isBlank()) return id;
        return tooltip + "\n" + id;
    }

    private static String tiered(Tier tier, String name) {
        return tier != null && tier != Tier.NONE
                ? tier.getName() + "_" + name
                : name;
    }
}
