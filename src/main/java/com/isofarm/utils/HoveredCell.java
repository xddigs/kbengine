package com.isofarm.utils;

import com.isofarm.data.BlockPos;
import com.isofarm.entity.Player;
import com.isofarm.input.Mouse;
import com.isofarm.wrld.GameMaster;

/**
 * Encapsulates the state and operations required by hovered cell within the game runtime.
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
