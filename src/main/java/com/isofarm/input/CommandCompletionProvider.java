package com.isofarm.input;

import com.isofarm.data.Command;
import com.isofarm.data.CommandArgument;
import com.isofarm.data.CompletionProvider;
import com.isofarm.service.CommandRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the state and operations required by command completion provider within the game runtime.
 */
public class CommandCompletionProvider implements CompletionProvider {
    private final CommandRegistry commandRegistry;
    /**
     * Creates a new {@code CommandCompletionProvider} instance.
     * @param commandRegistry the {@link CommandRegistry} supplied as {@code commandRegistry}
     */
    public CommandCompletionProvider(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    /**
     * {@inheritDoc}
     * Updates text or selection state for complete.
     * @param text the {@link String} supplied as {@code text}
     * @param cursorPosition the {@code int} supplied as {@code cursorPosition}
     * @return the {@link List} representing the complete result
     */
    @Override
    public List<String> complete(String text, int cursorPosition) {
        if (text == null || cursorPosition <= 0) {
            return List.of();
        }
        String beforeCursor = text.substring(0, cursorPosition);
        if (!beforeCursor.contains(" ")) {
            return completeCommand(beforeCursor);
        }
        return completeArgument(beforeCursor);
    }

    /**
     * Updates text or selection state for complete command.
     * @param text the {@link String} supplied as {@code text}
     * @return the {@link List} representing the complete command result
     */
    private List<String> completeCommand(String text) {
        if (!text.startsWith("/")) {
            return List.of();
        }

        String prefix = text.substring(1).toLowerCase();
        List<String> result = new ArrayList<>();
        for (String commandName : commandRegistry.getCommands().keySet()) {
            String normalized = commandName.startsWith("/") ? commandName.substring(1) : commandName;
            if (normalized.toLowerCase().startsWith(prefix)) {
                result.add("/" + normalized);
            }
        }
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    /**
     * Updates text or selection state for complete argument.
     * @param text the {@link String} supplied as {@code text}
     * @return the {@link List} representing the complete argument result
     */
    private List<String> completeArgument(String text) {
        String trimmedLead = text.stripLeading();
        if (trimmedLead.isEmpty()) {
            return List.of();
        }

        String[] parts = trimmedLead.split("\\s+");
        String commandName = parts[0];
        Command command = commandRegistry.get(commandName);
        if (command == null) {
            return List.of();
        }

        int firstSpaceIndex = text.indexOf(' ');
        if (firstSpaceIndex == -1) {
            return List.of();
        }

        String argsSubstring = text.substring(firstSpaceIndex + 1);
        String[] args = argsSubstring.split("\\s+", -1);
        int argumentIndex = args.length - 1;

        if (argumentIndex < 0 || argumentIndex >= command.args().length) {
            return List.of();
        }

        CommandArgument argument = command.args()[argumentIndex];
        if (argument == null || !argument.hasCompletion()) {
            return List.of();
        }

        String inputPrefix = args[argumentIndex];
        // Completion providers expect a cursor position within the argument text,
        // not the ordinal of the argument in the command.
        return argument.complete(inputPrefix, inputPrefix.length());
    }
}
