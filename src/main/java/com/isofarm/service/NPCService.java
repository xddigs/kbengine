package com.isofarm.service;

import com.isofarm.data.Job;
import com.isofarm.data.BlockPos;
import com.isofarm.data.BlockShape;
import com.isofarm.data.NPCGender;
import com.isofarm.data.Ray;
import com.isofarm.data.Singleton;
import com.isofarm.entity.NPC;
import com.isofarm.entity.Player;
import com.isofarm.input.Mouse;
import com.isofarm.item.iBlock;
import com.isofarm.ui.GameUIService;
import com.isofarm.utils.Settings;
import com.isofarm.wrld.GameMaster;
import org.joml.Vector3f;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents the NPC service component of the {@link NPC}'s
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
@SuppressWarnings("all")
@Singleton
public class NPCService implements Service<NPC> {
    public static final NPCService npcs = new NPCService();
    private final List<NPC> npcsList = new LinkedList<>();

    /** Creates a new {@code NPCService} instance. */
    private NPCService() {}

    /**
     * Initializes the NPCService and creates the NPCs.
     */
    public void init() {
        clear();
        NPCGender[] genders = NPCGender.values();
        NPCGender gender = genders[(int) (Math.random() * genders.length)];
        add(new NPC(gender, Job.TRADER));
    }

    /**
     * Places all registered NPCs on terrain after world generation completes.
     * Their positions are distributed around spawn deterministically by list order.
     */
    public void spawn() {
        for (int index = 0; index < npcsList.size(); index++) {
            float angle = (float) (index * Math.PI * 2.0 / Math.max(1, npcsList.size()));
            float radius = 3.0f + index % 3;
            float x = 0.5f + (float) Math.sin(angle) * radius;
            float z = 0.5f + (float) Math.cos(angle) * radius;
            float y = GameMaster.game.getWorld().getHighestY(x, z).y() + 1.0f;
            npcsList.get(index).setHome(new Vector3f(x, y, z));
        }
    }

    /**
     * Interacts with the closest NPC under the pointer when it lies within the
     * same reach used for block placement and breaking.
     *
     * @param gameMaster the active game and camera owner
     * @return {@code true} when an NPC consumed the click
     */
    public boolean interact(GameMaster gameMaster) {
        NPC closest = getClosest();
        return interact(closest);
    }

    /** Interacts with the closest NPC that is not occluded by the targeted block. */
    public boolean interact(GameMaster gameMaster, BlockPos blockTarget) {
        NPC closest = getClosestBeforeBlock(gameMaster, blockTarget);
        return interact(closest);
    }

    private boolean interact(NPC closest) {
        if (closest == null) return false;
        closest.interactWith(Player.plyr);
        closest.speak();
        if (closest.getJob() == Job.TRADER && GameUIService.ui != null) {
            GameUIService.ui.getInventoryUI().openExternalInventory(closest.getInventory());
        }
        return true;
    }

    /**
     * Attacks with the closest NPC under the pointer when it lies within the
     * same reach used for block placement and breaking.
     * @param gameMaster the active game and camera owner
     * @return {@code true} when an NPC consumed the click
     */
    public boolean attack(GameMaster gameMaster) {
        NPC closest = getClosest();
        return attack(closest);
    }

    /** Attacks the closest NPC only when it is in front of the targeted block. */
    public boolean attack(GameMaster gameMaster, BlockPos blockTarget) {
        NPC closest = getClosestBeforeBlock(gameMaster, blockTarget);
        return attack(closest);
    }

    private boolean attack(NPC closest) {
        if (closest == null) return false;
        closest.damage(Player.plyr.getAttack(), Player.plyr);
        return true;
    }

    /**
     * Returns the closest {@link NPC} to the {@link Player}.
     * @return the closest {@link NPC}
     */
    public NPC getClosest() {
        GameMaster gameMaster = GameMaster.game;
        if (gameMaster == null || Player.plyr == null) return null;
        return getClosestBeforeBlock(gameMaster, null);
    }

    /**
     * Returns the living NPC nearest to the player, independently of the cursor.
     * @return the nearest living NPC, or {@code null} when none is registered
     */
    public NPC getNearestToPlayer() {
        if (Player.plyr == null) return null;
        NPC nearest = null;
        float nearestDistanceSquared = Float.POSITIVE_INFINITY;
        for (NPC npc : npcsList) {
            if (!npc.isAlive()) continue;
            float distanceSquared = Player.plyr.getPosition().distanceSquared(npc.getPosition());
            if (distanceSquared < nearestDistanceSquared) {
                nearest = npc;
                nearestDistanceSquared = distanceSquared;
            }
        }
        return nearest;
    }

    /** Returns whether an NPC is still managed and can remain focus-locked. */
    public boolean contains(NPC npc) {
        return npc != null && npcsList.contains(npc);
    }

    /**
     * Returns the closest NPC hit by the cursor, provided it is closer than the
     * block currently hit by that same cursor ray.
     */
    public NPC getClosestBeforeBlock(GameMaster gameMaster, BlockPos blockTarget) {
        if (gameMaster == null || Player.plyr == null) return null;
        Ray ray = gameMaster.getCamera().getMouseRay(Mouse.getX(), Mouse.getY(),
                gameMaster.getWindowWidth(), gameMaster.getWindowHeight());
        float blockRayDistance = getBlockRayDistance(gameMaster, blockTarget, ray);
        NPC closest = null;
        float closestRayDistance = Float.POSITIVE_INFINITY;
        for (NPC npc : npcsList) {
            if (!npc.isAlive()) continue;
            float playerDistance = Player.plyr.getPosition().distance(npc.getPosition());
            if (playerDistance > Settings.getMaxInteractionDistance()) continue;
            float rayDistance = npc.rayIntersection(ray.origin(), ray.direction());
            if (rayDistance < closestRayDistance && rayDistance < blockRayDistance) {
                closest = npc;
                closestRayDistance = rayDistance;
            }
        }
        return closest;
    }

    private float getBlockRayDistance(GameMaster gameMaster, BlockPos blockTarget, Ray ray) {
        if (blockTarget == null) return Float.POSITIVE_INFINITY;
        iBlock interactiveBlock = gameMaster.getWorld().getInteractiveBlockAt(
                blockTarget.x(), blockTarget.y(), blockTarget.z());
        if (interactiveBlock != null && interactiveBlock.getType().isDoor()) {
            return interactiveBlock.rayIntersection(ray.origin(), ray.direction());
        }
        BlockShape shape = gameMaster.getWorld().getBlockShapeAt(
                blockTarget.x(), blockTarget.y(), blockTarget.z());
        if (shape == null) shape = BlockShape.FULL_CUBE;
        BlockShape.RayHit hit = shape.raycast(ray.origin(), ray.direction(),
                blockTarget.x(), blockTarget.y(), blockTarget.z());
        return hit == null ? Float.POSITIVE_INFINITY : hit.distance();
    }

    /**
     * Adds an NPC to the managed list and world.
     * @param npc the NPC to addEnemy
     * @return the added NPC
     */
    public NPC add(NPC npc) {
        npcsList.add(npc);
        GameMaster.game.addEntity(npc);
        return npc;
    }

    /**
     * Removes an NPC from the managed list and world.
     * @param npc the NPC to removeEnemy
     * @return the removed NPC
     */
    public NPC remove(NPC npc) {
        npcsList.remove(npc);
        GameMaster.game.removeEntity(npc);
        return npc;
    }

    /**
     * Removes every managed NPC from the list and world.
     */
    public void clear() {
        for (NPC npc : List.copyOf(npcsList)) {
            GameMaster.game.removeEntity(npc);
        }
        npcsList.clear();
    }

    /**
     * Returns the managed NPC list.
     * @return the managed NPCs
     */
    public List<NPC> getNpcs() {
        return npcsList;
    }

    /** Returns the first trader NPC, or {@code null} when none is registered. */
    public NPC getTrader() {
        for (NPC npc : npcsList) {
            if (npc.getJob() == Job.TRADER) return npc;
        }
        return null;
    }
}
