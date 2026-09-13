package org.kbeng.games.rpg.voxel;

import org.junit.jupiter.api.Test;
import org.kbeng.games.rpg.data.BlockData;
import org.joml.Vector3f;

import static org.junit.jupiter.api.Assertions.*;

/** Regression tests for the RPG-only quarter-unit data path. They deliberately
 * avoid an OpenGL context: generation, persistence and DDA selection are pure
 * data contracts and can be verified on CI hosts without a display. */
class VoxelTerrainTest {
    @Test
    void runLengthColumnReplacesOneCellOnly() {
        byte[] cells = new byte[VoxelGrid.HEIGHT];
        cells[10] = BlockData.STONE.getId();
        VoxelColumn original = VoxelColumn.compress(cells);
        VoxelColumn changed = original.with(10, BlockData.DIRT.getId());
        assertEquals(BlockData.STONE.getId(), original.get(10));
        assertEquals(BlockData.DIRT.getId(), changed.get(10));
        assertEquals(0, changed.get(9));
        assertEquals(0, changed.get(11));
    }

    @Test
    void generatedColumnsAreQuarterUnitAndEditsSurviveUnload() {
        VoxelWorld world = new VoxelWorld(42L);
        world.generate(0, 0);
        int y = VoxelGrid.cell(world.surface(0.5f, 0.5f)) - 1;
        assertTrue(y >= 0);
        assertTrue(world.set(2, y, 2, BlockData.AIR.getId()));
        world.unload(0, 0);
        world.generate(0, 0);
        assertEquals(BlockData.AIR.getId(), world.get(2, y, 2));
    }

    @Test
    void ddaHitsTheSelectedQuarterVoxelAndReportsFace() {
        VoxelWorld world = new VoxelWorld(7L);
        world.generate(0, 0);
        for (int y = 0; y < 700; y++) world.set(8, y, 8, BlockData.AIR.getId());
        assertTrue(world.set(8, 400, 8, BlockData.STONE.getId()));
        VoxelRaycast.Hit hit = VoxelRaycast.cast(world,
                new Vector3f(2.125f, 99.875f, 2.125f), new Vector3f(0, 1, 0), 2, true);
        assertNotNull(hit);
        assertEquals(8, hit.x());
        assertEquals(400, hit.y());
        assertEquals(-1, hit.ny());
    }
}
