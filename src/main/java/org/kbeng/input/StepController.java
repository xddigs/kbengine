package org.kbeng.input;

import org.kbeng.data.Singleton;
import org.kbeng.item.Block;
import org.kbeng.entity.Player;
import org.kbeng.data.SoundGroup;
import org.kbeng.service.SoundService;
import org.kbeng.wrld.GameMaster;
import org.kbeng.wrld.World;
import org.joml.Vector3f;

/**
 * Represents the step controller component of the Isofarm runtime.
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
