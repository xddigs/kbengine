package org.kbeng.item;

import org.kbeng.data.BookLine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Page provides page capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public class Page {
    private final List<BookLine> bookLines;

    /**
     * Creates a new {@code Page} instance.
     */
    public Page() {
        this.bookLines = new ArrayList<>();
    }

    /**
     * Returns the lines.
     * @return the {@link List} representing the lines
     */
    public List<BookLine> getLines() {
        return bookLines;
    }

    /**
     * Adds the line.
     * @param line the {@link String} supplied as {@code line}
     */
    public void addLine(String line) {
        bookLines.add(new BookLine(line));
    }

    /**
     * Adds the line.
     * @param line the {@link String} supplied as {@code line}
     * @param action the {@link Consumer} supplied as {@code action}
     */
    public void addLine(String line, Consumer<BookLine> action) {
        bookLines.add(new BookLine(line, action));
    }

    /**
     * Adds the line.
     * @param line the {@link String} supplied as {@code line}
     * @param action the {@link Consumer} supplied as {@code action}
     * @param tooltipText the {@link String} supplied as {@code tooltipText}
     */
    public void addLine(String line, Consumer<BookLine> action, String tooltipText) {
        BookLine bookLine = new BookLine(line, action);
        bookLine.setTooltipText(tooltipText);
        bookLines.add(bookLine);
    }

    /**
     * Adds an icon-backed interactive line.
     * @param item the {@link Item} argument; the item displayed as an icon
     * @param action the {@link Consumer} supplied as {@code action}
     * @param tooltipText the {@link String} supplied as {@code tooltipText}
     */
    public BookLine addItem(Item item, Consumer<BookLine> action, String tooltipText) {
        BookLine bookLine = new BookLine(item, action);
        bookLine.setTooltipText(tooltipText);
        bookLines.add(bookLine);
        return bookLine;
    }

    /**
     * Returns the line.
     * @param index the {@code int} supplied as {@code index}
     * @return the {@link BookLine} representing the line
     */
    public BookLine getLine(int index) {
        return bookLines.get(index);
    }

    /**
     * Sets the line.
     * @param index the {@code int} supplied as {@code index}
     * @param line the {@link String} supplied as {@code line}
     */
    public void setLine(int index, String line) {
        bookLines.set(index, new BookLine(line));
    }

    /**
     * Adds line to the corresponding collection or processing queue.
     * @param index the {@code int} supplied as {@code index}
     * @param line the {@link String} supplied as {@code line}
     */
    public void insertLine(int index, String line) {
        bookLines.add(index, new BookLine(line));
    }

    /**
     * Removes the line.
     * @param index the {@code int} supplied as {@code index}
     */
    public void removeLine(int index) {
        bookLines.remove(index);
    }

    /**
     * Removes clear.
     */
    public void clear() {
        bookLines.clear();
    }

    /**
     * Returns the number or extent represented by size.
     * @return {@code int}; the size result
     */
    public int size() {
        return bookLines.size();
    }

    /**
     * Updates or derives runtime state for for each line according to the supplied arguments.
     * @param consumer the {@link Consumer} supplied as {@code consumer}
     */
    public void forEachLine(Consumer<BookLine> consumer) {
        bookLines.forEach(consumer);
    }
}
