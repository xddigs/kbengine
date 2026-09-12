package org.kbeng.rpg.service;

import org.kbeng.rpg.data.BlockData;
import org.kbeng.engine.utils.Utils;

import java.util.HashMap;
import java.util.Map;
/**
 * BlockRegistry provides block registry capabilities within the service subsystem.
 * It provides shared runtime services, registries, and policy logic consumed by orchestrators and feature modules.
 * The registry maintains canonical lookup structures and resolves identifiers to runtime instances.
 * It implements Service<BlockData>, providing a concrete strategy for this subsystem contract.
 */

@Utils
public class BlockRegistry implements Service<BlockData> {
    private static final int MAX_BLOCK_IDS = 256;
    private static final BlockData[] ID_TO_BLOCK = new BlockData[MAX_BLOCK_IDS];
    private static final Map<BlockData, Byte> BLOCK_TO_ID = new HashMap<>();
    private static final Map<String, BlockData> NAME_TO_BLOCK = new HashMap<>();
    private static boolean isInitialized;

    public static synchronized void init() {
        if (isInitialized) return;
        int currentId = 0;
        for (BlockData block : BlockData.values()) {
            if (block == BlockData.AIR) {
                register((byte) 0, block);
                continue;
            }

            if (++currentId >= MAX_BLOCK_IDS) {
                throw new IllegalStateException("A byte can represent at most " + MAX_BLOCK_IDS + " block types");
            }

            register((byte) currentId, block);
        }
        isInitialized = true;
    }

    public static void register(byte id, BlockData block) {
        int index = Byte.toUnsignedInt(id);
        if (block == null) throw new IllegalArgumentException("Block cannot be null");
        if (ID_TO_BLOCK[index] != null || BLOCK_TO_ID.containsKey(block)) {
            throw new IllegalStateException("Block id already registered: " + index);
        }
        ID_TO_BLOCK[index] = block;
        BLOCK_TO_ID.put(block, id);
        NAME_TO_BLOCK.put(block.name(), block);
        NAME_TO_BLOCK.put(block.getName(), block);
    }

    public static BlockData getBlock(byte id) {
        safeguard();
        return ID_TO_BLOCK[Byte.toUnsignedInt(id)];
    }

    public static byte getId(BlockData block) {
        safeguard();
        Byte id = BLOCK_TO_ID.get(block);
        if (id == null) throw new IllegalArgumentException("Unregistered block: " + block);
        return id;
    }

    public static BlockData getByName(String name) {
        safeguard();
        return NAME_TO_BLOCK.get(name);
    }

    private static void safeguard() {
        if (!isInitialized) init();
    }
}
