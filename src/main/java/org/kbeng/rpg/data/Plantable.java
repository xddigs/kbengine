package org.kbeng.rpg.data;

import org.kbeng.rpg.item.Item;

/**
 * Contract for inventory items that can be planted into farmable terrain.
 * A {@code Plantable} item exposes the resulting {@link CropType} so placement logic, growth systems,
 * and harvesting behavior can map a held item to its world crop representation.
 */
public interface Plantable extends Item {
    /**
     * Returns the crop type.
     * @return {@code CropType}
     */
    CropType getType();
}
