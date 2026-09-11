package org.kbeng.data;

import org.kbeng.wrld.GameMaster;

/**
 * Defines the player state contract.
 */
public interface PlayerState {
    /**
     * Handles input and applies its effect to the current interaction state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     */
    void input(GameMaster gameMaster);
    /**
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    void update(float delta);
    /**
     * Activates this object and prepares any state it requires.
     */
    void enter();
    /**
     * Deactivates this object and releases its transient state.
     */
    void exit();
}
