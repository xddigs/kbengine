package org.kbeng.data;

import org.kbeng.item.Item;

import java.util.function.Consumer;

/**
 * Represents the book line component of the kbengine runtime.
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
public class BookLine {
    private String text;
    private Consumer<BookLine> action;
    private String tooltipText;
    private Item item;
    private boolean favorite;

    /**
     * Creates a new {@code BookLine} instance.
     * @param text the {@link String} supplied as {@code text}
     */
    public BookLine(String text) {
        this(text, null);
    }

    /**
     * Creates a new {@code BookLine} instance.
     * @param text the {@link String} supplied as {@code text}
     * @param action the {@link Consumer} supplied as {@code action}
     */
    public BookLine(String text, Consumer<BookLine> action) {
        this.text = text;
        this.action = action;
    }

    /**
     * Creates an icon-backed interactive book line.
     * @param item the {@link Item} argument; the item rendered by the book UI
     * @param action the {@link Consumer} supplied as {@code action}
     */
    public BookLine(Item item, Consumer<BookLine> action) {
        this(item != null ? item.getDisplayName() : "", action);
        this.item = item;
    }

    /**
     * Returns the text.
     * @return the {@link String} representing the text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text.
     * @param text the {@link String} supplied as {@code text}
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Checks whether the interactive condition is met.
     * @return {@code true} if interactive; otherwise {@code false}
     */
    public boolean isInteractive() {
        return action != null;
    }

    /**
     * Sets the click.
     * @param action the {@link Consumer} supplied as {@code action}
     */
    public void setClick(Consumer<BookLine> action) {
        this.action = action;
    }

    /**
     * Handles click and applies its effect to the current interaction state.
     */
    public void click() {
        if (action != null) {
            action.accept(this);
        }
    }

    /**
     * Returns the tooltip text.
     * @return the {@link String} representing the tooltip text
     */
    public String getTooltipText() {
        return tooltipText;
    }

    /**
     * Sets the tooltip text.
     * @param tooltipText the {@link String} supplied as {@code tooltipText}
     * @return the {@link BookLine} representing the set tooltip text result
     */
    public BookLine setTooltipText(String tooltipText) {
        this.tooltipText = tooltipText;
        return this;
    }

    /**
     * Returns the item represented by this line.
     * @return the {@link Item} representing the represented item, or {@code null} for a text line
     */
    public Item getItem() {
        return item;
    }

    /** Returns whether the represented recipe is marked as a favorite. */
    public boolean isFavorite() { return favorite; }

    /** Sets the visual favorite state associated with this recipe line. */
    public BookLine setFavorite(boolean favorite) {
        this.favorite = favorite;
        return this;
    }
}
