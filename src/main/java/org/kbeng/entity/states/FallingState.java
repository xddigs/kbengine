package org.kbeng.entity.states;

import org.kbeng.data.PlayerState;
import org.kbeng.data.Cause;
import org.kbeng.data.SoundGroup;
import org.kbeng.entity.Player;
import org.kbeng.service.SoundService;
import org.kbeng.wrld.GameMaster;
import org.kbeng.wrld.World;

/**
 * FallingState provides falling state capabilities within the entity subsystem.
 * It participates in actor simulation, state transitions, and per-frame world interaction contracts.
 * The state object captures one branch in a larger state machine and enforces transition rules.
 * It implements PlayerState, providing a concrete strategy for this subsystem contract.
 */
public class FallingState implements PlayerState {
    private final Player player = Player.plyr;
    private static final float FALL_DISTANCE = 4.0f;
    private static final float DOUBLE_JUMP_WINDOW = 0.75f;
    private static final float VOID_DAMAGE_DELAY = 10.0f;
    private static final float VOID_DAMAGE_INTERVAL = 1.0f;
    private static final float VOID_DAMAGE_STEP = 0.1f;

    private float fallStartY;
    private float fallTime;
    private float jumpTime;
    private float voidDamageTimer;
    private int voidDamageTicks;

    /**
     * {@inheritDoc}
     * Activates this object and prepares any state it requires.
     */
    @Override
    public void enter() {
        this.fallStartY = player.getPosition().y;
        this.fallTime = 0.0f;
        this.jumpTime = 0.0f;
        this.voidDamageTimer = 0.0f;
        this.voidDamageTicks = 0;

        player.setFalling(true);
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
    }

    /**
     * {@inheritDoc}
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    @Override
    public void update(float delta) {
        fallTime += delta;
        jumpTime += delta;

        if (player.isInFluid(World.wrld)) {
            player.setFalling(false);
            player.changeState(new SwimmingState());
            return;
        }

        float yaw = GameMaster.game.getActiveCamera().getYaw();
        player.wasd(delta, yaw, false);

        if (!player.isAlive()) return;

        if (player.isOnGround()) {
            float fallDistance = fallStartY - player.getPosition().y;
            if (fallDistance > FALL_DISTANCE) {
                float damage = (fallDistance - FALL_DISTANCE) * 2.0f;
                player.fallDamage(damage);
                SoundService.fx.playBreakSound(SoundGroup.ENTITY);
            }

            player.setFalling(false);
            player.changeState(new GroundedState());
        }
    }

    /**
     * Applies increasingly severe damage after falling continuously into the
     * void for ten seconds.
     * @param delta the {@code float} argument; frame time in seconds
     */
    private void applyVoidDamage(float delta) {
        if (fallTime < VOID_DAMAGE_DELAY) return;

        if (voidDamageTicks == 0 && voidDamageTimer == 0.0f) {
            voidDamageTimer = VOID_DAMAGE_INTERVAL;
        } else {
            voidDamageTimer += delta;
        }
        while (voidDamageTimer >= VOID_DAMAGE_INTERVAL && player.isAlive()) {
            voidDamageTimer -= VOID_DAMAGE_INTERVAL;
            voidDamageTicks++;

            float healthFraction = VOID_DAMAGE_STEP * voidDamageTicks;
            float damage = Math.max(1.0f,
                    player.getMaxHitpoints() * healthFraction);
            player.damage(damage, Cause.VOID);
        }
    }

    /**
     * {@inheritDoc}
     * Deactivates this object and releases its transient state.
     */
    @Override
    public void exit() {
        player.setFalling(false);
    }
}
