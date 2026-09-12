package org.kbeng.data;

import java.util.List;
import java.util.function.BiFunction;

/**
 * CommandArgument is an immutable carrier for command argument state in the data subsystem.
 *
 * Record semantics make instances cheap to pass across systems while preserving value-based equality and snapshot safety.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public record CommandArgument(String name, BiFunction<String, Integer, List<String>> completion) {

    /**
     * Creates a new {@code CommandArgument} instance.
     * @param name the {@link String} supplied as {@code name}
     */
    public CommandArgument(String name) {
        this(name, null);
    }

    /**
     * Creates or returns of from the supplied arguments.
     * @param name the {@link String} supplied as {@code name}
     * @param completion the {@link BiFunction} supplied as {@code completion}
     * @return the {@link CommandArgument} representing the of result
     */
    public static CommandArgument of(String name, BiFunction<String, Integer, List<String>> completion) {
        return new CommandArgument(name, completion);
    }

    /**
     * Checks whether the completion condition is met.
     * @return {@code true} if completion; otherwise {@code false}
     */
    public boolean hasCompletion() {
        return completion != null;
    }

    /**
     * Updates text or selection state for complete.
     * @param text the {@link String} supplied as {@code text}
     * @param cursorPosition the {@code int} supplied as {@code cursorPosition}
     * @return the {@link List} representing the complete result
     */
    public List<String> complete(String text, int cursorPosition) {
        if (completion == null) {
            return List.of();
        }

        if (text == null) {
            text = "";
        }

        cursorPosition = Math.clamp(cursorPosition, 0, text.length());
        return completion.apply(text, cursorPosition);
    }
}