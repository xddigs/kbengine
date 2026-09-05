package com.isofarm.service;

import com.isofarm.data.Command;
import com.isofarm.ui.GameUIService;
import com.isofarm.utils.ToastFactory;

/**
 * Encapsulates the state and operations required by command service within the game runtime.
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
