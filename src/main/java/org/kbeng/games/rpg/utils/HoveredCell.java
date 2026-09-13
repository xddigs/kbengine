package org.kbeng.games.rpg.utils;

import org.kbeng.engine.utils.Utils;

import org.kbeng.games.rpg.data.BlockPos;
import org.kbeng.games.rpg.entity.Player;
import org.kbeng.engine.input.Mouse;
import org.kbeng.games.rpg.wrld.GameMaster;

/**
 * HoveredCell provides hovered cell capabilities within the utils subsystem.
 * It centralizes reusable utilities such as constants, localization, settings, and small cross-cutting helpers.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
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
                Player.plyr.getPosition(), gameMaster.getActiveCamera(),
                gameMaster.getAimScreenX(), gameMaster.getAimScreenY(),
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
