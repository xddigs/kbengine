package org.kbeng.engine.ui;

import org.joml.Vector4f;

/**
 * UIProgressBar provides uiprogress bar capabilities within the ui subsystem.
 * It drives retained UI composition, user interaction handling, and rendering behavior for in-game screens.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends UIElement, inheriting shared behavior while specializing subsystem-specific logic.
 */
public class UIProgressBar extends UIElement {
    private float value;
    private float maxValue;
    
    private float borderWidth = 2.0f;
    private float cornerRadius = 4.0f;
    private boolean showText;

    private Vector4f backgroundColor = new Vector4f(0.15f, 0.15f, 0.15f, 0.8f);
    private Vector4f fillColor = new Vector4f(0.8f, 0.2f, 0.2f, 1.0f);
    private Vector4f borderColor = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
    private final Vector4f textColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

    /**
     * Creates a new {@code UIProgressBar} instance.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     * @param value the {@code float} supplied as {@code value}
     * @param maxValue the {@code float} supplied as {@code maxValue}
     * @param showText the {@code boolean} supplied as {@code showText}
     */
    public UIProgressBar(float x, float y, float width, float height,
                         float value, float maxValue, boolean showText) {
        super(x, y, width, height);
        this.maxValue = Math.max(1.0f, maxValue);
        this.value = Math.clamp(value, 0.0f, this.maxValue);
        this.showText = showText;
    }

    /**
     * {@inheritDoc}
     * Renders this object in the requested render pass.
     */
    @Override
    public void render() {
        if (!isVisible()) {
            return;
        }

        float absX = getAbsoluteX();
        float absY = getAbsoluteY();
        float absW = getAbsoluteWidth();
        float absH = getAbsoluteHeight();

        Frontend.drawRect(absX, absY, absW, absH, backgroundColor,
                cornerRadius, borderColor, borderWidth);

        float fillPercentage = Math.clamp(value / maxValue, 0.0f, 1.0f);
        float fillWidth = (absW - (borderWidth * 2.0f)) * fillPercentage;

        if (fillWidth > 0.0f) {
            float fillX = absX + borderWidth;
            float fillY = absY + borderWidth;
            float fillH = absH - (borderWidth * 2.0f);
            
            float fillRadius = Math.max(0.0f, cornerRadius - borderWidth);
            Frontend.drawRect(fillX, fillY, fillWidth, fillH, fillColor, fillRadius);
        }

        if (showText) {
            String text = (int) value + " / " + (int) maxValue;
            UIFont font = Frontend.getSmallBoldFont();
            float textW = Frontend.getStringWidth(text, font);
            float textX = absX + (absW - textW) * 0.5f;
            float textY = Frontend.getCenteredTextY(text, font, absY, absH);

            Frontend.drawString(text, textX, textY, font, textColor);
        }
    }

    /**
     * Sets the values.
     * @param value the {@code float} supplied as {@code value}
     * @param maxValue the {@code float} supplied as {@code maxValue}
     * @return the {@link UIProgressBar} representing the set values result
     */
    public UIProgressBar setValues(float value, float maxValue) {
        this.maxValue = Math.max(1.0f, maxValue);
        this.value = Math.clamp(value, 0.0f, this.maxValue);
        return this;
    }

    /**
     * Sets the value.
     * @param value the {@code float} supplied as {@code value}
     * @return the {@link UIProgressBar} representing the set value result
     */
    public UIProgressBar setValue(float value) {
        this.value = Math.clamp(value, 0.0f, maxValue);
        return this;
    }

    /**
     * Sets the colors.
     * @param fillColor the {@link Vector4f} supplied as {@code fillColor}
     * @param backgroundColor the {@link Vector4f} supplied as {@code backgroundColor}
     * @return the {@link UIProgressBar} representing the set colors result
     */
    public UIProgressBar setColors(Vector4f fillColor, Vector4f backgroundColor) {
        if (fillColor != null) this.fillColor.set(fillColor);
        if (backgroundColor != null) this.backgroundColor.set(backgroundColor);
        return this;
    }

    /**
     * Sets the border.
     * @param borderColor the {@link Vector4f} supplied as {@code borderColor}
     * @param borderWidth the {@code float} supplied as {@code borderWidth}
     * @return the {@link UIProgressBar} representing the set border result
     */
    public UIProgressBar setBorder(Vector4f borderColor, float borderWidth) {
        if (borderColor != null) this.borderColor.set(borderColor);
        this.borderWidth = borderWidth;
        return this;
    }

    /**
     * Sets the corner radius.
     * @param cornerRadius the {@code float} supplied as {@code cornerRadius}
     * @return the {@link UIProgressBar} representing the set corner radius result
     */
    public UIProgressBar setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
        return this;
    }

    /**
     * Sets the show text.
     * @param showText the {@code boolean} supplied as {@code showText}
     * @return the {@link UIProgressBar} representing the set show text result
     */
    public UIProgressBar setShowText(boolean showText) {
        this.showText = showText;
        return this;
    }

    /**
     * Returns the value.
     * @return {@code float}; the value
     */
    public float getValue() { return value; }
    /**
     * Returns the max value.
     * @return {@code float}; the max value
     */
    public float getMaxValue() { return maxValue; }
}
