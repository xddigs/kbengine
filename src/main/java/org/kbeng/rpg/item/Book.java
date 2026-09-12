package org.kbeng.rpg.item;

import org.kbeng.rpg.data.Enchantment;
import org.kbeng.rpg.data.SoundGroup;
import org.kbeng.rpg.data.Usables;
import org.kbeng.engine.ui.BookUI;
import org.kbeng.engine.input.ControlAction;
import org.kbeng.engine.input.Controls;
import org.kbeng.rpg.service.BookService;
import org.kbeng.rpg.service.SoundService;
import org.kbeng.rpg.wrld.GameMaster;

import java.util.ArrayList;
import java.util.List;

/**
 * Book provides book capabilities within the item subsystem.
 * It models inventory-facing objects, equipables, consumables, and item behavior surfaced to gameplay and UI.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends Usable, inheriting shared behavior while specializing subsystem-specific logic.
 */
public class Book extends Usable {
    private final List<Page> pages;
    private int currentPage;
    private boolean isOpen = false;
    private final boolean hasContent;

    /**
     * Creates a new {@code Book} instance.
     * @param hasContent the {@code boolean} supplied as {@code hasContent}
     */
    public Book(boolean hasContent) {
        super(Usables.BOOK, "Book");
        this.pages = new ArrayList<>();
        this.hasContent = hasContent;
    }

    /**
     * Creates a new {@code Book} instance.
     */
    public Book() {
        super(Usables.CRAFTING_BOOK, "Crafting Book");
        this.pages = new ArrayList<>();
        this.hasContent = true;
    }

    /**
     * {@inheritDoc}
     * Handles use and applies its effect to the current interaction state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param isCtrlHeld the {@code boolean} supplied as {@code isCtrlHeld}
     * @return {@code boolean}; the use result
     */
    @Override
    public boolean use(GameMaster gameMaster, boolean isCtrlHeld) {
        if (!hasContent) return false;
        if (isOpen) {
            BookService.bs.close();
        } else {
            BookService.bs.open(this);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * Updates the current state.
     */
    @Override
    public void update() {
        if (Controls.isPressed(ControlAction.PREVIOUS_PAGE)) previousPage();
        if (Controls.isPressed(ControlAction.NEXT_PAGE)) nextPage();
    }


    /**
     * {@inheritDoc}
     * Creates an independent copy that preserves the relevant state of this object.
     * @return the {@link Item} representing the copy result
     */
    @Override
    public Item copy() {
        return new Book(hasContent);
    }

    /**
     * {@inheritDoc}
     * Applies enchanting and updates the affected character or item state.
     * @param enchantment the {@link Enchantment} supplied as {@code enchantment}
     * @return {@code boolean}; the enchanting result
     */
    @Override
    public boolean enchanting(Enchantment enchantment) {
        return false;
    }

    /**
     * Activates this object and prepares any state it requires.
     */
    public void open() {
        isOpen = true;
    }

    /**
     * Releases the resources associated with this object.
     */
    public void close() {
        isOpen = false;
    }

    /**
     * Returns the pages.
     * @return the {@link List} representing the pages
     */
    public List<Page> getPages() {
        return pages;
    }

    /**
     * Adds the page.
     * @param page the {@link Page} supplied as {@code page}
     */
    public void addPage(Page page) {
        pages.add(page);
    }

    /**
     * Returns the page.
     * @param index the {@code int} supplied as {@code index}
     * @return the {@link Page} representing the page
     */
    public Page getPage(int index) {
        return pages.get(index);
    }

    /**
     * Removes the page.
     * @param page the {@link Page} supplied as {@code page}
     */
    public void removePage(Page page) {
        pages.remove(page);
    }

    /**
     * Clears the pages.
     */
    public void clearPages() {
        pages.clear();
    }

    /**
     * Checks whether the closed condition is met.
     * @return {@code true} if closed; otherwise {@code false}
     */
    public boolean isClosed() {
        return !isOpen;
    }

    /**
     * Checks whether the content condition is met.
     * @return {@code true} if content; otherwise {@code false}
     */
    public boolean hasContent() {
        return hasContent;
    }

    /**
     * Returns the current page.
     * @return {@code int}; the current page
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Returns the book to its first page without playing a page-turn animation.
     * This is used after reloading dynamic book contents.
     */
    public void resetCurrentPage() {
        currentPage = 0;
    }

    /**
     * Updates text or selection state for next page.
     */
    public void nextPage() {
        if (hasNextPage()) {
            currentPage += 2;
            SoundService.fx.playUseSound(SoundGroup.BOOKS);
            BookUI.bui.nextPage();
        }
    }

    /**
     * Updates text or selection state for previous page.
     */
    public void previousPage() {
        if (hasPreviousPage()) {
            currentPage -= 2;
            SoundService.fx.playUseSound(SoundGroup.BOOKS);
            BookUI.bui.previousPage();
        }
    }

    /**
     * Updates movement for navigate to according to the current physics and input state.
     * @param targetPage the {@code int} supplied as {@code targetPage}
     */
    public void navigateTo(int targetPage) {
        if (targetPage < 0 || targetPage >= pages.size() || targetPage == currentPage) {
            return;
        }

        if (targetPage % 2 != 0) {
            targetPage--;
        }

        if (targetPage > currentPage) {
            currentPage = targetPage;
            SoundService.fx.playUseSound(SoundGroup.BOOKS);
            BookUI.bui.nextPage();
        } else if (targetPage < currentPage) {
            currentPage = targetPage;
            SoundService.fx.playUseSound(SoundGroup.BOOKS);
            BookUI.bui.previousPage();
        }
    }

    /**
     * Checks whether the next page condition is met.
     * @return {@code true} if next page; otherwise {@code false}
     */
    public boolean hasNextPage() {
        return currentPage + 2 < pages.size();
    }

    /**
     * Checks whether the previous page condition is met.
     * @return {@code true} if previous page; otherwise {@code false}
     */
    public boolean hasPreviousPage() {
        return currentPage - 2 >= 0;
    }

    /**
     * Reloads dynamic book contents.
     */
    public void reload() {}
}
