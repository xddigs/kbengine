package com.isofarm.ui;

import com.isofarm.utils.K;
import com.isofarm.utils.Local;
import com.isofarm.utils.Settings;
import org.joml.Vector4f;

import java.util.Arrays;

/**
 * Encapsulates the state and operations required by uitooltip within the game runtime.
 */
@SuppressWarnings("unused")
public class UITooltip extends UIElement {
    private static final float OFFSET_PADDING = 2.2f;
    private String text = "";
    private String cornerText = "";
    private float padding = Settings.getScaledPadding();
    private float offsetX = Settings.getScaledSpacing();
    private float offsetY = Settings.getScaledSpacing();

    /**
     * Creates a new {@code UITooltip} instance.
     */
    public UITooltip() {
        super(0.0f, 0.0f, 0.0f, 0.0f);
        setZIndex(Integer.MAX_VALUE);
        setInteractable(false);
        hide();
    }

    /**
     * {@inheritDoc}
     * Renders this object in the requested render pass.
     */
    @Override
    public void render() {
        if (!isActuallyVisible() || text == null || text.isBlank()) return;

        Frontend.drawRect(getAbsoluteX(), getAbsoluteY(), getAbsoluteWidth(), getAbsoluteHeight(),
                K.UI.UI_BACKGROUND_COLOR, Settings.getScaledCornerRadius(),
                K.UI.UI_BORDER_COLOR, Settings.getScaledThickness());

        UIFont font = Frontend.getNormalFont();
        float lineHeight = font.getSize();
        String[] lines = text.split("\n", -1);
        float textY = getAbsoluteY() + padding * 2.2f;
        for (String line : lines) {
            Vector4f color = Arrays.stream(lines).toList().getLast().equals(line) ?
                    new Vector4f(0.6f) : K.UI.UI_TEXT_COLOR;

            Frontend.drawString(line, getAbsoluteX() + padding, textY, font, color);
            textY += lineHeight;
        }
        if (!cornerText.isBlank()) {
            float cornerX = getAbsoluteX() + getAbsoluteWidth() - padding
                    - Frontend.getStringWidth(cornerText, font);
            Frontend.drawString(cornerText, cornerX,
                    getAbsoluteY() + padding * 2.2f, font, K.UI.UI_TEXT_COLOR);
        }

        renderChildren();
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
        this.text = text == null ? "" : Local.lang.t(text);
    }

    /**
     * Creates or returns text from the supplied arguments.
     * @param text the {@link String} supplied as {@code text}
     * @return the {@link UITooltip} representing the text result
     */
    public UITooltip text(String text) {
        setText(text);
        updateSize();
        return this;
    }

    /** Sets the optional upper-right text, such as a trader price. */
    public UITooltip cornerText(String text) {
        cornerText = text == null ? "" : text;
        updateSize();
        return this;
    }

    private void updateSize() {
        UIFont font = Frontend.getNormalFont();
        String[] lines = this.text.split("\n", -1);
        float maxWidth = 0.0f;
        for (String line : lines) {
            maxWidth = Math.max(
                    maxWidth,
                    Frontend.getStringWidth(line, font)
            );
        }
        if (!cornerText.isBlank()) {
            float firstLineWidth = Frontend.getStringWidth(lines[0], font);
            maxWidth = Math.max(maxWidth, firstLineWidth + padding +
                    Frontend.getStringWidth(cornerText, font));
        }

        float lineHeight = font.getSize();
        float totalWidth = maxWidth + padding * 2.0f;
        float totalHeight = (lineHeight * lines.length) + padding * 2.0f;
        setSize(totalWidth, totalHeight);
    }

    /**
     * Returns the padding.
     * @return {@code float}; the padding
     */
    public float getPadding() {
        return padding;
    }

    /**
     * Sets the padding.
     * @param padding the {@code float} supplied as {@code padding}
     */
    public void setPadding(float padding) {
        this.padding = Math.max(0.0f, padding);
    }

    /**
     * Creates or returns padding from the supplied arguments.
     * @param padding the {@code float} supplied as {@code padding}
     * @return the {@link UITooltip} representing the padding result
     */
    public UITooltip padding(float padding) {
        setPadding(padding);
        return this;
    }

    /**
     * Returns the offset x.
     * @return {@code float}; the offset x
     */
    public float getOffsetX() {
        return offsetX;
    }

    /**
     * Sets the offset x.
     * @param offsetX the {@code float} supplied as {@code offsetX}
     */
    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    /**
     * Returns the offset y.
     * @return {@code float}; the offset y
     */
    public float getOffsetY() {
        return offsetY;
    }

    /**
     * Sets the offset y.
     * @param offsetY the {@code float} supplied as {@code offsetY}
     */
    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    /**
     * Transforms this object according to the supplied values.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @return the {@link UITooltip} representing the offset result
     */
    public UITooltip offset(float x, float y) {
        setOffsetX(x);
        setOffsetY(y);
        return this;
    }

    /**
     * Updates the position.
     * @param mouseX the {@code float} supplied as {@code mouseX}
     * @param mouseY the {@code float} supplied as {@code mouseY}
     * @param windowWidth the {@code float} supplied as {@code windowWidth}
     * @param windowHeight the {@code float} supplied as {@code windowHeight}
     */
    public void updatePosition(float mouseX, float mouseY,
                               float windowWidth, float windowHeight) {
        float x = mouseX + offsetX;
        float y = mouseY + offsetY;

        if (x + getWidth() > windowWidth) {
            x = mouseX - getWidth() - offsetX;
        }

        if (y + getHeight() > windowHeight) {
            y = mouseY - getHeight() - offsetY;
        }

        setPosition(x, y);
    }
}
