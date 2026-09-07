package com.isofarm.service;

import com.isofarm.data.Singleton;
import com.isofarm.data.Task;
import com.isofarm.entity.Animal;
import com.isofarm.wrld.GameMaster;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents the methods, data, behavior of the {@link Animal}'s.
 */
@Singleton
@Task(reason="Pending map of Animals")
public class AnimalService implements Service<Animal> {
    public static final AnimalService anml = new AnimalService();
    private final List<Animal> animalsList = new LinkedList<>();

    /** Creates a new {@code AnimalService} instance. */
    private AnimalService() {}

    /**
     * Initializes the AnimalService and creates the Animals.
     */
    public void init() {}

    /**
     * Adds an animal to the list of Animals
     * @param animal {@link Animal} to be added
     * @return {@link Animal}
     */
    public Animal add(Animal animal) {
        animalsList.add(animal);
        GameMaster.game.addEntity(animal);
        return animal;
    }

    /**
     * Remove an animal to the list of Animals
     * @param animal {@link Animal} to be added
     * @return {@link Animal}
     */
    public Animal remove(Animal animal) {
        animalsList.remove(animal);
        GameMaster.game.removeEntity(animal);
        return animal;
    }

    /**
     * Clears the list of Animals
     */
    public void clear() {
        animalsList.clear();
    }

    /**
     * Retrieves {@code AnimalsList}
     * @return {@link List} {@link Animal} value of AnimalsList
     */
    public List<Animal> getAnimals() {
        return animalsList;
    }
}
