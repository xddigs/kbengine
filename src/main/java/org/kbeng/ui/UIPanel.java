package org.kbeng.ui;

import org.joml.Vector4f;

/**
 * Represents the uipanel component of the kbengine runtime.
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
public class UIPanel extends UIElement {
    private final Vector4f color = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);

    /**
     * Creates a new {@code UIPanel} instance.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     */
    public UIPanel(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    /**
     * {@inheritDoc}
     * Renders this object in the requested render pass.
     */
    @Override
    public void render() {
        renderChildren();
    }

    /**
     * Returns the color.
     * @return the {@link Vector4f} representing the color
     */
    public Vector4f getColor() {
        return new Vector4f(color);
    }

    /**
     * Sets the color.
     * @param r the {@code float} supplied as {@code r}
     * @param g the {@code float} supplied as {@code g}
     * @param b the {@code float} supplied as {@code b}
     * @param a the {@code float} supplied as {@code a}
     */
    public void setColor(float r, float g, float b, float a) {
        color.set(r, g, b, a);
    }
}
