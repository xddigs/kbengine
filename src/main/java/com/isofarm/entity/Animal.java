package com.isofarm.entity;

import com.isofarm.data.*;
import com.isofarm.wrld.GameMaster;

/**
 * Represents the animal component of the Isofarm runtime.
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
@DataClass
@Task(reason="Animals are not implemented yet")
public class Animal extends Entity {
    private final AnimalData animal;

    /**
     * Creates a new {@code Animal} instance.
     * @param name the {@link String} supplied as {@code name}
     */
    public Animal(String name, AnimalData animal) {
        super(name);
        this.animal = animal;
    }

    /**
     * Creates a new {@code Animal} instance.
     */
    public Animal(AnimalData animal) {
        this(Animal.class.getSimpleName(), animal);
    }

    /** Creates a new {@code Animal} instance. Specifically a {@link AnimalData#COW} */
    public Animal() {
        this(AnimalData.COW);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(BlockPos blockPos, float delta) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(GameMaster gameMaster, RenderPass pass) {

    }

    /**
     * Retrieves {@code animal}
     * @return {@link AnimalData} value of animal
     */
    public AnimalData getAnimal() {
        return animal;
    }
}
