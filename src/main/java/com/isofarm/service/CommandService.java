package com.isofarm.service;

import com.isofarm.data.Command;
import com.isofarm.ui.GameUIService;
import com.isofarm.utils.ToastFactory;

/**
 * Represents the command service component of the Isofarm runtime.
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
public class CommandService implements Service<Command> {
    private final CommandRegistry registry;

    /**
     * Creates a new {@code CommandService} instance.
     * @param registry the {@link CommandRegistry} supplied as {@code registry}
     */
    public CommandService(CommandRegistry registry) {
        this.registry = registry;
    }

    /**
     * Executes execute as part of the application lifecycle.
     * @param input the {@link String} supplied as {@code input}
     */
    public void execute(String input) {
        if (input == null || input.isBlank()) return;
        if (!input.startsWith("/")) return;

        input = input.trim();
        String[] tokens = input.split("\\s+");
        if (tokens.length == 0) return;

        String commandName = tokens[0];
        Command command = registry.get(commandName);
        if (command == null) {
            if (GameUIService.ui != null) {
                ToastFactory.error("Command not found: " + commandName);
            }
            return;
        }

        String[] args = new String[tokens.length - 1];
        System.arraycopy(tokens, 1, args, 0, args.length);
        command.action().accept(args);
    }
}
