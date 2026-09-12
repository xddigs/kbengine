package org.kbeng.wrld;

import org.kbeng.data.BlockData;

/**
 * FlatworldGenerator provides flatworld generator capabilities within the wrld subsystem.
 *
 * It maintains world simulation concerns including terrain, chunks, fluids, and authoritative spatial state.
 *
 * The generator produces deterministic content from seeds, rules, and runtime configuration.
 *
 * It implements Generator, providing a concrete strategy for this subsystem contract.
 */
public final class FlatworldGenerator implements Generator {
    private static final int SURFACE_Y = 128;
    private static final int TOTAL_DEPTH = 128;
    private static final int BOTTOM_Y = SURFACE_Y - TOTAL_DEPTH + 1;

    /**
     * Creates a flatworld generator bound to a world instance.
     */
    public FlatworldGenerator() {}

    /**
     * {@inheritDoc}
     * Generates one chunk of infinite flat terrain.
     * @param chunkX the {@code int} supplied as {@code chunkX}
     * @param chunkZ the {@code int} supplied as {@code chunkZ}
     */
    @Override
    public void generateChunk(int chunkX, int chunkZ) {
        Chunk chunk = World.wrld.getOrCreateChunk(chunkX, chunkZ);
        for (int localX = 0; localX < Chunk.SIZE_X; localX++) {
            for (int localZ = 0; localZ < Chunk.SIZE_Z; localZ++) {
                generateColumn(chunk, localX, localZ);
            }
        }
    }

    /**
     * Fills one vertical chunk column using the configured flatworld layers.
     * @param chunk the {@link Chunk} supplied as {@code chunk}
     * @param localX the {@code int} supplied as {@code localX}
     * @param localZ the {@code int} supplied as {@code localZ}
     */
    private void generateColumn(Chunk chunk, int localX, int localZ) {
        for (int y = BOTTOM_Y; y <= SURFACE_Y; y++) {
            int depth = SURFACE_Y - y + 1;
            byte blockId;

            if (depth == TOTAL_DEPTH) {
                blockId = BlockData.VOIDSEAL.getId();
            } else if (depth == 1) {
                blockId = BlockData.GRASS.getId();
            } else if (depth <= 3) {
                blockId = BlockData.DIRT.getId();
            } else {
                blockId = BlockData.STONE.getId();
            }

            chunk.setBlock(localX, y, localZ, blockId);
        }
    }
}
