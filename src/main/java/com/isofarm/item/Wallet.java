package com.isofarm.item;

import com.isofarm.data.DataClass;
import com.isofarm.data.Enchantment;
import com.isofarm.data.Task;
import com.isofarm.data.Usables;
import com.isofarm.wrld.GameMaster;

/**
 * Encapsulates the state and operations required by wallet within the game runtime.
 */
@DataClass
@Task(reason="Wallets are not implemented yet")
public class Wallet extends Usable implements Craftable,
        Equippable {
    private Integer coins;

    /** {@inheritDoc} */
    public Wallet() {
        super(Usables.WALLET);
        this.coins = 0;
    }

    /**
     * Handles use and applies its effect to the current interaction state.
     *
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param isCtrlHeld the {@code boolean} supplied as {@code isCtrlHeld}
     * @return {@code boolean}; the use result
     */
    @Override
    public boolean use(GameMaster gameMaster, boolean isCtrlHeld) {
        return false;
    }

    /**
     * Updates the current state.
     */
    @Override
    public void update() {

    }

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

    /**
     * Returns the {@code coins} value
     * @return {@link Integer} value of coins
     */
    public Integer getCoins() {
        return coins;
    }

    /**
     * Sets the coins value
     * @return {@link Integer} value of coins
     */
    public Wallet setCoins(Integer coins) {
        this.coins = coins;
        return this;
    }
}
