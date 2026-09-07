package com.isofarm.utils;

import com.isofarm.data.Singleton;

import java.util.Random;

/**
 * Defines the naming contract, generates a random {@link String} full name
 */
@SuppressWarnings("all")
@Singleton
@Utils
public class Naming {
    public static final Naming nm = new Naming();
    private static final Random random = new Random();
    private static final String[] names = {
            "Adrian", "Alex", "Avery", "Blake", "Cameron",
            "Clara", "Daniel", "Diana", "Elias", "Emma",
            "Eric", "Iris", "Jamie", "Jordan", "Leo",
            "Luna", "Morgan", "Nora", "Oliver", "Robin",
            "Sasha", "Sofia", "Taylor", "Victor", "Zoe"
    };

    private static final String[] lastNames = {
            "Anderson", "Bennett", "Brooks", "Carter", "Collins",
            "Cooper", "Davis", "Foster", "Garcia", "Gray",
            "Hayes", "Hughes", "Jackson", "Lewis", "Martin",
            "Miller", "Morgan", "Parker", "Reed", "Rivera",
            "Scott", "Stone", "Walker", "Ward", "Wilson"
    };

    /**
     * Generates a random full name
     * @return the {@link String} representing the full name
     */
    public String fullName() {
        return names[random.nextInt(names.length)] + " " +
                lastNames[random.nextInt(lastNames.length)];
    }
}
