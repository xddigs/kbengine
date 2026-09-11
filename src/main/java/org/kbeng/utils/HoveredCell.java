package org.kbeng.utils;

import org.kbeng.data.BlockPos;
import org.kbeng.entity.Player;
import org.kbeng.input.Mouse;
import org.kbeng.wrld.GameMaster;

/**
 * Represents the hovered cell component of the Isofarm runtime.
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
@Utils
public class HoveredCell {

    /**
     * Returns get.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param isShiftHeld the {@code boolean} supplied as {@code isShiftHeld}
     * @return the {@link BlockPos} representing the get result
     */
    public static BlockPos get(GameMaster gameMaster, boolean isShiftHeld) {
        return gameMaster.getCamera().highlight(gameMaster.getWorld(),
                Player.plyr.getPosition(), Mouse.getX(), Mouse.getY(),
                gameMaster.getWindowWidth(),
                gameMaster.getWindowHeight(),
                isShiftHeld);
    }

    /**
     * Returns get.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @return the {@link BlockPos} representing the get result
     */
    public static BlockPos get(GameMaster gameMaster) {
        return get(gameMaster, false);
    }
}
