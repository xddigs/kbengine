package org.kbeng.games.rpg.input;

import org.kbeng.games.rpg.data.Singleton;
import org.kbeng.games.rpg.item.Block;
import org.kbeng.games.rpg.entity.Player;
import org.kbeng.games.rpg.data.SoundGroup;
import org.kbeng.games.rpg.service.SoundService;
import org.kbeng.games.rpg.wrld.GameMaster;
import org.kbeng.games.rpg.wrld.World;
import org.joml.Vector3f;

/**
 * StepController provides step controller capabilities within the input subsystem.
 * It defines device mappings, control-state tracking, and interaction orchestration for player actions.
 * The controller translates user or system signals into deterministic runtime state transitions.
 */
@Singleton
public class StepController {
    public static final StepController step = new StepController();
    private float stepDistanceAccumulator = 0.0f;
    private static final float STEP_DISTANCE_THRESHOLD = 1.6f;
    private final Vector3f lastPosition = new Vector3f();

    /**
     * Updates the current state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param soundService the {@link SoundService} supplied as {@code soundService}
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void update(GameMaster gameMaster, SoundService soundService, float delta) {
        Player player = Player.plyr;
        Vector3f currentPos = player.getPosition();
        float dx = currentPos.x - lastPosition.x;
        float dz = currentPos.z - lastPosition.z;
        float distanceMoved = (float) Math.sqrt(dx * dx + dz * dz);

        lastPosition.set(currentPos);
        if (distanceMoved < 0.001f || distanceMoved > 2.0f) {
            return;
        }

        stepDistanceAccumulator += distanceMoved;

        if (stepDistanceAccumulator >= STEP_DISTANCE_THRESHOLD) {
            stepDistanceAccumulator -= STEP_DISTANCE_THRESHOLD;

            int blockX = (int) Math.floor(currentPos.x);
            int blockY = (int) Math.floor(currentPos.y - 0.5f);
            int blockZ = (int) Math.floor(currentPos.z);

            World world = gameMaster.getWorld();
            Block block = world.getBlockAt(blockX, blockY, blockZ);
            if (block != null && block.getType() != null) {
                SoundGroup soundGroup = block.getType().getSoundGroup();
                soundService.playStepSound(soundGroup);
            }
        }
    }
}
