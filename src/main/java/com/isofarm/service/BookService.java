package com.isofarm.service;

import com.isofarm.data.Singleton;
import com.isofarm.ui.BookUI;
import com.isofarm.item.Book;

/**
 * Encapsulates the state and operations required by book service within the game runtime.
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
        // TODO toggle recipes that can only be crafted,
        //  whilst the alternative is showing all recipes
    }
}