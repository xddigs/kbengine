package org.kbeng.service;

import org.kbeng.data.MaterialID;
import org.kbeng.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * ItemRegistry provides item registry capabilities within the service subsystem.
 *
 * It provides shared runtime services, registries, and policy logic consumed by orchestrators and feature modules.
 *
 * The registry maintains canonical lookup structures and resolves identifiers to runtime instances.
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
