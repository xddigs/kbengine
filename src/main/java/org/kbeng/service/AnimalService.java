package org.kbeng.service;

import org.kbeng.data.Singleton;
import org.kbeng.data.Task;
import org.kbeng.entity.Animal;
import org.kbeng.wrld.GameMaster;

import java.util.LinkedList;
import java.util.List;

/**
 * AnimalService provides animal service capabilities within the service subsystem.
 * It provides shared runtime services, registries, and policy logic consumed by orchestrators and feature modules.
 * The service acts as a shared policy and state access point for other runtime modules.
 * It implements Service<Animal>, providing a concrete strategy for this subsystem contract.
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
