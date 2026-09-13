package org.kbeng.games.rpg.data;

import org.kbeng.games.rpg.item.Item;

/** Session-scoped selection state owned by the RPG layer. */
public final class ItemSelection {
    private ItemSelection() { }

    public static Item selectedItem;

    public static Item getSelectedItem() {
        return selectedItem;
    }
}
