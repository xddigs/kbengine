package com.isofarm.entity;

import com.isofarm.data.*;
import com.isofarm.wrld.GameMaster;

/**
 * Encapsulates the state and operations required by animal within the game runtime.
 */
@DataClass
@TODO(reason="Animals are not implemented yet")
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
