package org.kbeng.games.rpg.item;

import org.kbeng.games.rpg.data.BlockData;
import java.util.Objects;

/** Inventory unit representing exactly one quarter-unit solid or fluid cell.
 * It has no placed coordinates, GL resources, shape state or breaking animation.
 * Legacy door/chest material names remain valid colours, not interactive objects. */
public record Voxel(BlockData type) implements Craftable {
    public Voxel { Objects.requireNonNull(type); }
    public BlockData getType() { return type; }
    @Override public byte getId() { return type.getId(); }
    @Override public String getName() { return type.getDisplayName(); }
    @Override public String getDisplayName() { return type.getDisplayName(); }
    @Override public int getValue() { return type.getValue(); }
    @Override public Item copy() { return new Voxel(type); }
}
