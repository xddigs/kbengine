package org.kbeng.ui;

import org.kbeng.input.Keyboard;
import org.kbeng.input.Mouse;
import org.kbeng.utils.ItemIds;

/**
 * Represents the uimanager component of the kbengine runtime.
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
public class UIManager {
    private static final float MOUSE_OFFSET = 16.0f;
    private final UIPanel root;
    private final UITooltip tooltip;
    private UIElement focusedElement;

    /**
     * Creates a new {@code UIManager} instance.
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     */
    public UIManager(float width, float height) {
        root = new UIPanel(0.0f, 0.0f, width, height);
        tooltip = new UITooltip();
        tooltip.setInteractable(false);
        tooltip.setLayer(Integer.MAX_VALUE);
        root.addChild(tooltip);
    }

    /**
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void update(float delta) {
        root.update(delta);
        root.mouseMoved(Mouse.getX(), Mouse.getY());

        UIElement hovered = root.findElementAt(Mouse.getX(), Mouse.getY());
        Mouse.setCursorHovered(hovered != null && hovered != root && hovered != tooltip);

        if (hovered != null && hovered != tooltip
                && hovered.getTooltipText() != null
                && !hovered.getTooltipText().isBlank()) {
            String cornerText = hovered instanceof InventorySlotUI slot ? slot.getTooltipCornerText() : null;
            String tooltipText = hovered instanceof InventorySlotUI slot ?
                    ItemIds.appendToTooltip(hovered.getTooltipText(), slot.getItem()) : hovered.getTooltipText();
            tooltip.cornerText(cornerText).text(tooltipText);
            float cursorX = Mouse.getX() + MOUSE_OFFSET + MOUSE_OFFSET / 2;
            float cursorY = Mouse.getY() - MOUSE_OFFSET / 2;
            tooltip.updatePosition(
                    cursorX,
                    cursorY,
                    root.getWidth(),
                    root.getHeight());
            tooltip.show();
        } else {
            tooltip.hide();
            Frontend.setWasCursorIconDrawn(false);
        }

        for (int button = Mouse.BUTTON_1;
             button <= Mouse.BUTTON_LAST;
             button++) {

            if (Mouse.isButtonPressed(button)) {
                UIElement element = root.findElementAt(Mouse.getX(), Mouse.getY());

                if (element != null && element.isFocusable()) {
                    setFocusedElement(element);
                } else {
                    clearFocus();
                }
                root.mousePressed(Mouse.getX(), Mouse.getY(), button);
            }

            if (Mouse.isButtonReleased(button)) {
                root.mouseReleased(Mouse.getX(), Mouse.getY(), button);
            }
        }

        if (Mouse.getScrollY() != 0.0f) {
            root.mouseScrolled(Mouse.getX(), Mouse.getY(), 0.0f, Mouse.getScrollY());
        }

        if (focusedElement != null) {
            for (int key = Keyboard.KEY_SPACE;
                 key <= Keyboard.KEY_LAST;
                 key++) {

                if (Keyboard.isKeyPressed(key)) {
                    focusedElement.keyPressed(key, 0, Keyboard.getModifiers());
                }

                if (Keyboard.isKeyReleased(key)) {
                    focusedElement.keyReleased(key, 0, 0);
                }
            }

            String typedCharacters = Keyboard.getTypedCharacters();
            for (int i = 0; i < typedCharacters.length();) {
                int codePoint = typedCharacters.codePointAt(i);
                if (focusedElement.charTyped(codePoint)) {
                    break;
                }
                i += Character.charCount(codePoint);
            }
            if (!focusedElement.isFocused()) {
                clearFocus();
            }
        } else {
            Keyboard.getTypedCharacters();
        }
    }

    /**
     * Renders this object in the requested render pass.
     */
    public void render() {
        Frontend.render(root);
    }

    /**
     * Returns the root.
     * @return the {@link UIPanel} representing the root
     */
    public UIPanel getRoot() {
        return root;
    }

    /**
     * Returns the tooltip.
     * @return the {@link UITooltip} representing the tooltip
     */
    public UITooltip getTooltip() {
        return tooltip;
    }

    /**
     * Activates tooltip and prepares any state it requires.
     * @param text the {@link String} supplied as {@code text}
     * @param mouseX the {@code float} supplied as {@code mouseX}
     * @param mouseY the {@code float} supplied as {@code mouseY}
     */
    public void showTooltip(String text, float mouseX, float mouseY) {
        if (text == null || text.isBlank()) {
            tooltip.hide();
            return;
        }

        tooltip.text(text);
        tooltip.updatePosition(mouseX, mouseY, root.getWidth(), root.getHeight());
        tooltip.show();
    }

    /**
     * Deactivates tooltip and releases its transient state.
     */
    public void hideTooltip() {
        tooltip.hide();
    }

    /**
     * Returns the focused element.
     * @return the {@link UIElement} representing the focused element
     */
    public UIElement getFocusedElement() {
        return focusedElement;
    }

    /**
     * Sets the focused element.
     * @param element the {@link UIElement} supplied as {@code element}
     */
    public void setFocusedElement(UIElement element) {
        if (focusedElement == element) {
            return;
        }

        if (focusedElement != null) {
            focusedElement.setFocused(false);
        }

        focusedElement = null;

        if (element != null
                && element.isFocusable()
                && element.isActuallyVisible()
                && element.isActuallyEnabled()) {

            focusedElement = element;
            focusedElement.setFocused(true);
        }
    }

    /**
     * Clears the focus.
     */
    public void clearFocus() {
        setFocusedElement(null);
    }

    /**
     * Transforms this object according to the supplied values.
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     */
    public void resize(float width, float height) {
        root.setSize(width, height);
        Frontend.resize((int) width, (int) height);
    }
}
