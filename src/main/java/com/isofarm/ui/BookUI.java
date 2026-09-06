package com.isofarm.ui;

import com.isofarm.data.BookLine;
import com.isofarm.data.Singleton;
import com.isofarm.graphics.ResourceManager;
import com.isofarm.graphics.SpriteSheet;
import com.isofarm.input.ControlAction;
import com.isofarm.input.Controls;
import com.isofarm.input.Mouse;
import com.isofarm.item.Book;
import com.isofarm.item.CraftingBook;
import com.isofarm.item.Item;
import com.isofarm.item.Page;
import com.isofarm.service.BookService;
import com.isofarm.utils.K;
import com.isofarm.utils.Settings;
import org.joml.Vector4f;
import org.lwjgl.stb.STBTTBakedChar;

/**
 * Encapsulates the state and operations required by book ui within the game runtime.
 */
@Singleton
public class BookUI extends UIElement {
    private static final float ANIMATION_DURATION = 0.35f;
    private static final float PAGE_FLIP_DURATION = 0.4f;
    private static final int TOTAL_ANIM_FRAMES = 16;
    private static final int GRID_COLUMNS = 4;
    private static final int GRID_ROWS = 4;
    private static final float GRID_ICON_SIZE = Settings.getScaledSlot();
    private static final float GRID_GAP = 16.0f;
    private static final float GRID_OUTLINE_SIZE = 3.0f;
    private static final float PAGE_CONTENT_CURVE = 18.0f;
    private static final float PAGE_STATIC_CONTENT_HIDE_PROGRESS = 0.99f;
    private static final float BASE_CONTENT_HEIGHT_OFFSET = 0.15f;
    private static final float MOUSE_OFFSET = 32.0f;
    private static final float BUTTON_SIZE = Settings.getScaledSlot();
    private static final float BUTTON_GAP = 10.0f;
    private static final float BUTTON_TOP_PADDING = 96.0f;
    private static final float LEFT_PAGE_CONTENT_INSET = 0.154f;
    private static final float RIGHT_PAGE_CONTENT_INSET = 0.038f;
    private static final float PAGE_CONTENT_WIDTH = 0.808f;
    private static final float PAGE_CONTENT_TOP = 0.154f;
    private static final float PAGE_CONTENT_HEIGHT = 0.692f;
    private static final float PAGE_TEXT_INSET = 0.092f;

    public static BookUI bui;
    private float animationProgress = 0.0f;

    private boolean isOpening = false;
    private boolean isClosing = false;

    private boolean isFlippingPage = false;
    private boolean isFlippingNext = true;

    private float pageFlipTimer = 0.0f;

    private BookLine hoveredBookLine;
    private final UIButton sortNameButton;
    private final UIButton sortTypeButton;
    private final UIButton homeCraftingsButton;
    private final UIButton closeButton;

    /**
     * Creates a new {@code BookUI} instance.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     */
    public BookUI(float x, float y, float width, float height) {
        super(x, y, width, height);
        sortNameButton = createButton(ResourceManager.rem.getBookSortNameIcon(), "book.sort_name", () -> sortCraftingBook(true));
        sortTypeButton = createButton(ResourceManager.rem.getBookSortTypeIcon(), "book.sort_type", () -> sortCraftingBook(false));
        homeCraftingsButton = createButton(ResourceManager.rem.getBookHomeCraftings(), "book.toggle_recipes", BookService.bs::toggleRecipes);
        closeButton = createButton(ResourceManager.rem.getBookCloseIcon(), "book.close", BookService.bs::close);

        addChild(sortNameButton);
        addChild(sortTypeButton);
        addChild(homeCraftingsButton);
        addChild(closeButton);
        layoutButtons(width);
        hide();
    }

    private UIButton createButton(SpriteSheet icon, String tooltip, Runnable action) {
        UIButton button = new UIButton(0.0f, 0.0f, BUTTON_SIZE, BUTTON_SIZE)
                .setOnClick(action)
                .setCanDrawBackground(false);
        button.setSpriteSheet(icon);
        button.setTooltipText(tooltip);
        return button;
    }

    private void layoutButtons(float bookWidth) {
        float totalWidth = BUTTON_SIZE * 4.0f + BUTTON_GAP * 3.0f;
        float extraOffset = 10.0f;
        float startX = bookWidth - K.UI.UI_BOOK_PADDING_X - totalWidth - extraOffset;
        sortNameButton.setPosition(startX, BUTTON_TOP_PADDING);
        sortTypeButton.setPosition(startX + BUTTON_SIZE + BUTTON_GAP, BUTTON_TOP_PADDING);
        homeCraftingsButton.setPosition(startX + (BUTTON_SIZE + BUTTON_GAP) * 2.0f, BUTTON_TOP_PADDING);
        closeButton.setPosition(startX + (BUTTON_SIZE + BUTTON_GAP) * 3.0f, BUTTON_TOP_PADDING);
    }

    private void sortCraftingBook(boolean byName) {
        if (!isOpen() || !(BookService.bs.getOpenedBook()
                instanceof CraftingBook book)) {
            return;
        }

        if (byName) {
            book.sortByName();
        } else {
            book.sortByType();
        }
        hoveredBookLine = null;
    }

    /**
     * Initializes the component.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     */
    public static void init(float x, float y, float width, float height) {
        if (bui != null) return;
        bui = new BookUI(x, y, width, height);
    }

    /**
     * Transforms in out cubic according to the supplied values.
     * @param t the {@code float} supplied as {@code t}
     * @return {@code float}; the ease in out cubic result
     */
    private static float easeInOutCubic(float t) {
        if (t < 0.5f) {
            return 4.0f * t * t * t;
        }

        return 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0f) / 2.0f;
    }

    /**
     * Transforms this object according to the supplied values.
     * @param start the {@code float} supplied as {@code start}
     * @param end the {@code float} supplied as {@code end}
     * @param t the {@code float} supplied as {@code t}
     * @return {@code float}; the lerp result
     */
    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    /**
     * {@inheritDoc}
     * Renders this object in the requested render pass.
     */
    @Override
    public void render() {
    }

    /**
     * Activates this object and prepares any state it requires.
     */
    public void open() {
        isClosing = false;
        isOpening = true;
        show();
        updateButtonState();
    }

    /**
     * Releases the resources associated with this object.
     */
    public void close() {
        if (isClosing) return;

        isClosing = true;
        isOpening = false;
        updateButtonState();
    }

    /**
     * Checks whether the closed condition is met.
     * @return {@code true} if closed; otherwise {@code false}
     */
    public boolean isClosed() {
        return !isOpening && !isClosing && animationProgress <= 0.0f;
    }

    /**
     * Checks whether the animating condition is met.
     * @return {@code true} if animating; otherwise {@code false}
     */
    public boolean isAnimating() {
        return isOpening || isClosing;
    }

    /**
     * Checks whether the open condition is met.
     * @return {@code true} if open; otherwise {@code false}
     */
    public boolean isOpen() {
        return !isOpening && !isClosing && animationProgress >= 1.0f;
    }

    /**
     * Updates the current state.
     * @param book the {@link Book} supplied as {@code book}
     * @param animSheet the {@link SpriteSheet} supplied as {@code animSheet}
     */
    public void update(Book book, SpriteSheet animSheet) {
        if (book == null || !isOpen() || isFlippingPage
                || book.getPages().isEmpty() || animSheet == null) {
            hoveredBookLine = null;
            GameUIService.ui
                    .getUIManager()
                    .hideTooltip();
            return;
        }

        if (isAnyButtonHovered()) {
            hoveredBookLine = null;
            return;
        }

        updateBookLine(animSheet, book);
        if (Controls.isPressed(ControlAction.UI_SELECT)) {
            click();
        }
    }

    /**
     * Updates the book line.
     * @param animSheet the {@link SpriteSheet} supplied as {@code animSheet}
     * @param book the {@link Book} supplied as {@code book}
     */
    private void updateBookLine(SpriteSheet animSheet, Book book) {
        float screenWidth = Frontend.getScreenWidth();
        float screenHeight = Frontend.getScreenHeight();

        float scale = 2.0f;

        float bookWidth = animSheet.getFrameWidth() * scale;
        float bookHeight = animSheet.getFrameHeight() * scale;

        float centerX = (screenWidth - bookWidth) * 0.5f;
        float centerY = (screenHeight - bookHeight) * 0.5f;

        float easedProgress = easeInOutCubic(animationProgress);
        float y = lerp(screenHeight, centerY, easedProgress);

        float lineHeight = Frontend.getNormalFont().getSize();

        setTooltipText(null);
        GameUIService.ui
                .getUIManager()
                .hideTooltip();

        hoveredBookLine = null;
        int leftPageIndex = book.getCurrentPage();
        float pageWidth = bookWidth / 2.0f;

        if (leftPageIndex < book.getPages().size()) {
            checkHover(book.getPage(leftPageIndex), centerX, y,
                    pageWidth, bookHeight, lineHeight);
        }

        int rightPageIndex = leftPageIndex + 1;
        if (hoveredBookLine == null && rightPageIndex < book.getPages().size()) {
            checkHover(book.getPage(rightPageIndex), centerX + pageWidth, y,
                    pageWidth, bookHeight, lineHeight);
        }
    }

    /**
     * Determines whether hover is satisfied by the current state.
     * @param page the {@link Page} supplied as {@code page}
     * @param pageX the {@code float} supplied as {@code pageX}
     * @param pageY the {@code float} supplied as {@code pageY}
     * @param pageWidth the {@code float} supplied as {@code pageWidth}
     * @param pageHeight the {@code float} supplied as {@code pageHeight}
     * @param lineHeight the {@code float} supplied as {@code lineHeight}
     */
    private void checkHover(Page page, float pageX, float pageY,
                            float pageWidth, float pageHeight,
                            float lineHeight) {
        if (hasItemIcons(page)) {
            checkGridHover(page, pageX, pageY, pageWidth, pageHeight);
            return;
        }

        float textX = getPageTextX(pageX, pageWidth);
        float textY = getPageContentTop(pageY, pageHeight);
        for (BookLine bookLine : page.getLines()) {
            if (bookLine.isInteractive() && isMouseHovering(textX, textY, bookLine.getText())) {
                hoveredBookLine = bookLine;
                float mouseX = Mouse.getX() + MOUSE_OFFSET + MOUSE_OFFSET / 2;
                float mouseY = Mouse.getY() - MOUSE_OFFSET / 2;
                GameUIService.ui.getUIManager().showTooltip(bookLine.getTooltipText(),
                        mouseX, mouseY);
                break;
            }
            textY += lineHeight;
        }
    }

    /**
     * Checks which recipe icon in a page grid is under the mouse.
     */
    private void checkGridHover(Page page, float pageX, float pageY,
                                float pageWidth, float pageHeight) {
        float gridWidth = getGridWidth();
        float gridHeight = getGridHeight();
        float gridX = getPageContentX(pageX, pageWidth)
                + (getPageContentWidth(pageWidth) - gridWidth) * 0.5f;
        float gridY = getPageContentTop(pageY, pageHeight)
                + (getPageContentHeight(pageHeight) - gridHeight) * 0.5f;

        int count = Math.min(page.getLines().size(), GRID_COLUMNS * GRID_ROWS);
        for (int index = 0; index < count; index++) {
            BookLine bookLine = page.getLine(index);
            if (!bookLine.isInteractive() || bookLine.getItem() == null) continue;

            int column = index % GRID_COLUMNS;
            int row = index / GRID_COLUMNS;
            float iconX = gridX + column * (GRID_ICON_SIZE + GRID_GAP);
            float iconY = gridY + row * (GRID_ICON_SIZE + GRID_GAP);
            if (isMouseHovering(iconX, iconY, GRID_ICON_SIZE, GRID_ICON_SIZE)) {
                hoveredBookLine = bookLine;
                float mouseX = Mouse.getX() + MOUSE_OFFSET + MOUSE_OFFSET / 2;
                float mouseY = Mouse.getY() - MOUSE_OFFSET / 2;
                GameUIService.ui.getUIManager().showTooltip(
                        bookLine.getTooltipText(), mouseX, mouseY);
                return;
            }
        }
    }

    /**
     * Renders this object in the requested render pass.
     * @param book the {@link Book} supplied as {@code book}
     * @param delta the {@code float} supplied as {@code delta}
     * @param animSheet the {@link SpriteSheet} supplied as {@code animSheet}
     */
    public void render(Book book, float delta, SpriteSheet animSheet) {
        if (book == null || animSheet == null) return;
        float screenWidth = Frontend.getScreenWidth();
        float screenHeight = Frontend.getScreenHeight();

        float scale = 2.0f;
        float bookWidth = animSheet.getFrameWidth() * scale;
        float bookHeight = animSheet.getFrameHeight() * scale;

        float centerX = (screenWidth - bookWidth) * 0.5f;
        float centerY = (screenHeight - bookHeight) * 0.5f;

        updateAnimation(delta);
        float alpha = easeInOutCubic(animationProgress);
        float y = lerp(screenHeight, centerY, alpha);

        setPosition(centerX, y);
        setSize(bookWidth, bookHeight);
        setOpacity(alpha);
        layoutButtons(bookWidth);
        updateButtonState();

        Vector4f color = new Vector4f(0.8706f, 0.8196f, 0.6745f, 1.0f);
        Frontend.drawRect(centerX, y + BASE_CONTENT_HEIGHT_OFFSET + BASE_CONTENT_HEIGHT_OFFSET/2f,
                bookWidth, bookHeight + BASE_CONTENT_HEIGHT_OFFSET, color);

        if (isFlippingPage) {
            pageFlipTimer += delta;
            float progress = Math.min(1.0f, pageFlipTimer / PAGE_FLIP_DURATION);
            float animFrameProgress = isFlippingNext ? progress : (1.0f - progress);
            int currentFrame = (int) (animFrameProgress * (TOTAL_ANIM_FRAMES - 1));
            renderFlippingSpread(book, centerX, y, bookWidth, bookHeight, alpha, progress);
            Frontend.drawSprite(animSheet, currentFrame, centerX, y, bookWidth, bookHeight, new Vector4f(1.0f));

            if (progress >= 1.0f) {
                isFlippingPage = false;
            }

        } else {
            renderSpread(book, centerX, y, animSheet, scale, alpha);
            Frontend.drawSprite(animSheet, 0, centerX, y, bookWidth, bookHeight, new Vector4f(1.0f));
        }

        // Buttons belong to the book, not to a particular page. Rendering them
        // here keeps all four controls available when the recipe list is empty.
        renderBookButtons();
    }

    private boolean isAnyButtonHovered() {
        return sortNameButton.isHovered() || sortTypeButton.isHovered()
                || homeCraftingsButton.isHovered() || closeButton.isHovered();
    }

    private void updateButtonState() {
        Book openedBook = BookService.bs.getOpenedBook();
        boolean bookIsReady = isOpen() && !isFlippingPage;
        boolean isCraftingBook = openedBook instanceof CraftingBook;
        // Recipe controls stay visible even when no recipe currently matches the filter.
        setButtonState(sortNameButton, isCraftingBook, bookIsReady && isCraftingBook);
        setButtonState(sortTypeButton, isCraftingBook, bookIsReady && isCraftingBook);
        setButtonState(homeCraftingsButton, isCraftingBook,
                isOpen() && !isFlippingPage && isCraftingBook);
        homeCraftingsButton.setSpriteColumn(isCraftingBook
                && ((CraftingBook) openedBook).isShowingOnlyCraftableRecipes() ? 1 : 0);
        setButtonState(closeButton, openedBook != null, bookIsReady);
    }

    /**
     * Updates the crafting-filter icon after the player toggles the recipe list.
     * @param onlyCraftableRecipes whether the craftable-only filter is active
     */
    public void setCraftableRecipesSelected(boolean onlyCraftableRecipes) {
        homeCraftingsButton.setSpriteColumn(onlyCraftableRecipes ? 1 : 0);
    }

    private void setButtonState(UIButton button, boolean visible, boolean enabled) {
        button.setVisible(visible);
        button.setEnabled(enabled);
    }

    private void renderBookButtons() {
        renderChildren();
    }

    /**
     * Keeps page content attached to the animated sheet while it folds around
     * the spine. The outgoing page contracts and the incoming page unfolds.
     */
    private void renderFlippingSpread(Book book, float bookX, float bookY,
                                      float bookWidth, float bookHeight,
                                      float alpha, float progress) {
        float pageWidth = bookWidth / 2.0f;
        float spineX = bookX + pageWidth;
        float foldCurve = (float) Math.sin(progress * Math.PI) * PAGE_CONTENT_CURVE;
        int currentPage = book.getCurrentPage();

        if (isFlippingNext) {
            int oldLeft = currentPage - 2;
            int oldRight = currentPage - 1;
            if (progress < PAGE_STATIC_CONTENT_HIDE_PROGRESS) {
                renderPageAt(book, oldLeft, bookX, bookY,
                        pageWidth, bookHeight, alpha);
            }
            if (progress < 0.5f) {
                float fold = easeInOutCubic(progress * 2.0f);
                renderTransformedPage(book, oldRight, bookX + pageWidth, bookY,
                        pageWidth, bookHeight, alpha, spineX, 1.0f - fold, foldCurve);
            } else {
                float unfold = easeInOutCubic((progress - 0.5f) * 2.0f);
                renderPageAt(book, currentPage + 1, bookX + pageWidth, bookY,
                        pageWidth, bookHeight, alpha);
                renderTransformedPage(book, currentPage, bookX, bookY,
                        pageWidth, bookHeight, alpha, spineX, unfold, foldCurve);
            }
        } else {
            int oldLeft = currentPage + 2;
            int oldRight = currentPage + 3;
            if (progress < PAGE_STATIC_CONTENT_HIDE_PROGRESS) {
                renderPageAt(book, oldRight, bookX + pageWidth, bookY,
                        pageWidth, bookHeight, alpha);
            }
            if (progress < 0.5f) {
                float fold = easeInOutCubic(progress * 2.0f);
                renderTransformedPage(book, oldLeft, bookX, bookY,
                        pageWidth, bookHeight, alpha, spineX, 1.0f - fold, foldCurve);
            } else {
                float unfold = easeInOutCubic((progress - 0.5f) * 2.0f);
                renderPageAt(book, currentPage, bookX, bookY,
                        pageWidth, bookHeight, alpha);
                renderTransformedPage(book, currentPage + 1, bookX + pageWidth, bookY,
                        pageWidth, bookHeight, alpha, spineX, unfold, foldCurve);
            }
        }
    }

    /**
     * Renders a page when its index exists.
     */
    private void renderPageAt(Book book, int pageIndex, float pageX, float pageY,
                              float pageWidth, float pageHeight, float alpha) {
        if (pageIndex < 0 || pageIndex >= book.getPages().size()) return;
        renderPage(book.getPage(pageIndex), pageX, pageY, pageWidth, pageHeight, alpha);
    }

    /**
     * Renders one page with the temporary shader fold enabled.
     */
    private void renderTransformedPage(Book book, int pageIndex,
                                       float pageX, float pageY,
                                       float pageWidth, float pageHeight, float alpha,
                                       float spineX, float scaleX, float curve) {
        if (pageIndex < 0 || pageIndex >= book.getPages().size()) return;
        Frontend.beginPageTransform(spineX, scaleX, pageWidth, curve);
        renderPage(book.getPage(pageIndex), pageX, pageY, pageWidth, pageHeight, alpha);
        Frontend.endPageTransform();
    }

    /**
     * Updates the animation.
     * @param delta the {@code float} supplied as {@code delta}
     */
    private void updateAnimation(float delta) {
        float amount = delta / ANIMATION_DURATION;

        if (isOpening) {
            animationProgress += amount;

            if (animationProgress >= 1.0f) {
                animationProgress = 1.0f;
                isOpening = false;
            }
        }

        if (isClosing) {
            animationProgress -= amount;

            if (animationProgress <= 0.0f) {
                animationProgress = 0.0f;
                isClosing = false;
                hide();
            }
        }
    }

    /**
     * Updates text or selection state for next page.
     */
    public void nextPage() {
        if (isFlippingPage) return;

        isFlippingPage = true;
        isFlippingNext = true;
        pageFlipTimer = 0.0f;
    }

    /**
     * Updates text or selection state for previous page.
     */
    public void previousPage() {
        if (isFlippingPage) return;

        isFlippingPage = true;
        isFlippingNext = false;
        pageFlipTimer = 0.0f;
    }

    /**
     * Renders the spread.
     * @param book the {@link Book} supplied as {@code book}
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param animSheet the {@link SpriteSheet} supplied as {@code animSheet}
     * @param scale the {@code float} supplied as {@code scale}
     * @param alpha the {@code float} supplied as {@code alpha}
     */
    private void renderSpread(Book book, float x, float y, SpriteSheet animSheet, float scale, float alpha) {
        if (book.getPages().isEmpty() || alpha <= 0.0f) return;
        float bookWidth = animSheet.getFrameWidth() * scale;
        float bookHeight = animSheet.getFrameHeight() * scale;
        float pageWidth = bookWidth / 2.0f;

        int leftPageIndex = book.getCurrentPage();
        if (leftPageIndex < book.getPages().size()) {
            renderPage(book.getPage(leftPageIndex), x, y, pageWidth, bookHeight, alpha);
        }

        int rightPageIndex = leftPageIndex + 1;
        if (rightPageIndex < book.getPages().size()) {
            renderPage(book.getPage(rightPageIndex), x + pageWidth, y,
                    pageWidth, bookHeight, alpha);
        }
    }

    /**
     * Renders the page.
     * @param page the {@link Page} supplied as {@code page}
     * @param pageX the {@code float} supplied as {@code pageX}
     * @param pageY the {@code float} supplied as {@code pageY}
     * @param pageWidth the {@code float} supplied as {@code pageWidth}
     * @param pageHeight the {@code float} supplied as {@code pageHeight}
     * @param alpha the {@code float} supplied as {@code alpha}
     */
    private void renderPage(Page page, float pageX, float pageY,
                            float pageWidth, float pageHeight, float alpha) {
        if (hasItemIcons(page)) {
            renderItemGrid(page, pageX, pageY, pageWidth, pageHeight, alpha);
            return;
        }

        float textX = getPageTextX(pageX, pageWidth);
        float textY = getPageContentTop(pageY, pageHeight);
        float lineHeight = Frontend.getNormalFont().getSize();
        for (BookLine bookLine : page.getLines()) {
            String renderText = bookLine.getText();
            if (renderText.startsWith("-")) {
                renderText = renderText.replace("-", "");
            }

            boolean isHovered = bookLine == hoveredBookLine;
            Vector4f baseColor = isHovered ? new Vector4f(0.1f, 0.4f, 0.9f, 1.0f) : K.UI.UI_BOOK_TEXT_COLOR;
            Vector4f finalColor = new Vector4f(baseColor.x, baseColor.y, baseColor.z, baseColor.w * alpha);

            if (isHovered) {
                float cursorOffset = 24.0f;
                Frontend.drawNormalString(">", textX - cursorOffset, textY, finalColor);
            }
            if (renderText.startsWith("**")) {
                renderText = renderText.replace("**", "");
            }
            Frontend.drawNormalString(renderText, textX, textY, finalColor);
            textY += lineHeight;
        }
    }

    /**
     * Renders up to sixteen recipe results in a centered 4x4 grid.
     */
    private void renderItemGrid(Page page, float pageX, float pageY,
                                float pageWidth, float pageHeight, float alpha) {
        float gridX = getPageContentX(pageX, pageWidth)
                + (getPageContentWidth(pageWidth) - getGridWidth()) * 0.5f;
        float gridY = getPageContentTop(pageY, pageHeight)
                + (getPageContentHeight(pageHeight) - getGridHeight()) * 0.5f;
        int count = Math.min(page.getLines().size(), GRID_COLUMNS * GRID_ROWS);

        for (int index = 0; index < count; index++) {
            BookLine bookLine = page.getLine(index);
            Item item = bookLine.getItem();
            if (item == null) continue;

            SpriteSheet spriteSheet = ResourceManager.getItemSpriteSheet(item);
            if (spriteSheet == null) continue;

            int column = index % GRID_COLUMNS;
            int row = index / GRID_COLUMNS;
            float iconX = gridX + column * (GRID_ICON_SIZE + GRID_GAP);
            float iconY = gridY + row * (GRID_ICON_SIZE + GRID_GAP);
            int frame = ResourceManager.getItemFrame(item);

            if (bookLine == hoveredBookLine) {
                Frontend.drawSpriteOutline(spriteSheet, frame, iconX, iconY,
                        GRID_ICON_SIZE, GRID_ICON_SIZE, GRID_OUTLINE_SIZE,
                        new Vector4f(1.0f, 1.0f, 1.0f, alpha));
            }

            Frontend.drawSprite(spriteSheet, frame, iconX, iconY,
                    GRID_ICON_SIZE, GRID_ICON_SIZE, new Vector4f(1.0f, 1.0f, 1.0f, alpha));
        }
    }

    private boolean hasItemIcons(Page page) {
        return page.getLines().stream().anyMatch(line -> line.getItem() != null);
    }

    private float getGridWidth() {
        return GRID_COLUMNS * GRID_ICON_SIZE + (GRID_COLUMNS - 1) * GRID_GAP;
    }

    private float getGridHeight() {
        return GRID_ROWS * GRID_ICON_SIZE + (GRID_ROWS - 1) * GRID_GAP;
    }

    private float getPageContentX(float pageX, float pageWidth) {
        float inset = isRightPage(pageX, pageWidth)
                ? RIGHT_PAGE_CONTENT_INSET : LEFT_PAGE_CONTENT_INSET;
        return pageX + pageWidth * inset;
    }

    private float getPageContentWidth(float pageWidth) {
        return pageWidth * PAGE_CONTENT_WIDTH;
    }

    private float getPageContentTop(float pageY, float pageHeight) {
        return pageY + pageHeight * PAGE_CONTENT_TOP;
    }

    private float getPageContentHeight(float pageHeight) {
        return pageHeight * PAGE_CONTENT_HEIGHT;
    }

    private float getPageTextX(float pageX, float pageWidth) {
        return getPageContentX(pageX, pageWidth) + pageWidth * PAGE_TEXT_INSET;
    }

    private boolean isRightPage(float pageX, float pageWidth) {
        float bookLeft = (Frontend.getScreenWidth() - pageWidth * 2.0f) * 0.5f;
        return pageX >= bookLeft + pageWidth * 0.5f;
    }

    /**
     * Checks whether the mouse hovering condition is met.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param text the {@link String} supplied as {@code text}
     * @return {@code true} if mouse hovering; otherwise {@code false}
     */
    public boolean isMouseHovering(float x, float y, String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        float mouseX = Mouse.getX();
        float mouseY = Mouse.getY();
        String cleanText = text.replace("**", "").replace("-", "").trim();

        if (cleanText.isEmpty()) {
            return false;
        }

        UIFont font = Frontend.getNormalFont();
        float width = Frontend.getStringWidth(cleanText, font);
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int i = 0; i < cleanText.length(); ) {
            int codePoint = cleanText.codePointAt(i);
            STBTTBakedChar glyph = font.getGlyph(codePoint);
            if (glyph != null) {
                float glyphTop = y + glyph.yoff();
                float glyphBottom = glyphTop + (glyph.y1() - glyph.y0());
                minY = Math.min(minY, glyphTop);
                maxY = Math.max(maxY, glyphBottom);
            }
            i += Character.charCount(codePoint);
        }

        if (minY == Float.MAX_VALUE) {
            return false;
        }

        float horizontalPadding = 6.0f;
        float verticalPadding = 2.0f;
        return mouseX >= x - horizontalPadding && mouseX <= x + width + horizontalPadding
                && mouseY >= minY - verticalPadding && mouseY <= maxY + verticalPadding;
    }

    /**
     * Checks whether the mouse is inside a rectangular icon.
     */
    private boolean isMouseHovering(float x, float y, float width, float height) {
        float mouseX = Mouse.getX();
        float mouseY = Mouse.getY();
        return mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    /**
     * Handles click and applies its effect to the current interaction state.
     */
    private void click() {
        if (hoveredBookLine == null || !hoveredBookLine.isInteractive()) {
            return;
        }

        hoveredBookLine.click();
    }

    /**
     * Reloads this object from its authoritative source.
     * @param openedBook the {@link Book} supplied as {@code openedBook}
     */
    public void reload(Book openedBook) {
        openedBook.reload();
    }
}
