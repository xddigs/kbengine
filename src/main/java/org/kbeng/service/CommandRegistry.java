package org.kbeng.service;

import org.kbeng.data.Command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CommandRegistry provides command registry capabilities within the service subsystem.
 * It provides shared runtime services, registries, and policy logic consumed by orchestrators and feature modules.
 * The registry maintains canonical lookup structures and resolves identifiers to runtime instances.
 * It implements Service<Command>, providing a concrete strategy for this subsystem contract.
 */
public class CommandRegistry implements Service<Command> {
    private final Map<String, Command> commands = new HashMap<>();

    /**
     * Creates a new {@code CommandRegistry} instance.
     */
    public CommandRegistry() {
    }

    /**
     * Adds the supplied element to the corresponding collection or processing queue.
     * @param command the {@link Command} supplied as {@code command}
     */
    public void register(Command command) {
        if (command == null || command.name() == null) {
            return;
        }

        commands.put(normalize(command.name()), command);
    }

    /**
     * Returns get.
     * @param name the {@link String} supplied as {@code name}
     * @return the {@link Command} representing the get result
     */
    public Command get(String name) {
        if (name == null) {
            return null;
        }

        return commands.get(normalize(name));
    }

    /**
     * Determines whether this object is satisfied by the current state.
     * @param name the {@link String} supplied as {@code name}
     * @return {@code boolean}; the contains result
     */
    public boolean contains(String name) {
        return name != null && commands.containsKey(normalize(name));
    }

    /**
     * Removes the supplied element and updates any dependent state.
     * @param name the {@link String} supplied as {@code name}
     */
    public void unregister(String name) {
        if (name != null) {
            commands.remove(normalize(name));
        }
    }

    /**
     * Removes clear.
     */
    public void clear() {
        commands.clear();
    }

    /**
     * Checks whether the empty condition is met.
     * @return {@code true} if empty; otherwise {@code false}
     */
    public boolean isEmpty() {
        return commands.isEmpty();
    }

    /**
     * Returns the number or extent represented by size.
     * @return {@code int}; the size result
     */
    public int size() {
        return commands.size();
    }

    /**
     * Returns the commands.
     * @return the {@link Map} representing the commands
     */
    public Map<String, Command> getCommands() {
        return commands;
    }

    /**
     * Returns the names.
     * @return the {@link List} representing the names
     */
    public List<String> getNames() {
        return commands.keySet()
                .stream()
                .sorted()
                .toList();
    }

    /**
     * Transforms this object according to the supplied values.
     * @param name the {@link String} supplied as {@code name}
     * @return the {@link String} representing the normalize result
     */
    private String normalize(String name) {
        return name.startsWith("/")
                ? name.substring(1).toLowerCase()
                : name.toLowerCase();
    }
}
