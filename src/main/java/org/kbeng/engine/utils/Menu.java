package org.kbeng.engine.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Builder for a terminal menu whose options may be supplied directly or by
 * {@link MenuProvider} implementations discovered on the classpath.
 */
public final class Menu {
    private static final String DEFAULT_PROMPT = "application";

    private final BufferedReader input;
    private final PrintStream output;
    private final List<Option> options = new ArrayList<>();
    private String prompt = DEFAULT_PROMPT;
    private boolean providersLoaded;

    private Menu(InputStream input, PrintStream output) {
        this.input = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(input, "input"), StandardCharsets.UTF_8));
        this.output = Objects.requireNonNull(output, "output");
    }

    /** Creates a menu connected to the process terminal. */
    public static Menu create() {
        return new Menu(System.in, System.out);
    }

    /** Sets the name shown in the menu heading. */
    public Menu prompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Menu prompt must not be blank");
        }
        this.prompt = prompt.strip();
        return this;
    }

    /** Adds an option to this menu. Options retain insertion order. */
    public Menu option(String label, Runnable action) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Menu option label must not be blank");
        }
        String normalizedLabel = label.strip();
        boolean duplicate = options.stream()
                .anyMatch(option -> option.label().equalsIgnoreCase(normalizedLabel));
        if (duplicate) {
            throw new IllegalArgumentException("Duplicate menu option: " + normalizedLabel);
        }
        options.add(new Option(normalizedLabel,
                Objects.requireNonNull(action, "action")));
        return this;
    }

    /**
     * Displays the menu, reads until a valid selection is entered and executes
     * the selected action. Selecting zero or closing standard input exits.
     */
    public void display() {
        loadProviders();
        if (options.isEmpty()) {
            throw new IllegalStateException("No terminal menu options are registered");
        }

        while (true) {
            printMenu();
            String line = readLine();
            if (line == null) return;

            Integer selection = parseSelection(line);
            if (selection == null || selection < 0 || selection > options.size()) {
                output.println("Invalid option. Enter a number between 0 and "
                        + options.size() + ".");
                output.println();
                continue;
            }
            if (selection == 0) return;

            options.get(selection - 1).action().run();
            return;
        }
    }

    /**
     * Loads {@link MenuProvider} implementations discovered on the classpath.
     */
    private void loadProviders() {
        if (providersLoaded) return;
        providersLoaded = true;

        List<MenuProvider> providers = ServiceLoader.load(MenuProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparingInt(MenuProvider::order)
                        .thenComparing(provider -> provider.getClass().getName()))
                .toList();
        providers.forEach(provider -> provider.configure(this));
    }

    /**
     * Prints the menu to standard output.
     */
    private void printMenu() {
        output.println("Welcome to " + prompt + "! Select an option:");
        for (int index = 0; index < options.size(); index++) {
            output.println("  " + (index + 1) + ") " + options.get(index).label());
        }
        output.println("  0) Exit");
        output.print("> ");
        output.flush();
    }

    /**
     * Reads a line from standard input. Returns {@code null} if the end of the
     * stream is reached.
     */
    private String readLine() {
        try {
            return input.readLine();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read the terminal selection", exception);
        }
    }

    /**
     * Parses a selection from a string. Returns {@code null} if the string is
     * @param value the {@link String} supplied as {@code value}
     * @return the {@code int} representing the parsed selection, or {@code null}
     */
    private static Integer parseSelection(String value) {
        try {
            return Integer.valueOf(value.strip());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** Represents a menu option. */
    private record Option(String label, Runnable action) { }
}
