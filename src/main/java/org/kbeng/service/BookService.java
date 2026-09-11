package org.kbeng.service;

import org.kbeng.data.Singleton;
import org.kbeng.ui.BookUI;
import org.kbeng.item.Book;
import org.kbeng.item.CraftingBook;

/**
 * Represents the book service component of the Isofarm runtime.
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
@Singleton
public class BookService implements Service<Book> {
    public static final BookService bs = new BookService();
    private Book openedBook;

    /**
     * Creates a new {@code BookService} instance.
     */
    private BookService() {}

    /**
     * Returns the opened book.
     * @return the {@link Book} representing the opened book
     */
    public Book getOpenedBook() {
        return openedBook;
    }

    /**
     * Checks whether the open condition is met.
     * @return {@code true} if open; otherwise {@code false}
     */
    public boolean isOpen() {
        return openedBook != null;
    }

    /**
     * Activates this object and prepares any state it requires.
     * @param book the {@link Book} supplied as {@code book}
     */
    public void open(Book book) {
        if (book == null || openedBook != null) {
            return;
        }

        openedBook = book;
        book.open();
        BookUI.bui.open();
    }

    /**
     * Releases the resources associated with this object.
     */
    public void close() {
        if (openedBook == null) {
            return;
        }
        BookUI.bui.close();
    }

    /**
     * Updates the current state.
     */
    public void update() {
        if (openedBook != null && !BookUI.bui.isAnimating()) {
            openedBook.update();
        }

        if (BookUI.bui.isClosed()) {
            if (openedBook != null) {
                openedBook.close();
            }
            openedBook = null;
        }
    }

    public void toggleRecipes() {
        if (!(openedBook instanceof CraftingBook craftingBook)) {
            return;
        }

        craftingBook.toggleCraftableRecipes();
        if (BookUI.bui != null) {
            BookUI.bui.setCraftableRecipesSelected(
                    craftingBook.isShowingOnlyCraftableRecipes());
        }
    }

    /**
     * Reloads the active crafting book after the player's available materials change.
     */
    public void reloadOpenCraftingBook() {
        if (openedBook instanceof CraftingBook craftingBook) {
            craftingBook.reload();
        }
    }
}
