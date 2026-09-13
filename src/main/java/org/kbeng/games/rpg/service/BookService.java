package org.kbeng.games.rpg.service;

import org.kbeng.engine.service.Service;
import org.kbeng.games.rpg.data.Singleton;
import org.kbeng.games.rpg.ui.BookUI;
import org.kbeng.games.rpg.item.Book;
import org.kbeng.games.rpg.item.CraftingBook;

/**
 * BookService provides book service capabilities within the service subsystem.
 * It provides shared runtime services, registries, and policy logic consumed by orchestrators and feature modules.
 * The service acts as a shared policy and state access point for other runtime modules.
 * It implements Service<Book>, providing a concrete strategy for this subsystem contract.
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
