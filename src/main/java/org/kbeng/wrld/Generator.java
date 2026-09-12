package org.kbeng.wrld;

/**
 * Generator defines the generator contract within the wrld subsystem.
 * Implementations are expected to preserve these behavioral guarantees while choosing their own storage and execution strategy.
 * The generator produces deterministic content from seeds, rules, and runtime configuration.
 */
public interface Generator {
    /**
     * Generates a chunk
     * 1. Generates the terrain
     * 2. Generates the trees
     * 3. Generates the plants
     * @param chunkX the {@code int} supplied as {@code chunkX}
     * @param chunkZ the {@code int} supplied as {@code chunkZ}
     */
    void generateChunk(int chunkX, int chunkZ);
}
