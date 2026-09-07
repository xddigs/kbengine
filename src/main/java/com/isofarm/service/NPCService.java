package com.isofarm.service;

import com.isofarm.data.Singleton;
import com.isofarm.data.TODO;
import com.isofarm.entity.NPC;
import com.isofarm.wrld.GameMaster;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents the methods, data, behavior of the {@link NPC}'s.
 */
@Singleton
@TODO(reason="Pending map of NPCModels")
public class NPCService implements Service<NPC> {
    public static final NPCService npcs = new NPCService();
    private final List<NPC> npcsList = new LinkedList<>();

    /** Creates a new {@code NPCService} instance. */
    private NPCService() {}

    /**
     * Initializes the NPCService and creates the NPCs.
     */
    public void init() {}

    /**
     * Adds an NPC to the list of NPCs
     * @param npc {@link NPC} to be added
     * @return {@link NPC}
     */
    public NPC add(NPC npc) {
        npcsList.add(npc);
        GameMaster.game.addEntity(npc);
        return npc;
    }

    /**
     * Remove an NPC to the list of NPCs
     * @param npc {@link NPC} to be added
     * @return {@link NPC}
     */
    public NPC remove(NPC npc) {
        npcsList.remove(npc);
        GameMaster.game.removeEntity(npc);
        return npc;
    }

    /**
     * Clears the list of NPCs
     */
    public void clear() {
        npcsList.clear();
    }

    /**
     * Retrieves {@code npcsList}
     * @return {@link List} {@link NPC} value of npcsList
     */
    public List<NPC> getNpcs() {
        return npcsList;
    }
}
