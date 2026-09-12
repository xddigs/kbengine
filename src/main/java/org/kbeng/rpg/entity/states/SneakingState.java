package org.kbeng.rpg.entity.states;

import org.kbeng.rpg.data.PlayerState;
import org.kbeng.rpg.entity.Player;
import org.kbeng.engine.input.ControlAction;
import org.kbeng.engine.input.Controls;
import org.kbeng.rpg.wrld.GameMaster;

/**
 * SneakingState provides sneaking state capabilities within the entity subsystem.
 * It participates in actor simulation, state transitions, and per-frame world interaction contracts.
 * The state object captures one branch in a larger state machine and enforces transition rules.
 * It implements PlayerState, providing a concrete strategy for this subsystem contract.
 */
public class SneakingState implements PlayerState {
    private final Player player = Player.plyr;
    private static final float SNEAK_EYE_HEIGHT = 1.2f;

    /**
     * {@inheritDoc}
     * Activates this object and prepares any state it requires.
     */
    @Override
    public void enter() {
        player.setTargetEyeHeight(SNEAK_EYE_HEIGHT);
        player.setSpeed(player.getSpeed() * 0.5f);
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

        if (Controls.isPressed(ControlAction.PRIMARY_ACTION) ||
                Controls.isPressed(ControlAction.SECONDARY_ACTION)) {
            player.interact();
            return;
        }

        if (!Controls.isDown(ControlAction.SNEAK)) {
            player.changeState(new GroundedState());
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

        float yaw = GameMaster.game.getActiveCamera().getYaw();
        player.wasd(delta, yaw, false);
    }

    /**
     * {@inheritDoc}
     * Deactivates this object and releases its transient state.
     */
    @Override
    public void exit() {
        player.setTargetEyeHeight(1.6f);
        player.setSpeed(player.getSpeed() * 2.0f);
    }
}
