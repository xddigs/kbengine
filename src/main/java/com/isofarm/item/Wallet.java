package com.isofarm.item;

import com.isofarm.data.DataClass;
import com.isofarm.data.Enchantment;
import com.isofarm.data.Task;
import com.isofarm.data.Usables;
import com.isofarm.utils.ToastFactory;
import com.isofarm.wrld.GameMaster;

/**
 * Encapsulates the state and operations required by wallet within the game runtime.
 */
@DataClass
@Task(reason="Wallets are not implemented yet")
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
        return new Wallet();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equip() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean unequip() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEquipped() {
        return false;
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
        this.coins += coins;
        return this;
    }

    /**
     * Spend coins from the wallet
     * @param coins the {@link Integer} value to spend
     * @return {@link Wallet}
     */
    public Wallet spend(Integer coins) {
        this.coins -= coins;
        return this;
    }

    /**
     * Empties the wallet
     */
    public void empty() {
        this.coins = 0;
    }
}
