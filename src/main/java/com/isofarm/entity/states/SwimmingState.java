package com.isofarm.entity.states;

import com.isofarm.data.PlayerState;
import com.isofarm.entity.Player;
import com.isofarm.input.ControlAction;
import com.isofarm.input.Controls;
import com.isofarm.wrld.GameMaster;
import com.isofarm.wrld.World;

/**
 * Represents the swimming state component of the Isofarm runtime.
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
