package com.isofarm.service;

import com.isofarm.data.MaterialID;
import com.isofarm.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Represents the item registry component of the Isofarm runtime.
 *
 * <p>This type centralizes the state, lifecycle and behavior required by its callers,
 * keeping domain rules together with the data they operate on.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li><b>State:</b> Owns the data needed to represent the component consistently.</li>
 *   <li><b>Lifecycle:</b> Exposes the operations used to create, update and release its state.</li>
 *   <li><b>Integration:</b> Coordinates with the surrounding game, rendering or UI systems through its public API.</li>
 * </ul>
 *
 * <p>Callers should use the documented public operations and allow this type to preserve
 * its invariants rather than modifying implementation details directly.
 */
public class ItemRegistry {
    private final Map<String, Supplier<Item>> items = new HashMap<>();
    private final Map<MaterialID, Supplier<Item>> materials = new HashMap<>();

    /**
     * Adds the supplied element to the corresponding collection or processing queue.
     * @param id the {@link String} supplied as {@code id}
     * @param factory the {@link Supplier} supplied as {@code factory}
     */
    public void register(String id, Supplier<Item> factory) {
        items.put(id.toLowerCase(), factory);
    }

    /**
     * Returns create.
     * @param id the {@link String} supplied as {@code id}
     * @return the {@link Item} representing the create result
     */
    public Item create(String id) {
        if (id == null) return null;
        Supplier<Item> factory = items.get(id.toLowerCase());
        if (factory == null) return null;
        return factory.get();
    }

    /**
     * Adds the supplied element to the corresponding collection or processing queue.
     * @param materialID the {@link MaterialID} supplied as {@code materialID}
     * @param factory the {@link Supplier} supplied as {@code factory}
     */
    public void register(MaterialID materialID,
                         Supplier<Item> factory) {
        if (materialID == null || factory == null) return;
        materials.put(materialID, factory);
    }

    /**
     * Returns create.
     * @param materialID the {@link MaterialID} supplied as {@code materialID}
     * @return the {@link Item} representing the create result
     */
    public Item create(MaterialID materialID) {
        if (materialID == null) return null;
        Supplier<Item> factory = materials.get(materialID);
        if (factory == null) return null;
        return factory.get();
    }

    /**
     * Determines whether this object is satisfied by the current state.
     * @param materialID the {@link MaterialID} supplied as {@code materialID}
     * @return {@code boolean}; the contains result
     */
    public boolean contains(MaterialID materialID) {
        return materialID != null && materials.containsKey(materialID);
    }

    /**
     * Removes the supplied element and updates any dependent state.
     * @param materialID the {@link MaterialID} supplied as {@code materialID}
     */
    public void unregister(MaterialID materialID) {
        if (materialID != null) {
            materials.remove(materialID);
        }
    }

    /**
     * Clears the materials.
     */
    public void clearMaterials() {
        materials.clear();
    }

    /**
     * Removes clear.
     */
    public void clear() {
        items.clear();
        materials.clear();
    }

    /**
     * Returns the number or extent represented by size.
     * @return {@code int}; the size result
     */
    public int size() {
        return items.size();
    }

    /**
     * Returns the number or extent represented by material size.
     * @return {@code int}; the material size result
     */
    public int materialSize() {
        return materials.size();
    }

    /**
     * Returns the ids.
     * @return the {@link List} representing the ids
     */
    public List<String> getIds() {
        return items.keySet()
                .stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /**
     * Returns the material ids.
     * @return the {@link List} representing the material ids
     */
    public List<String> getMaterialIds() {
        return materials.keySet()
                .stream()
                .map(Enum::name)
                .toList();
    }
}