package org.kbeng.rpg.item;

import org.kbeng.rpg.data.Enchantment;
import org.kbeng.rpg.data.Usables;
import org.kbeng.rpg.entity.Player;
import org.kbeng.rpg.ui.GameUIService;
import org.kbeng.rpg.wrld.GameMaster;

/**
 * Backpack provides backpack capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Usable and implements Equippable, combining inherited behavior with explicit runtime contracts.
 */
public class Backpack extends Usable implements Equippable,
        Undroppable {

    /**
     * Creates a new {@code Backpack} instance.
     */
    public Backpack() {
        super(Usables.BACKPACK);
    }

    /** {@inheritDoc} */
    @Override
    public boolean use(GameMaster gameMaster,  boolean isCtrlHeld) {
        if (gameMaster == null || gameMaster.isChatOpen() || !isEquipped()) return false;
        gameMaster.setBackpackOpen(!gameMaster.isBackpackOpen());
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void update() {}

    /** {@inheritDoc} */
    @Override
    public boolean equip() {
        if (!Player.plyr.getInventory().hasBackpackEquipped()) {
            Player.plyr.getInventory().equipBackpack(this);
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean unequip() {
        if (Player.plyr.getInventory().hasBackpackEquipped()) {
            Player.plyr.getInventory().unequipBackpack();
            boolean unequipped = !Player.plyr.getInventory().hasBackpackEquipped();
            if (unequipped && GameUIService.ui != null) {
                GameUIService.ui.resetHotbarPosition();
            }
            return unequipped;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEquipped() {
        return Player.plyr.getInventory().hasBackpackEquipped();
    }

    /** {@inheritDoc} */
    @Override
    public Item copy() {
        return new Backpack();
    }

    /** {@inheritDoc} */
    @Override
    public boolean enchanting(Enchantment enchantment) {
        return false;
    }
}
