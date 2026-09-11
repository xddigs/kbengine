package org.kbeng.data;

import java.util.List;

/**
 * Defines the completion provider contract.
 */
@DataClass
@FunctionalInterface
public interface CompletionProvider {
    /**
     * Updates text or selection state for complete.
     * @param text the {@link String} supplied as {@code text}
     * @param cursorPosition the {@code int} supplied as {@code cursorPosition}
     * @return the {@link List} representing the complete result
     */
    List<String> complete(String text, int cursorPosition);
}