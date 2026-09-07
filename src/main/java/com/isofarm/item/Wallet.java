package com.isofarm.item;

import com.isofarm.data.DataClass;
import com.isofarm.data.Enchantment;
import com.isofarm.data.Usables;
import com.isofarm.entity.Player;
import com.isofarm.ui.GameUIService;
import com.isofarm.utils.ToastFactory;
import com.isofarm.wrld.GameMaster;

/**
 * Encapsulates the state and operations required by wallet within the game runtime.
 */
@DataClass
public class Wallet extends Usable
        implements Craftable, Equippable {
    private Integer coins;

    /** {@inheritDoc} */
    public Wallet() {
        super(Usables.WALLET);
        this.coins = 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean use(GameMaster gameMaster, boolean isCtrlHeld) {
        if (isCtrlHeld) {
            if (isEquipped()) {
                return unequip();
            }
            if (equip()) {
                if (GameUIService.ui != null) {
                    GameUIService.ui.resetHotbarPosition();
                }
                return true;
            }
            return false;
        }
        ToastFactory.info("$" + coins.toString());
        return true;
    }

    /**
     * Updates the current state.
     */
    @Override
    public void update() {}

    /** {@inheritDoc} */
    @Override
    public boolean enchanting(Enchantment enchantment) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Item copy() {
        return new Wallet().earn(coins);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equip() {
        if (Player.plyr == null || isEquipped()) return false;
        Player.plyr.getInventory().equipWallet(this);
        return isEquipped();
    }

    /** {@inheritDoc} */
    @Override
    public boolean unequip() {
        if (Player.plyr == null || !isEquipped()) return false;
        Player.plyr.getInventory().unequipWallet();
        boolean unequipped = !isEquipped();
        if (unequipped && GameUIService.ui != null) {
            GameUIService.ui.resetHotbarPosition();
        }
        return unequipped;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEquipped() {
        return Player.plyr != null
                && Player.plyr.getInventory().getWallet() == this;
    }

    /**
     * Returns the {@code coins} value
     * @return {@link Integer} value of coins
     */
    public Integer coins() {
        return coins;
    }

    /**
     * Adds coins to the wallet
     * @param coins the {@link Integer} value to add
     * @return {@link Wallet}
     */
    public Wallet earn(Integer coins) {
        if (coins != null && coins > 0) {
            this.coins += coins;
        }
        return this;
    }

    /**
     * Spend coins from the wallet
     * @param coins the {@link Integer} value to spend
     * @return {@link Wallet}
     */
    public Wallet spend(Integer coins) {
        if (coins != null && coins > 0) {
            this.coins = Math.max(0, this.coins - coins);
        }
        return this;
    }

    /**
     * Empties the wallet
     */
    public void empty() {
        this.coins = 0;
    }
}
