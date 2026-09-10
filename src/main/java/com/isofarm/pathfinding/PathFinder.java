package com.isofarm.pathfinding;

import com.isofarm.entity.Player;
import com.isofarm.wrld.World;

import java.util.List;

/**
 * Represents the path finder component of the Isofarm runtime.
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
public final class PathFinder {

    /**
     * Creates a new {@code PathFinder} instance.
     */
    private PathFinder() {}

    /**
     * Finds and returns the path.
     * @param world the {@link World} supplied as {@code world}
     * @param start the {@link GridPos} supplied as {@code start}
     * @param goal the {@link GridPos} supplied as {@code goal}
     * @return the {@link List} representing the located path
     */
    public static List<GridPos> findPath(World world, GridPos start, GridPos goal) {
        return AStar.findPath(world, start, goal);
    }

    /**
     * Returns the player grid position.
     * @return the {@link GridPos} representing the player grid position
     */
    public static GridPos getPlayerGridPosition() {
        Player player = Player.plyr;
        return new GridPos(
                (int) Math.floor(player.getPosition().x),
                (int) Math.floor(player.getPosition().y),
                (int) Math.floor(player.getPosition().z)
        );
    }

    /**
     * Returns the walkable position.
     * @param world the {@link World} supplied as {@code world}
     * @param x the {@code int} supplied as {@code x}
     * @param z the {@code int} supplied as {@code z}
     * @return the {@link GridPos} representing the walkable position
     */
    public static GridPos getWalkablePosition(World world, int x, int z) {
        GridPos highestY = world.getHighestY(
                x + 0.5f,
                z + 0.5f
        );

        int y = highestY.y();
        if (!canStand(world, x, y, z)) {
            return null;
        }

        return new GridPos(x, y, z);
    }

    /**
     * Checks whether the stand condition is met.
     * @param world the {@link World} supplied as {@code world}
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     * @return {@code true} if stand; otherwise {@code false}
     */
    private static boolean canStand(World world, int x, int y, int z) {
        return world.isBlockSolid(x, y - 1, z)
                && !world.isBlockSolid(x, y, z)
                && !world.isBlockSolid(x, y + 1, z);
    }
}
