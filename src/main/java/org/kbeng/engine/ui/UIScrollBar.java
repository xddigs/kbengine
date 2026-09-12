package org.kbeng.engine.ui;

import org.kbeng.engine.graphics.ResourceManager;
import org.kbeng.engine.graphics.Texture;
import org.kbeng.engine.input.Mouse;
import org.kbeng.engine.utils.Settings;
import org.joml.Vector4f;

import java.util.function.IntConsumer;

/**
 * UIScrollBar provides uiscroll bar capabilities within the ui subsystem.
 * It drives retained UI composition, user interaction handling, and rendering behavior for in-game screens.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 * It extends UIElement, inheriting shared behavior while specializing subsystem-specific logic.
 */
public class UIScrollBar extends UIElement {
    private static final int TRACK_SLICE_SIZE = 3;

    private int value;
    private int maximum;
    private boolean dragging;
    private float dragOffset;
    private IntConsumer valueChangedListener;

    /**
     * Creates a vertical scroll bar.
     * @param x the horizontal position relative to its parent
     * @param y the vertical position relative to its parent
     * @param width the width of the track and knob
     * @param height the full track height
     */
    public UIScrollBar(float x, float y, float width, float height) {
        super(x, y, width, height);
        setFocusable(true);
    }

    /**
     * {@inheritDoc}
     * Keeps the knob attached to the pointer while the primary mouse button is held.
     */
    @Override
    public void update(float delta) {
        super.update(delta);
        if (!dragging) return;
        if (!Mouse.isButtonDown(Mouse.BUTTON_LEFT)) {
            dragging = false;
            return;
        }
        setValueFromKnobY(Mouse.getY() - dragOffset);
    }

    /**
     * {@inheritDoc}
     * Starts dragging the knob or moves it directly to the selected track position.
     */
    @Override
    public boolean mousePressed(float mouseX, float mouseY, int button) {
        if (button != Mouse.BUTTON_LEFT || !isVisible() || !isEnabled()
                || !contains(mouseX, mouseY)) {
            return false;
        }

        float knobY = getKnobY();
        float knobHeight = getKnobHeight();
        if (mouseY >= knobY && mouseY <= knobY + knobHeight) {
            dragOffset = mouseY - knobY;
        } else {
            dragOffset = knobHeight * 0.5f;
            setValueFromKnobY(mouseY - dragOffset);
        }
        dragging = true;
        return true;
    }

    /**
     * {@inheritDoc}
     * Ends an active knob drag even when the pointer leaves the track.
     */
    @Override
    public boolean mouseReleased(float mouseX, float mouseY, int button) {
        if (button != Mouse.BUTTON_LEFT || !dragging) return false;
        dragging = false;
        return true;
    }

    /**
     * Moves the scroll position by an integer number of steps.
     * @param amount positive values move down and negative values move up
     */
    public void scrollBy(int amount) {
        setValue(value + amount);
    }

    /**
     * Sets the largest permitted scroll position and clamps the current value.
     * @param maximum the non-negative maximum value
     */
    public void setMaximum(int maximum) {
        this.maximum = Math.max(0, maximum);
        setValue(value);
    }

    /**
     * Returns the current integer scroll position.
     * @return the current scroll position
     */
    public int getValue() {
        return value;
    }

    /**
     * Sets the current scroll position.
     * @param value the requested scroll position
     */
    public void setValue(int value) {
        int clamped = Math.clamp(value, 0, maximum);
        if (this.value == clamped) return;
        this.value = clamped;
        if (valueChangedListener != null) valueChangedListener.accept(clamped);
    }

    /**
     * Registers a listener invoked whenever the integer position changes.
     * @param listener the listener, or {@code null} to clear it
     * @return this scroll bar
     */
    public UIScrollBar setOnValueChanged(IntConsumer listener) {
        this.valueChangedListener = listener;
        return this;
    }

    private void setValueFromKnobY(float knobY) {
        float travel = Math.max(0.0f, getAbsoluteHeight() - getKnobHeight());
        if (maximum == 0 || travel == 0.0f) {
            setValue(0);
            return;
        }
        float position = Math.clamp(knobY - getAbsoluteY(), 0.0f, travel);
        setValue(Math.round(position / travel * maximum));
    }

    private float getKnobHeight() {
        return Math.min(getAbsoluteWidth(), getAbsoluteHeight());
    }

    private float getKnobY() {
        if (maximum == 0) return getAbsoluteY();
        float travel = Math.max(0.0f, getAbsoluteHeight() - getKnobHeight());
        return getAbsoluteY() + travel * value / maximum;
    }

    /**
     * {@inheritDoc}
     * Draws the vertically stretched track and the draggable knob.
     */
    @Override
    public void render() {
        int textureWidth = Math.max(TRACK_SLICE_SIZE * 2,
                Math.round(getAbsoluteWidth() / Settings.getScale()));
        int textureHeight = Math.max(TRACK_SLICE_SIZE * 2,
                Math.round(getAbsoluteHeight() / Settings.getScale()));
        Texture track = Frontend.createNineSliceTexture(
                ResourceManager.rem.getScrollBar(), textureWidth,
                textureHeight, TRACK_SLICE_SIZE);
        Vector4f color = new Vector4f(1.0f);
        Frontend.drawTexture(track, getAbsoluteX(), getAbsoluteY(),
                getAbsoluteWidth(), getAbsoluteHeight(), color);
        Frontend.drawTexture(ResourceManager.rem.getScrollKnob(),
                getAbsoluteX(), getKnobY(), getAbsoluteWidth(),
                getKnobHeight(), color);
    }
}
