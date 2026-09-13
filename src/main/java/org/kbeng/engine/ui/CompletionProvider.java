package org.kbeng.engine.ui;

import java.util.List;

/**
 * CompletionProvider defines the completion provider contract within the data subsystem.
 * Implementations are expected to preserve these behavioral guarantees while choosing their own storage and execution strategy.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
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
