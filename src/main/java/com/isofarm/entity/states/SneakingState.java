package com.isofarm.entity.states;

import com.isofarm.data.PlayerState;
import com.isofarm.entity.Player;
import com.isofarm.input.ControlAction;
import com.isofarm.input.Controls;
import com.isofarm.wrld.GameMaster;

/**
 * Represents the sneaking state component of the Isofarm runtime.
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
