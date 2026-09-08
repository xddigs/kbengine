package com.isofarm.data;

import com.isofarm.item.Item;
import com.isofarm.entity.Player;
import com.isofarm.service.SoundService;

/**
 * Defines the consumable contract, whether an item can be consumed.
 */
@DataClass
public interface Consumable extends Item {
    /**
     * Returns the amount of hunger restored by this item.
     * @return the positive hunger value, or zero when the item is not edible
     */
    float getFoodValue();

    /**
     * Consumes the item
     * @return {@code true} if the item was consumed, {@code false} otherwise
     */
    default boolean consume() {
        Player player = Player.plyr;
        float foodValue = getFoodValue();
        if (player == null || !player.isInSurvival() || foodValue <= 0.0f
                || player.getHunger() >= player.getMaxHunger()
                || player.getInventory().getAmount(this) <= 0) {
            return false;
        }

        player.restoreHunger(foodValue);
        player.remove(this, 1);
        SoundService.fx.playUseSound(SoundGroup.FOOD);
        return true;
    }
}
