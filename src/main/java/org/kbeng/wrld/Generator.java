package org.kbeng.wrld;

/**
 * Defines the generator contract.
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
