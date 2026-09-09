package com.isofarm.entity.states;

import com.isofarm.data.PlayerState;
import com.isofarm.entity.Player;
import com.isofarm.input.ControlAction;
import com.isofarm.input.Controls;
import com.isofarm.wrld.GameMaster;

/**
 * Encapsulates the state and operations required by grounded state within the game runtime.
 */
public class GroundedState implements PlayerState {
    private final Player player = Player.plyr;

    /**
     * {@inheritDoc}
     * Activates this object and prepares any state it requires.
     */
    @Override
    public void enter() {
        player.setTargetEyeHeight(1.6f);
    }

    /**
     * {@inheritDoc}
     * Handles input and applies its effect to the current interaction state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     */
    @Override
    public void input(GameMaster gameMaster) {
        if (gameMaster.isInventoryOpen() || gameMaster.isChatOpen()) {
            return;
        }

        if (Controls.isPressed(ControlAction.JUMP)) {
            player.jump();
        }

        if (Controls.isDown(ControlAction.SNEAK)) {
            player.changeState(new SneakingState());
            return;
        }
    }

    /**
     * {@inheritDoc}
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    @Override
    public void update(float delta) {
        if (player.isInFluid(GameMaster.game.getWorld())) {
            player.changeState(new SwimmingState());
            return;
        }

        if (!player.isOnGround() && !player.isFalling()) {
            player.changeState(new FallingState());
            return;
        }

        float yaw = GameMaster.game
                .getActiveCamera()
                .getYaw();

        player.wasd(delta, yaw, false);
    }

    /**
     * {@inheritDoc}
     * Deactivates this object and releases its transient state.
     */
    @Override
    public void exit() {
    }
}
