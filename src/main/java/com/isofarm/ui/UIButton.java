package com.isofarm.ui;

import com.isofarm.data.SoundGroup;
import com.isofarm.graphics.ResourceManager;
import com.isofarm.graphics.SpriteSheet;
import com.isofarm.graphics.Texture;
import com.isofarm.service.SoundService;
import org.joml.Vector4f;

/**
 * Encapsulates the state and operations required by uibutton within the game runtime.
 */
@SuppressWarnings("all")
public class UIButton extends UIElement {
    private static final float ICON_SCALE = 0.65f;
    private static final float BACKGROUNDLESS_ICON_SCALE = 5.0f / 6.0f;
    private static final Texture BUTTON_TEXTURE = ResourceManager
            .rem.getBackgroundUI();

    private SpriteSheet spriteSheet;
    private int spriteFrame;
    private Runnable onClick;
    private boolean canDrawBackground = true;

    private Vector4f normalColor = new Vector4f(1.0f);
    private Vector4f hoverColor = new Vector4f(0.25f, 0.25f, 0.25f, 1.0f);
    private Vector4f pressedColor = new Vector4f(0.1f, 0.1f, 0.1f, 1.0f);

    /**
     * Creates a new {@code UIButton} instance.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     */
    public UIButton(float x, float y, float width, float height) {
        super(x, y, width, height);
        setInteractable(true);
        setFocusable(true);
    }

    /**
     * {@inheritDoc}
     * Handles mouse released and applies its effect to the current interaction state.
     * @param mouseX the {@code float} supplied as {@code mouseX}
     * @param mouseY the {@code float} supplied as {@code mouseY}
     * @param button the {@code int} supplied as {@code button}
     * @return {@code boolean}; the mouse released result
     */
    @Override
    public boolean mouseReleased(float mouseX, float mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        if (button != 0 || !isHovered()) {
            return false;
        }
        if (onClick != null) {
            onClick.run();
            SoundService.fx.playUseSound(SoundGroup.BUTTON);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render() {
        Vector4f color = normalColor;
        if (isPressed()) {
            color = pressedColor;
        } else if (isHovered()) {
            color = hoverColor;
        }

        if (canDrawBackground) {
            Frontend.drawTexture(BUTTON_TEXTURE, getAbsoluteX(), getAbsoluteY(),
                    getAbsoluteWidth(), getAbsoluteHeight(), color);
        }

        if (spriteSheet != null) {
            float iconScale = canDrawBackground ? ICON_SCALE : BACKGROUNDLESS_ICON_SCALE;
            float size = Math.min(getAbsoluteWidth(), getAbsoluteHeight()) * iconScale;
            float x = getAbsoluteX() + (getAbsoluteWidth() - size) * 0.5f;
            float y = getAbsoluteY() + (getAbsoluteHeight() - size) * 0.5f;

            if (!canDrawBackground && (isHovered() || isPressed())) {
                float pixelScaleX = size / spriteSheet.getFrameWidth();
                float pixelScaleY = size / spriteSheet.getFrameHeight();
                float outlineSize = Math.max(1.0f,
                        Math.round(Math.min(pixelScaleX, pixelScaleY)));
                Frontend.drawSpriteOutline(spriteSheet, spriteFrame, x, y, size, size,
                        outlineSize,
                        new Vector4f(1.0f, 1.0f, 1.0f, getWorldOpacity()));
            }

            Frontend.drawSprite(spriteSheet, spriteFrame, 0, x, y, size, size,
                    new Vector4f(1.0f, 1.0f, 1.0f, getWorldOpacity()));
        }

        renderChildren();
    }

    /**
     * Returns the sprite sheet.
     * @return the {@link SpriteSheet} representing the sprite sheet
     */
    public SpriteSheet getSpriteSheet() {
        return spriteSheet;
    }

    /**
     * Sets the sprite sheet.
     * @param spriteSheet the {@link SpriteSheet} supplied as {@code spriteSheet}
     */
    public void setSpriteSheet(SpriteSheet spriteSheet) {
        this.spriteSheet = spriteSheet;
    }

    /**
     * Returns the sprite col.
     * @return {@code int}; the sprite col
     */
    public int getSpriteCol() {
        return spriteFrame;
    }

    /**
     * Sets the sprite column.
     * @param spriteFrame the {@code int} supplied as {@code spriteFrame}
     */
    public void setSpriteColumn(int spriteFrame) {
        this.spriteFrame = Math.max(0, spriteFrame);
    }

    /**
     * Returns the on click.
     * @return the {@link Runnable} representing the on click
     */
    public Runnable getOnClick() {
        return onClick;
    }

    /**
     * Sets the on click.
     * @param onClick the {@link Runnable} supplied as {@code onClick}
     * @return the {@link UIButton} representing the set on click result
     */
    public UIButton setOnClick(Runnable onClick) {
        this.onClick = onClick;
        return this;
    }

    /**
     * Checks whether the standard slot texture is drawn behind the button icon.
     * @return {@code true} when the button background is drawn
     */
    public boolean isCanDrawBackground() {
        return canDrawBackground;
    }

    /**
     * Controls whether the standard slot texture is drawn behind the button icon.
     * Backgroundless buttons use the icon's alpha channel to draw a hover outline.
     * @param canDrawBackground whether the standard background should be drawn
     * @return this button
     */
    public UIButton setCanDrawBackground(boolean canDrawBackground) {
        this.canDrawBackground = canDrawBackground;
        return this;
    }

    /**
     * Returns the normal color.
     * @return the {@link Vector4f} representing the normal color
     */
    public Vector4f getNormalColor() {
        return new Vector4f(normalColor);
    }

    /**
     * Sets the normal color.
     * @param r the {@code float} supplied as {@code r}
     * @param g the {@code float} supplied as {@code g}
     * @param b the {@code float} supplied as {@code b}
     * @param a the {@code float} supplied as {@code a}
     * @return the {@link UIButton} representing the set normal color result
     */
    public UIButton setNormalColor(float r, float g, float b, float a) {
        normalColor.set(r, g, b, a);
        return this;
    }

    /**
     * Returns the hover color.
     * @return the {@link Vector4f} representing the hover color
     */
    public Vector4f getHoverColor() {
        return new Vector4f(hoverColor);
    }

    /**
     * Sets the hover color.
     * @param r the {@code float} supplied as {@code r}
     * @param g the {@code float} supplied as {@code g}
     * @param b the {@code float} supplied as {@code b}
     * @param a the {@code float} supplied as {@code a}
     * @return the {@link UIButton} representing the set hover color result
     */
    public UIButton setHoverColor(float r, float g, float b, float a) {
        hoverColor.set(r, g, b, a);
        return this;
    }

    /**
     * Returns the pressed color.
     * @return the {@link Vector4f} representing the pressed color
     */
    public Vector4f getPressedColor() {
        return new Vector4f(pressedColor);
    }

    /**
     * Sets the pressed color.
     * @param r the {@code float} supplied as {@code r}
     * @param g the {@code float} supplied as {@code g}
     * @param b the {@code float} supplied as {@code b}
     * @param a the {@code float} supplied as {@code a}
     * @return the {@link UIButton} representing the set pressed color result
     */
    public UIButton setPressedColor(float r, float g, float b, float a) {
        pressedColor.set(r, g, b, a);
        return this;
    }
}
