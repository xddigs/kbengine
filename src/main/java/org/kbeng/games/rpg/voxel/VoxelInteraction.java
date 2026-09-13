package org.kbeng.games.rpg.voxel;

import org.kbeng.engine.utils.Settings;
import org.kbeng.games.rpg.data.BlockData;
import org.kbeng.games.rpg.entity.Entity;
import org.kbeng.games.rpg.entity.Player;
import org.kbeng.games.rpg.entity.WorldItem;
import org.kbeng.games.rpg.item.Item;
import org.kbeng.games.rpg.item.Voxel;
import org.kbeng.games.rpg.utils.HoveredCell;
import org.kbeng.games.rpg.wrld.GameMaster;
import org.joml.Vector3f;

/** Single-cell input transaction. A primary press removes exactly the selected
 * quarter-unit cell immediately; no hardness timer, crack overlay, tool tier or
 * smart-tree expansion participates. A secondary press places one inventory
 * voxel against the actual hit face, rejecting occupied cells and actor bounds. */
public final class VoxelInteraction {
    private VoxelInteraction() { }

    /** Checks reach in physical world units, independently of the orthographic
     * ray origin, which can be hundreds of units from the player. */
    public static boolean reachable(VoxelRaycast.Hit hit) {
        if (hit == null || Player.plyr == null) return false;
        Vector3f p = new Vector3f(Player.plyr.getPosition());
        p.y += Player.plyr.getDimensions().y * 0.5f;
        return hit.center().distance(p) <= Settings.getMaxInteractionDistance();
    }

    public static void edit(GameMaster game, Item item, boolean remove, boolean place) {
        VoxelRaycast.Hit hit = HoveredCell.voxel(game);
        if (!reachable(hit)) return;
        VoxelWorld world = game.getWorld().voxels();
        Player player = Player.plyr;
        if (remove) {
            if (hit.material() == BlockData.VOIDSEAL.getId()) return;
            if (world.set(hit.x(), hit.y(), hit.z(), (byte) 0) && !player.getGamemode().isGodmode()) {
                game.addEntity(new WorldItem(new Voxel(BlockData.fromId(hit.material())), 1, hit.center()));
            }
        } else if (place && item instanceof Voxel voxel && voxel.type() != BlockData.AIR) {
            int x = hit.x() + hit.nx(), y = hit.y() + hit.ny(), z = hit.z() + hit.nz();
            if (hit.nx() == 0 && hit.ny() == 0 && hit.nz() == 0 || world.get(x, y, z) != 0) return;
            if (VoxelPalette.solid(voxel.getId()) && occupiedByActor(game, x, y, z)) return;
            if (world.set(x, y, z, voxel.getId()) && !player.getGamemode().isGodmode()) player.remove(item);
        }
    }

    /** Tests one physical 0.25 cube against unchanged actor dimensions. Dropped
     * inventory sprites do not prevent placing terrain beneath them. */
    public static boolean occupiedByActor(GameMaster game, int x, int y, int z) {
        float ax = VoxelGrid.world(x), ay = VoxelGrid.world(y), az = VoxelGrid.world(z);
        for (Entity e : game.getEntities()) {
            if (e instanceof WorldItem || !e.isAlive()) continue;
            Vector3f p = e.getPosition(), d = e.getDimensions();
            if (p.x - d.x / 2 < ax + .25f && p.x + d.x / 2 > ax
                    && p.y < ay + .25f && p.y + d.y > ay
                    && p.z - d.z / 2 < az + .25f && p.z + d.z / 2 > az) return true;
        }
        return false;
    }
}
