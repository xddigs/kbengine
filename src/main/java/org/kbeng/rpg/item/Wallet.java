package org.kbeng.rpg.item;

import org.kbeng.rpg.data.DataClass;
import org.kbeng.rpg.data.Enchantment;
import org.kbeng.rpg.data.Usables;
import org.kbeng.rpg.entity.Player;
import org.kbeng.engine.utils.ToastFactory;
import org.kbeng.rpg.wrld.GameMaster;

/**
 * Wallet provides wallet capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Usable, inheriting shared behavior while specializing subsystem-specific logic.
 */
@DataClass
public class Wallet extends Usable
        implements Craftable {
    private Integer coins;

    /** {@inheritDoc} */
    public Wallet() {
        super(Usables.WALLET);
        this.coins = 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean use(GameMaster gameMaster, boolean isCtrlHeld) {
        if (Player.plyr == null || !Player.plyr.isInBackpack(this)) {
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

    /**
     * Returns the {@code coins} value
     * @return {@link Integer} value of coins
     */
    public Integer coins() {
        return coins;
    }

    /**
     * Adds coins to the wallet
     * @param coins the {@link Integer} value to addEnemy
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
