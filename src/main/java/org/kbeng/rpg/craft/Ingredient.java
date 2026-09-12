package org.kbeng.rpg.craft;

import org.kbeng.rpg.data.DataClass;
import org.kbeng.rpg.item.Craftable;

import java.util.ArrayList;
import java.util.List;

/**
 * Ingredient is an immutable carrier for ingredient state in the craft subsystem.
 * Record semantics make instances cheap to pass across systems while preserving value-based equality and snapshot safety.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public record Ingredient(Craftable craftable, int amount, List<Ingredient> alternatives) {
    public Ingredient(Craftable craftable, int amount) {
        this(craftable, amount, List.of());
    }

    public Ingredient {
        alternatives = List.copyOf(alternatives);
    }

    /** Returns this ingredient and every craftable that may replace it. */
    public List<Ingredient> options() {
        List<Ingredient> options = new ArrayList<>(alternatives.size() + 1);
        options.add(new Ingredient(craftable, amount));
        options.addAll(alternatives);
        return List.copyOf(options);
    }
}
