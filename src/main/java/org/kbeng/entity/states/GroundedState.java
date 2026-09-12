package org.kbeng.entity.states;

import org.kbeng.data.PlayerState;
import org.kbeng.entity.Player;
import org.kbeng.input.ControlAction;
import org.kbeng.input.Controls;
import org.kbeng.wrld.GameMaster;

/**
 * GroundedState provides grounded state capabilities within the entity subsystem.
 * It participates in actor simulation, state transitions, and per-frame world interaction contracts.
 * The state object captures one branch in a larger state machine and enforces transition rules.
 * It implements PlayerState, providing a concrete strategy for this subsystem contract.
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
