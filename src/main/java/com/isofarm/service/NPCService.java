package com.isofarm.service;

import com.isofarm.data.Job;
import com.isofarm.data.NPCGender;
import com.isofarm.data.Ray;
import com.isofarm.data.Singleton;
import com.isofarm.entity.NPC;
import com.isofarm.entity.Player;
import com.isofarm.input.Mouse;
import com.isofarm.ui.GameUIService;
import com.isofarm.utils.Settings;
import com.isofarm.wrld.GameMaster;
import org.joml.Vector3f;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents the methods, data, behavior of the {@link NPC}'s.
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
        add(new NPC(gender, Job.FARMER));
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
        if (closest == null) return false;
        closest.damage(Player.plyr.getAttack(), Player.plyr);
        closest.grunt(closest.getGender());
        return true;
    }

    /**
     * Returns the closest {@link NPC} to the {@link Player}.
     * @return the closest {@link NPC}
     */
    public NPC getClosest() {
        GameMaster gameMaster = GameMaster.game;
        if (gameMaster == null || Player.plyr == null) return null;
        Ray ray = gameMaster.getCamera().getMouseRay(Mouse.getX(), Mouse.getY(),
                gameMaster.getWindowWidth(), gameMaster.getWindowHeight());
        NPC closest = null;
        float closestRayDistance = Float.POSITIVE_INFINITY;
        for (NPC npc : npcsList) {
            if (!npc.isAlive()) continue;
            float playerDistance = Player.plyr.getPosition().distance(npc.getPosition());
            if (playerDistance > Settings.getMaxInteractionDistance()) continue;
            float rayDistance = npc.rayIntersection(ray.origin(), ray.direction());
            if (rayDistance < closestRayDistance) {
                closest = npc;
                closestRayDistance = rayDistance;
            }
        }
        return closest;
    }

    /**
     * Adds an NPC to the managed list and world.
     * @param npc the NPC to add
     * @return the added NPC
     */
    public NPC add(NPC npc) {
        npcsList.add(npc);
        GameMaster.game.addEntity(npc);
        return npc;
    }

    /**
     * Removes an NPC from the managed list and world.
     * @param npc the NPC to remove
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
}
