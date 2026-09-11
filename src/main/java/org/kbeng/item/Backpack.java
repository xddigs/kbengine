package org.kbeng.item;

import org.kbeng.data.Enchantment;
import org.kbeng.data.Usables;
import org.kbeng.entity.Player;
import org.kbeng.ui.GameUIService;
import org.kbeng.wrld.GameMaster;

/**
 * Represents the backpack component of the Isofarm runtime.
 *
 * <p>This type centralizes the state, lifecycle and behavior required by its callers,
 * keeping domain rules together with the data they operate on.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li><b>State:</b> Owns the data needed to represent the component consistently.</li>
 *   <li><b>Lifecycle:</b> Exposes the operations used to create, update and release its state.</li>
 *   <li><b>Integration:</b> Coordinates with the surrounding game, rendering or UI systems through its public API.</li>
 * </ul>
 *
 * <p>Callers should use the documented public operations and allow this type to preserve
 * its invariants rather than modifying implementation details directly.
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
