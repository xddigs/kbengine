package com.isofarm.craft;

import com.isofarm.data.DataClass;
import com.isofarm.item.Craftable;
import com.isofarm.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable value object containing ingredient.
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
