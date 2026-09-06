package com.isofarm.item;

import com.isofarm.data.Enchantment;
import com.isofarm.data.Usables;
import com.isofarm.entity.Player;
import com.isofarm.ui.GameUIService;
import com.isofarm.utils.Local;
import com.isofarm.wrld.GameMaster;

/**
 * Encapsulates the state and operations required by backpack within the game runtime.
 */
public class Backpack extends Usable implements Equippable,
        Undroppable {

    /**
     * Creates a new {@code Backpack} instance.
     */
    public Backpack() {
        super(Usables.BACKPACK, Local.lang.t("item.usable.backpack"));
    }

    /**
     * {@inheritDoc}
     * Handles use and applies its effect to the current interaction state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param isCtrlHeld the {@code boolean} supplied as {@code isCtrlHeld}
     * @return {@code boolean}; the use result
     */
    @Override
    public boolean use(GameMaster gameMaster,  boolean isCtrlHeld) {
        Player player = Player.plyr;
        if (!player.getInventory().hasBackpackEquipped()) {
            player.getInventory().equipBackpack(this);
            GameUIService.ui.resetHotbarPosition();
            return true;
        }

        if (!gameMaster.isChatOpen()) {
            gameMaster.toggleInventory();
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * Updates the current state.
     */
    @Override
    public void update() {}

    /**
     * Applies unequip and updates the affected character or item state.
     */
    public void unequip() {
        if (Player.plyr.getInventory().hasBackpackEquipped()) {
            Player.plyr.getInventory().unequipBackpack();
            GameUIService.ui.resetHotbarPosition();
        }
    }


    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Backpack();
    }

    /**
     * {@inheritDoc}
     * Applies enchanting and updates the affected character or item state.
     * @param enchantment the {@link Enchantment} supplied as {@code enchantment}
     * @return {@code boolean}; the enchanting result
     */
    @Override
    public boolean enchanting(Enchantment enchantment) {
        return false;
    }
}
