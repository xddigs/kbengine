package org.kbeng.entity;

import org.kbeng.data.*;
import org.kbeng.wrld.GameMaster;

/**
 * Animal provides animal capabilities within the entity subsystem.
 *
 * It participates in actor simulation, state transitions, and per-frame world interaction contracts.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 *
 * It extends Entity, inheriting shared behavior while specializing subsystem-specific logic.
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
