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
        super(Usables.BACKPACK);
    }

    /** {@inheritDoc} */
    @Override
    public boolean use(GameMaster gameMaster,  boolean isCtrlHeld) {
        this.equip();
        if (!gameMaster.isChatOpen()) {
            gameMaster.toggleInventory();
        }
        return false;
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
