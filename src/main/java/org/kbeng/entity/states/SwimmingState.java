package org.kbeng.entity.states;

import org.kbeng.data.PlayerState;
import org.kbeng.entity.Player;
import org.kbeng.input.ControlAction;
import org.kbeng.input.Controls;
import org.kbeng.wrld.GameMaster;
import org.kbeng.wrld.World;

/**
 * SwimmingState provides swimming state capabilities within the entity subsystem.
 *
 * It participates in actor simulation, state transitions, and per-frame world interaction contracts.
 *
 * The state object captures one branch in a larger state machine and enforces transition rules.
 *
 * It implements PlayerState, providing a concrete strategy for this subsystem contract.
 */
public class SwimmingState implements PlayerState {
    private final Player player = Player.plyr;

    private static final float SWIM_UP_SPEED = 4.0f;
    private static final float SWIM_DOWN_SPEED = -3.0f;
    private static final float WATER_DRAG = 0.90f;
    private static final float BUOYANCY = 1.2f;

    /**
     * {@inheritDoc}
     * Activates this object and prepares any state it requires.
     */
    @Override
    public void enter() {
        player.setTargetEyeHeight(1.2f);
    }

    /**
     * {@inheritDoc}
     * Handles input and applies its effect to the current interaction state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     */
    @Override
    public void input(GameMaster gameMaster) {
        if (Controls.isDown(ControlAction.SWIM_UP)) {
            player.getVelocity().y = SWIM_UP_SPEED;
        } else if (Controls.isDown(ControlAction.SWIM_DOWN)) {
            player.getVelocity().y = SWIM_DOWN_SPEED;
        }
    }

    /**
     * {@inheritDoc}
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    @Override
    public void update(float delta) {
        float yaw = GameMaster.game.getActiveCamera().getYaw();
        player.wasd(delta, yaw, false);

        if (player.isOnGround()) {
            if (Controls.isDown(ControlAction.SWIM_UP)) {
                player.changeState(new GroundedState());
                return;
            }
        }

        if (!player.isInFluid(World.wrld)) {
            if (player.isOnGround()) {
                player.changeState(new GroundedState());
            } else {
                player.changeState(new FallingState());
            }
        }
    }

    /**
     * {@inheritDoc}
     * Deactivates this object and releases its transient state.
     */
    @Override
    public void exit() {
        player.setTargetEyeHeight(1.6f);
    }
}
