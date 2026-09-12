package org.kbeng.data;

import org.kbeng.item.Item;
import org.kbeng.entity.Player;
import org.kbeng.service.SoundService;

/**
 * Consumable defines the consumable contract within the data subsystem.
 * Implementations are expected to preserve these behavioral guarantees while choosing their own storage and execution strategy.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Item, inheriting shared behavior while specializing subsystem-specific logic.
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
