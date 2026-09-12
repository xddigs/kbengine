package org.kbeng.ui;

import org.kbeng.data.DataClass;
import org.kbeng.data.GodObject;
import org.kbeng.graphics.SpriteSheet;
import org.kbeng.graphics.Texture;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * UIElement provides uielement capabilities within the ui subsystem.
 *
 * It drives retained UI composition, user interaction handling, and rendering behavior for in-game screens.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@SuppressWarnings("all")
@DataClass
@GodObject
public abstract class UIElement {
    private final List<UIElement> children = new ArrayList<>();
    private final Vector4f tint = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private final Vector2f spriteOffset = new Vector2f();
    private UIElement parent;
    private float x;
    private float y;
    private float width;
    private float height;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float rotation;
    private float pivotX = 0.5f;
    private float pivotY = 0.5f;
    private float opacity = 1.0f;
    private int zIndex;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean interactable = true;
    private boolean focusable;
    private boolean hovered;
    private boolean pressed;
    private boolean focused;
    private boolean mouseInside;
    private boolean clipChildren;
    private Anchor anchor = Anchor.TOP_LEFT;
    private Texture sprite;
    private SpriteSheet spriteSheet;
    private String tooltipText;
    private int spriteCol;
    private int spriteRow;
    private int spriteFrameCount = 1;
    private boolean spriteAnimated;
    private boolean spriteLoop = true;
    private float spriteFrameDuration = 0.1f;
    private float spriteAnimationTimer;
    private boolean spriteFlipX;
    private boolean spriteFlipY;
    private float spriteScaleX = 1.0f;
    private float spriteScaleY = 1.0f;
    private Consumer<UIElement> clickListener;
    private Consumer<UIElement> mouseEnterListener;
    private Consumer<UIElement> mouseExitListener;
    private Consumer<UIElement> mousePressListener;
    private Consumer<UIElement> mouseReleaseListener;
    private Consumer<UIElement> focusListener;
    private Consumer<UIElement> blurListener;

    /**
     * Creates a new {@code UIElement} instance.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     */
    protected UIElement(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Renders this object in the requested render pass.
     */
    public abstract void render();

    /**
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void update(float delta) {
        if (!visible) {
            return;
        }

        updateSpriteAnimation(delta);

        for (UIElement child : getSortedChildren()) {
            child.update(delta);
        }
    }

    /**
     * Updates the sprite animation.
     * @param delta the {@code float} supplied as {@code delta}
     */
    protected void updateSpriteAnimation(float delta) {
        if (!spriteAnimated || spriteSheet == null || spriteFrameCount <= 1) {
            return;
        }

        spriteAnimationTimer += delta;

        while (spriteAnimationTimer >= spriteFrameDuration) {
            spriteAnimationTimer -= spriteFrameDuration;
            spriteCol++;
            if (spriteCol >= spriteFrameCount) {
                if (spriteLoop) {
                    spriteCol = 0;
                } else {
                    spriteCol = spriteFrameCount - 1;
                    spriteAnimated = false;
                }
            }
        }
    }

    /**
     * Renders the children.
     */
    public void renderChildren() {
        for (UIElement child : getSortedChildren()) {
            if (child.isVisible()) {
                child.render();
            }
        }
    }

    /**
     * Adds the child.
     * @param child the {@link UIElement} supplied as {@code child}
     */
    public void addChild(UIElement child) {
        if (child == null || child == this) {
            return;
        }

        if (child.parent != null) {
            child.parent.removeChild(child);
        }

        child.parent = this;
        children.add(child);
        sortChildren();
    }

    /**
     * Removes the child.
     * @param child the {@link UIElement} supplied as {@code child}
     */
    public void removeChild(UIElement child) {
        if (child == null) {
            return;
        }

        if (children.remove(child)) {
            child.parent = null;
        }
    }

    /**
     * Removes the all children.
     */
    public void removeAllChildren() {
        for (UIElement child : children) {
            child.parent = null;
        }

        children.clear();
    }

    /**
     * Returns the children.
     * @return the {@link List} representing the children
     */
    public List<UIElement> getChildren() {
        return List.copyOf(children);
    }

    /**
     * Reorganizes inventory state for sort children.
     */
    private void sortChildren() {
        children.sort(Comparator.comparingInt(UIElement::getZIndex));
    }

    /**
     * Returns the parent.
     * @return the {@link UIElement} representing the parent
     */
    public UIElement getParent() {
        return parent;
    }

    /**
     * Sets the parent.
     * @param parent the {@link UIElement} supplied as {@code parent}
     */
    public void setParent(UIElement parent) {
        if (this.parent == parent) {
            return;
        }

        if (this.parent != null) {
            this.parent.removeChild(this);
        }

        if (parent != null) {
            parent.addChild(this);
        }
    }

    /**
     * Returns the x.
     * @return {@code float}; the x
     */
    public float getX() {
        return x;
    }

    /**
     * Sets the x.
     * @param x the {@code float} supplied as {@code x}
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     * Returns the y.
     * @return {@code float}; the y
     */
    public float getY() {
        return y;
    }

    /**
     * Sets the y.
     * @param y the {@code float} supplied as {@code y}
     */
    public void setY(float y) {
        this.y = y;
    }

    /**
     * Sets the position.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @return the {@link UIElement} representing the set position result
     */
    public UIElement setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * Returns the width.
     * @return {@code float}; the width
     */
    public float getWidth() {
        return width;
    }

    /**
     * Sets the width.
     * @param width the {@code float} supplied as {@code width}
     */
    public void setWidth(float width) {
        this.width = Math.max(0.0f, width);
    }

    /**
     * Returns the height.
     * @return {@code float}; the height
     */
    public float getHeight() {
        return height;
    }

    /**
     * Sets the height.
     * @param height the {@code float} supplied as {@code height}
     */
    public void setHeight(float height) {
        this.height = Math.max(0.0f, height);
    }

    /**
     * Sets the size.
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     * @return the {@link UIElement} representing the set size result
     */
    public UIElement setSize(float width, float height) {
        this.width = Math.max(0.0f, width);
        this.height = Math.max(0.0f, height);
        return this;
    }

    /**
     * Returns the scale x.
     * @return {@code float}; the scale x
     */
    public float getScaleX() {
        return scaleX;
    }

    /**
     * Sets the scale x.
     * @param scaleX the {@code float} supplied as {@code scaleX}
     */
    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }

    /**
     * Returns the scale y.
     * @return {@code float}; the scale y
     */
    public float getScaleY() {
        return scaleY;
    }

    /**
     * Sets the scale y.
     * @param scaleY the {@code float} supplied as {@code scaleY}
     */
    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }

    /**
     * Sets the scale.
     * @param scale the {@code float} supplied as {@code scale}
     * @return the {@link UIElement} representing the set scale result
     */
    public UIElement setScale(float scale) {
        this.scaleX = scale;
        this.scaleY = scale;
        return this;
    }

    /**
     * Sets the scale.
     * @param scaleX the {@code float} supplied as {@code scaleX}
     * @param scaleY the {@code float} supplied as {@code scaleY}
     * @return the {@link UIElement} representing the set scale result
     */
    public UIElement setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        return this;
    }

    /**
     * Returns the rotation.
     * @return {@code float}; the rotation
     */
    public float getRotation() {
        return rotation;
    }

    /**
     * Sets the rotation.
     * @param rotation the {@code float} supplied as {@code rotation}
     * @return the {@link UIElement} representing the set rotation result
     */
    public UIElement setRotation(float rotation) {
        this.rotation = rotation;
        return this;
    }

    /**
     * Returns the pivot x.
     * @return {@code float}; the pivot x
     */
    public float getPivotX() {
        return pivotX;
    }

    /**
     * Sets the pivot x.
     * @param pivotX the {@code float} supplied as {@code pivotX}
     */
    public void setPivotX(float pivotX) {
        this.pivotX = pivotX;
    }

    /**
     * Returns the pivot y.
     * @return {@code float}; the pivot y
     */
    public float getPivotY() {
        return pivotY;
    }

    /**
     * Sets the pivot y.
     * @param pivotY the {@code float} supplied as {@code pivotY}
     */
    public void setPivotY(float pivotY) {
        this.pivotY = pivotY;
    }

    /**
     * Sets the pivot.
     * @param pivotX the {@code float} supplied as {@code pivotX}
     * @param pivotY the {@code float} supplied as {@code pivotY}
     * @return the {@link UIElement} representing the set pivot result
     */
    public UIElement setPivot(float pivotX, float pivotY) {
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        return this;
    }

    /**
     * Returns the opacity.
     * @return {@code float}; the opacity
     */
    public float getOpacity() {
        return opacity;
    }

    /**
     * Sets the opacity.
     * @param opacity the {@code float} supplied as {@code opacity}
     */
    public void setOpacity(float opacity) {
        this.opacity = Math.clamp(opacity, 0.0f, 1.0f);
    }

    /**
     * Sets the opacity value.
     * @param opacity the {@code float} supplied as {@code opacity}
     * @return the {@link UIElement} representing the set opacity value result
     */
    public UIElement setOpacityValue(float opacity) {
        setOpacity(opacity);
        return this;
    }

    /**
     * Returns the zindex.
     * @return {@code int}; the zindex
     */
    public int getZIndex() {
        return zIndex;
    }

    /**
     * Sets the zindex.
     * @param zIndex the {@code int} supplied as {@code zIndex}
     */
    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;

        if (parent != null) {
            parent.sortChildren();
        }
    }

    /**
     * Sets the layer.
     * @param zIndex the {@code int} supplied as {@code zIndex}
     * @return the {@link UIElement} representing the set layer result
     */
    public UIElement setLayer(int zIndex) {
        setZIndex(zIndex);
        return this;
    }

    /**
     * Checks whether the visible condition is met.
     * @return {@code true} if visible; otherwise {@code false}
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets the visible.
     * @param visible the {@code boolean} supplied as {@code visible}
     */
    public void setVisible(boolean visible) {
        this.visible = visible;

        if (!visible) {
            setHovered(false);
            setPressed(false);
        }
    }

    /**
     * Activates this object and prepares any state it requires.
     * @return the {@link UIElement} representing the show result
     */
    public UIElement show() {
        setVisible(true);
        return this;
    }

    /**
     * Deactivates this object and releases its transient state.
     * @return the {@link UIElement} representing the hide result
     */
    public UIElement hide() {
        setVisible(false);
        return this;
    }

    /**
     * Checks whether the enabled condition is met.
     * @return {@code true} if enabled; otherwise {@code false}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the enabled.
     * @param enabled the {@code boolean} supplied as {@code enabled}
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        if (!enabled) {
            setHovered(false);
            setPressed(false);
            setFocused(false);
        }
    }

    /**
     * Activates this object and prepares any state it requires.
     * @return the {@link UIElement} representing the enable result
     */
    public UIElement enable() {
        setEnabled(true);
        return this;
    }

    /**
     * Deactivates this object and releases its transient state.
     * @return the {@link UIElement} representing the disable result
     */
    public UIElement disable() {
        setEnabled(false);
        return this;
    }

    /**
     * Returns the tooltip text.
     * @return the {@link String} representing the tooltip text
     */
    public String getTooltipText() {
        return tooltipText;
    }

    /**
     * Sets the tooltip text.
     * @param tooltipText the {@link String} supplied as {@code tooltipText}
     */
    public void setTooltipText(String tooltipText) {
        this.tooltipText = tooltipText;
    }

    /**
     * Creates or returns tooltip from the supplied arguments.
     * @param tooltipText the {@link String} supplied as {@code tooltipText}
     * @return the {@link UIElement} representing the tooltip result
     */
    public UIElement tooltip(String tooltipText) {
        this.tooltipText = tooltipText;
        return this;
    }

    /**
     * Checks whether the interactable condition is met.
     * @return {@code true} if interactable; otherwise {@code false}
     */
    public boolean isInteractable() {
        return interactable;
    }

    /**
     * Sets the interactable.
     * @param interactable the {@code boolean} supplied as {@code interactable}
     */
    public void setInteractable(boolean interactable) {
        this.interactable = interactable;
    }

    /**
     * Checks whether the focusable condition is met.
     * @return {@code true} if focusable; otherwise {@code false}
     */
    public boolean isFocusable() {
        return focusable;
    }

    /**
     * Sets the focusable.
     * @param focusable the {@code boolean} supplied as {@code focusable}
     */
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    /**
     * Checks whether the hovered condition is met.
     * @return {@code true} if hovered; otherwise {@code false}
     */
    public boolean isHovered() {
        return hovered;
    }

    /**
     * Sets the hovered.
     * @param hovered the {@code boolean} supplied as {@code hovered}
     */
    public void setHovered(boolean hovered) {
        if (this.hovered == hovered) {
            return;
        }

        this.hovered = hovered;

        if (hovered) {
            if (mouseEnterListener != null) {
                mouseEnterListener.accept(this);
            }
        } else {
            if (mouseExitListener != null) {
                mouseExitListener.accept(this);
            }
        }
    }

    /**
     * Checks whether the pressed condition is met.
     * @return {@code true} if pressed; otherwise {@code false}
     */
    public boolean isPressed() {
        return pressed;
    }

    /**
     * Sets the pressed.
     * @param pressed the {@code boolean} supplied as {@code pressed}
     */
    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    /**
     * Checks whether the focused condition is met.
     * @return {@code true} if focused; otherwise {@code false}
     */
    public boolean isFocused() {
        return focused;
    }

    /**
     * Sets the focused.
     * @param focused the {@code boolean} supplied as {@code focused}
     */
    public void setFocused(boolean focused) {
        if (this.focused == focused) {
            return;
        }

        this.focused = focused;

        if (focused) {
            if (focusListener != null) {
                focusListener.accept(this);
            }
        } else {
            if (blurListener != null) {
                blurListener.accept(this);
            }
        }
    }

    /**
     * Checks whether the mouse inside condition is met.
     * @return {@code true} if mouse inside; otherwise {@code false}
     */
    public boolean isMouseInside() {
        return mouseInside;
    }

    /**
     * Sets the mouse inside.
     * @param mouseInside the {@code boolean} supplied as {@code mouseInside}
     */
    public void setMouseInside(boolean mouseInside) {
        this.mouseInside = mouseInside;
    }

    /**
     * Checks whether the clip children condition is met.
     * @return {@code true} if clip children; otherwise {@code false}
     */
    public boolean isClipChildren() {
        return clipChildren;
    }

    /**
     * Sets the clip children.
     * @param clipChildren the {@code boolean} supplied as {@code clipChildren}
     */
    public void setClipChildren(boolean clipChildren) {
        this.clipChildren = clipChildren;
    }

    /**
     * Returns the anchor.
     * @return the {@link Anchor} representing the anchor
     */
    public Anchor getAnchor() {
        return anchor;
    }

    /**
     * Sets the anchor.
     * @param anchor the {@link Anchor} supplied as {@code anchor}
     */
    public void setAnchor(Anchor anchor) {
        this.anchor = anchor == null ? Anchor.TOP_LEFT : anchor;
    }

    /**
     * Creates or returns anchor from the supplied arguments.
     * @param anchor the {@link Anchor} supplied as {@code anchor}
     * @return the {@link UIElement} representing the anchor result
     */
    public UIElement anchor(Anchor anchor) {
        setAnchor(anchor);
        return this;
    }

    /**
     * Returns the tint.
     * @return the {@link Vector4f} representing the tint
     */
    public Vector4f getTint() {
        return new Vector4f(tint);
    }

    /**
     * Sets the tint.
     * @param tint the {@link Vector4f} supplied as {@code tint}
     */
    public void setTint(Vector4f tint) {
        if (tint == null) {
            this.tint.set(1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }

        this.tint.set(tint);
    }

    /**
     * Sets the tint.
     * @param r the {@code float} supplied as {@code r}
     * @param g the {@code float} supplied as {@code g}
     * @param b the {@code float} supplied as {@code b}
     * @param a the {@code float} supplied as {@code a}
     */
    public void setTint(float r, float g, float b, float a) {
        tint.set(r, g, b, a);
    }

    /**
     * Transforms this object according to the supplied values.
     * @param r the {@code float} supplied as {@code r}
     * @param g the {@code float} supplied as {@code g}
     * @param b the {@code float} supplied as {@code b}
     * @param a the {@code float} supplied as {@code a}
     * @return the {@link UIElement} representing the tint result
     */
    public UIElement tint(float r, float g, float b, float a) {
        setTint(r, g, b, a);
        return this;
    }

    /**
     * Transforms this object according to the supplied values.
     * @param r the {@code float} supplied as {@code r}
     * @param g the {@code float} supplied as {@code g}
     * @param b the {@code float} supplied as {@code b}
     * @return the {@link UIElement} representing the tint result
     */
    public UIElement tint(float r, float g, float b) {
        setTint(r, g, b, 1.0f);
        return this;
    }

    /**
     * Returns the sprite.
     * @return the {@link Texture} representing the sprite
     */
    public Texture getSprite() {
        return sprite;
    }

    /**
     * Sets the sprite.
     * @param sprite the {@link Texture} supplied as {@code sprite}
     */
    public void setSprite(Texture sprite) {
        this.sprite = sprite;
        this.spriteSheet = null;
        this.spriteCol = 0;
        this.spriteFrameCount = 1;
        this.spriteAnimated = false;
    }

    /**
     * Creates or returns sprite from the supplied arguments.
     * @param sprite the {@link Texture} supplied as {@code sprite}
     * @return the {@link UIElement} representing the sprite result
     */
    public UIElement sprite(Texture sprite) {
        setSprite(sprite);
        return this;
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
        this.sprite = null;
        this.spriteCol = 0;
        this.spriteFrameCount = spriteSheet != null ? spriteSheet.getTotalFrames() : 1;
        this.spriteAnimationTimer = 0.0f;
    }

    /**
     * Creates or returns sprite sheet from the supplied arguments.
     * @param spriteSheet the {@link SpriteSheet} supplied as {@code spriteSheet}
     * @return the {@link UIElement} representing the sprite sheet result
     */
    public UIElement spriteSheet(SpriteSheet spriteSheet) {
        setSpriteSheet(spriteSheet);
        return this;
    }

    /**
     * Checks whether the sprite condition is met.
     * @return {@code true} if sprite; otherwise {@code false}
     */
    public boolean hasSprite() {
        return sprite != null || spriteSheet != null;
    }

    /**
     * Checks whether the static sprite condition is met.
     * @return {@code true} if static sprite; otherwise {@code false}
     */
    public boolean hasStaticSprite() {
        return sprite != null;
    }

    /**
     * Checks whether the sprite sheet condition is met.
     * @return {@code true} if sprite sheet; otherwise {@code false}
     */
    public boolean hasSpriteSheet() {
        return spriteSheet != null;
    }

    /**
     * Returns the sprite col.
     * @return {@code int}; the sprite col
     */
    public int getSpriteCol() {
        return spriteCol;
    }

    /**
     * Sets the sprite column.
     * @param spriteFrame the {@code int} supplied as {@code spriteFrame}
     */
    public void setSpriteColumn(int spriteFrame) {
        if (spriteSheet == null) {
            this.spriteCol = 0;
            return;
        }

        this.spriteCol = Math.clamp(spriteFrame, 0, spriteFrameCount - 1);
    }

    /**
     * Returns the sprite row.
     * @return {@code int}; the sprite row
     */
    public int getSpriteRow() {
        return spriteRow;
    }

    /**
     * Sets the sprite row.
     * @param spriteRow the {@code int} supplied as {@code spriteRow}
     */
    public void setSpriteRow(int spriteRow) {
        if (spriteSheet == null) {
            this.spriteRow = 0;
            return;
        }

        this.spriteRow = Math.clamp(spriteRow, 0, spriteFrameCount - 1);
    }

    /**
     * Creates or returns col from the supplied arguments.
     * @param frame the {@code int} supplied as {@code frame}
     * @return the {@link UIElement} representing the col result
     */
    public UIElement col(int frame) {
        setSpriteColumn(frame);
        return this;
    }

    /**
     * Creates or returns row from the supplied arguments.
     * @param row the {@code int} supplied as {@code row}
     * @return the {@link UIElement} representing the row result
     */
    public UIElement row(int row) {
        setSpriteRow(row);
        return this;
    }

    /**
     * Returns the sprite frame count.
     * @return {@code int}; the sprite frame count
     */
    public int getSpriteFrameCount() {
        return spriteFrameCount;
    }

    /**
     * Sets the sprite frame count.
     * @param spriteFrameCount the {@code int} supplied as {@code spriteFrameCount}
     */
    public void setSpriteFrameCount(int spriteFrameCount) {
        this.spriteFrameCount = Math.max(1, spriteFrameCount);

        if (spriteCol >= this.spriteFrameCount) {
            spriteCol = this.spriteFrameCount - 1;
        }
    }

    /**
     * Returns the sprite frame duration.
     * @return {@code float}; the sprite frame duration
     */
    public float getSpriteFrameDuration() {
        return spriteFrameDuration;
    }

    /**
     * Sets the sprite frame duration.
     * @param spriteFrameDuration the {@code float} supplied as {@code spriteFrameDuration}
     */
    public void setSpriteFrameDuration(float spriteFrameDuration) {
        this.spriteFrameDuration = Math.max(0.001f, spriteFrameDuration);
    }

    /**
     * Calculates the value represented by frame duration from the current state.
     * @param duration the {@code float} supplied as {@code duration}
     * @return the {@link UIElement} representing the frame duration result
     */
    public UIElement frameDuration(float duration) {
        setSpriteFrameDuration(duration);
        return this;
    }

    /**
     * Checks whether the sprite animated condition is met.
     * @return {@code true} if sprite animated; otherwise {@code false}
     */
    public boolean isSpriteAnimated() {
        return spriteAnimated;
    }

    /**
     * Sets the sprite animated.
     * @param spriteAnimated the {@code boolean} supplied as {@code spriteAnimated}
     */
    public void setSpriteAnimated(boolean spriteAnimated) {
        this.spriteAnimated = spriteAnimated && spriteSheet != null && spriteFrameCount > 1;
    }

    /**
     * Updates or derives runtime state for animate sprite according to the supplied arguments.
     * @param animate the {@code boolean} supplied as {@code animate}
     * @return the {@link UIElement} representing the animate sprite result
     */
    public UIElement animateSprite(boolean animate) {
        setSpriteAnimated(animate);
        return this;
    }

    /**
     * Checks whether the sprite looping condition is met.
     * @return {@code true} if sprite looping; otherwise {@code false}
     */
    public boolean isSpriteLooping() {
        return spriteLoop;
    }

    /**
     * Sets the sprite looping.
     * @param spriteLoop the {@code boolean} supplied as {@code spriteLoop}
     */
    public void setSpriteLooping(boolean spriteLoop) {
        this.spriteLoop = spriteLoop;
    }

    /**
     * Processes each applicable element for loop sprite.
     * @param loop the {@code boolean} supplied as {@code loop}
     * @return the {@link UIElement} representing the loop sprite result
     */
    public UIElement loopSprite(boolean loop) {
        setSpriteLooping(loop);
        return this;
    }

    /**
     * Returns the sprite animation timer.
     * @return {@code float}; the sprite animation timer
     */
    public float getSpriteAnimationTimer() {
        return spriteAnimationTimer;
    }

    /**
     * Resets sprite animation to its initial runtime state.
     */
    public void resetSpriteAnimation() {
        spriteCol = 0;
        spriteAnimationTimer = 0.0f;
    }

    /**
     * Checks whether the sprite flip x condition is met.
     * @return {@code true} if sprite flip x; otherwise {@code false}
     */
    public boolean isSpriteFlipX() {
        return spriteFlipX;
    }

    /**
     * Sets the sprite flip x.
     * @param spriteFlipX the {@code boolean} supplied as {@code spriteFlipX}
     */
    public void setSpriteFlipX(boolean spriteFlipX) {
        this.spriteFlipX = spriteFlipX;
    }

    /**
     * Transforms x according to the supplied values.
     * @param flip the {@code boolean} supplied as {@code flip}
     * @return the {@link UIElement} representing the flip x result
     */
    public UIElement flipX(boolean flip) {
        setSpriteFlipX(flip);
        return this;
    }

    /**
     * Checks whether the sprite flip y condition is met.
     * @return {@code true} if sprite flip y; otherwise {@code false}
     */
    public boolean isSpriteFlipY() {
        return spriteFlipY;
    }

    /**
     * Sets the sprite flip y.
     * @param spriteFlipY the {@code boolean} supplied as {@code spriteFlipY}
     */
    public void setSpriteFlipY(boolean spriteFlipY) {
        this.spriteFlipY = spriteFlipY;
    }

    /**
     * Transforms y according to the supplied values.
     * @param flip the {@code boolean} supplied as {@code flip}
     * @return the {@link UIElement} representing the flip y result
     */
    public UIElement flipY(boolean flip) {
        setSpriteFlipY(flip);
        return this;
    }

    /**
     * Returns the sprite scale x.
     * @return {@code float}; the sprite scale x
     */
    public float getSpriteScaleX() {
        return spriteScaleX;
    }

    /**
     * Sets the sprite scale x.
     * @param spriteScaleX the {@code float} supplied as {@code spriteScaleX}
     */
    public void setSpriteScaleX(float spriteScaleX) {
        this.spriteScaleX = spriteScaleX;
    }

    /**
     * Returns the sprite scale y.
     * @return {@code float}; the sprite scale y
     */
    public float getSpriteScaleY() {
        return spriteScaleY;
    }

    /**
     * Sets the sprite scale y.
     * @param spriteScaleY the {@code float} supplied as {@code spriteScaleY}
     */
    public void setSpriteScaleY(float spriteScaleY) {
        this.spriteScaleY = spriteScaleY;
    }

    /**
     * Sets the sprite scale.
     * @param scale the {@code float} supplied as {@code scale}
     * @return the {@link UIElement} representing the set sprite scale result
     */
    public UIElement setSpriteScale(float scale) {
        this.spriteScaleX = scale;
        this.spriteScaleY = scale;
        return this;
    }

    /**
     * Returns the sprite offset.
     * @return the {@link Vector2f} representing the sprite offset
     */
    public Vector2f getSpriteOffset() {
        return new Vector2f(spriteOffset);
    }

    /**
     * Sets the sprite offset.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     */
    public void setSpriteOffset(float x, float y) {
        spriteOffset.set(x, y);
    }

    /**
     * Creates or returns sprite offset from the supplied arguments.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @return the {@link UIElement} representing the sprite offset result
     */
    public UIElement spriteOffset(float x, float y) {
        setSpriteOffset(x, y);
        return this;
    }

    /**
     * Returns the absolute x.
     * @return {@code float}; the absolute x
     */
    public float getAbsoluteX() {
        if (parent == null) {
            return x;
        }

        return parent.getContentX() + x;
    }

    /**
     * Returns the absolute y.
     * @return {@code float}; the absolute y
     */
    public float getAbsoluteY() {
        if (parent == null) {
            return y;
        }

        return parent.getContentY() + y;
    }

    /**
     * Returns the content x.
     * @return {@code float}; the content x
     */
    public float getContentX() {
        return getAbsoluteX();
    }

    /**
     * Returns the content y.
     * @return {@code float}; the content y
     */
    public float getContentY() {
        return getAbsoluteY();
    }

    /**
     * Returns the absolute width.
     * @return {@code float}; the absolute width
     */
    public float getAbsoluteWidth() {
        return width * scaleX;
    }

    /**
     * Returns the absolute height.
     * @return {@code float}; the absolute height
     */
    public float getAbsoluteHeight() {
        return height * scaleY;
    }

    /**
     * Returns the absolute position.
     * @return the {@link Vector2f} representing the absolute position
     */
    public Vector2f getAbsolutePosition() {
        return new Vector2f(getAbsoluteX(), getAbsoluteY());
    }

    /**
     * Returns the absolute size.
     * @return the {@link Vector2f} representing the absolute size
     */
    public Vector2f getAbsoluteSize() {
        return new Vector2f(getAbsoluteWidth(), getAbsoluteHeight());
    }

    /**
     * Determines whether this object is satisfied by the current state.
     * @param mouseX the {@code float} supplied as {@code mouseX}
     * @param mouseY the {@code float} supplied as {@code mouseY}
     * @return {@code boolean}; the contains result
     */
    public boolean contains(float mouseX, float mouseY) {
        if (!visible || !enabled || !interactable) {
            return false;
        }

        float absoluteX = getAbsoluteX();
        float absoluteY = getAbsoluteY();
        float absoluteWidth = getAbsoluteWidth();
        float absoluteHeight = getAbsoluteHeight();

        return mouseX >= absoluteX &&
                mouseX <= absoluteX + absoluteWidth &&
                mouseY >= absoluteY &&
                mouseY <= absoluteY + absoluteHeight;
    }

    /**
     * Finds and returns the element at.
     * @param mouseX the {@code float} supplied as {@code mouseX}
     * @param mouseY the {@code float} supplied as {@code mouseY}
     * @return the {@link UIElement} representing the located element at
     */
    public UIElement findElementAt(float mouseX, float mouseY) {
        if (!visible || !enabled || !interactable) {
            return null;
        }

        List<UIElement> sorted = new ArrayList<>(children);
        sorted.sort(Comparator.comparingInt(UIElement::getZIndex).reversed());

        for (UIElement child : sorted) {
            UIElement found = child.findElementAt(mouseX, mouseY);

            if (found != null) {
                return found;
            }
        }

        return contains(mouseX, mouseY) ? this : null;
    }

    /**
     * Handles mouse moved and applies its effect to the current interaction state.
     * @param mouseX the {@code float} supplied as {@code mouseX}
     * @param mouseY the {@code float} supplied as {@code mouseY}
     */
    public void mouseMoved(float mouseX, float mouseY) {
        if (!visible || !enabled) {
            return;
        }

        boolean inside = contains(mouseX, mouseY);
        setMouseInside(inside);
        setHovered(inside);

        for (UIElement child : getSortedChildren()) {
            child.mouseMoved(mouseX, mouseY);
        }
    }

    /**
     * Handles mouse pressed and applies its effect to the current interaction state.
     * @param mouseX the {@code float} supplied as {@code mouseX}
     * @param mouseY the {@code float} supplied as {@code mouseY}
     * @param button the {@code int} supplied as {@code button}
     * @return {@code boolean}; the mouse pressed result
     */
    public boolean mousePressed(float mouseX, float mouseY, int button) {
        if (!visible || !enabled || !interactable) {
            return false;
        }

        List<UIElement> sorted = new ArrayList<>(children);
        sorted.sort(Comparator.comparingInt(UIElement::getZIndex).reversed());

        for (UIElement child : sorted) {
            if (child.mousePressed(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (!contains(mouseX, mouseY)) {
            return false;
        }

        pressed = true;

        if (mousePressListener != null) {
            mousePressListener.accept(this);
        }

        return true;
    }

    /**
     * Handles mouse released and applies its effect to the current interaction state.
     * @param mouseX the {@code float} supplied as {@code mouseX}
     * @param mouseY the {@code float} supplied as {@code mouseY}
     * @param button the {@code int} supplied as {@code button}
     * @return {@code boolean}; the mouse released result
     */
    public boolean mouseReleased(float mouseX, float mouseY, int button) {
        if (!visible || !enabled || !interactable) {
            return false;
        }

        boolean wasPressed = pressed;
        pressed = false;

        List<UIElement> sorted = new ArrayList<>(children);
        sorted.sort(Comparator.comparingInt(UIElement::getZIndex).reversed());

        for (UIElement child : sorted) {
            if (child.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (!wasPressed) {
            return false;
        }

        if (contains(mouseX, mouseY)) {
            if (mouseReleaseListener != null) {
                mouseReleaseListener.accept(this);
            }

            if (clickListener != null) {
                clickListener.accept(this);
            }

            return true;
        }

        return false;
    }

    /**
     * Handles mouse scrolled and applies its effect to the current interaction state.
     * @param mouseX the {@code float} supplied as {@code mouseX}
     * @param mouseY the {@code float} supplied as {@code mouseY}
     * @param scrollX the {@code float} supplied as {@code scrollX}
     * @param scrollY the {@code float} supplied as {@code scrollY}
     * @return {@code boolean}; the mouse scrolled result
     */
    public boolean mouseScrolled(float mouseX, float mouseY, float scrollX, float scrollY) {
        if (!visible || !enabled || !interactable) {
            return false;
        }

        List<UIElement> sorted = new ArrayList<>(children);
        sorted.sort(Comparator.comparingInt(UIElement::getZIndex).reversed());

        for (UIElement child : sorted) {
            if (child.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }

        return contains(mouseX, mouseY);
    }

    /**
     * Handles key pressed and applies its effect to the current interaction state.
     * @param key the {@code int} supplied as {@code key}
     * @param scancode the {@code int} supplied as {@code scancode}
     * @param modifiers the {@code int} supplied as {@code modifiers}
     * @return {@code boolean}; the key pressed result
     */
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (!visible || !enabled) {
            return false;
        }

        List<UIElement> sorted = new ArrayList<>(children);
        sorted.sort(Comparator.comparingInt(UIElement::getZIndex).reversed());

        for (UIElement child : sorted) {
            if (child.keyPressed(key, scancode, modifiers)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Handles key released and applies its effect to the current interaction state.
     * @param key the {@code int} supplied as {@code key}
     * @param scancode the {@code int} supplied as {@code scancode}
     * @param modifiers the {@code int} supplied as {@code modifiers}
     * @return {@code boolean}; the key released result
     */
    public boolean keyReleased(int key, int scancode, int modifiers) {
        if (!visible || !enabled) {
            return false;
        }

        List<UIElement> sorted = new ArrayList<>(children);
        sorted.sort(Comparator.comparingInt(UIElement::getZIndex).reversed());

        for (UIElement child : sorted) {
            if (child.keyReleased(key, scancode, modifiers)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Handles char typed and applies its effect to the current interaction state.
     * @param codepoint the {@code int} supplied as {@code codepoint}
     * @return {@code boolean}; the char typed result
     */
    public boolean charTyped(int codepoint) {
        if (!visible || !enabled) {
            return false;
        }

        List<UIElement> sorted = new ArrayList<>(children);
        sorted.sort(Comparator.comparingInt(UIElement::getZIndex).reversed());

        for (UIElement child : sorted) {
            if (child.charTyped(codepoint)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Handles click and updates the affected state.
     * @param listener the {@link Consumer} supplied as {@code listener}
     */
    public void onClick(Consumer<UIElement> listener) {
        this.clickListener = listener;
    }

    /**
     * Handles mouse enter and updates the affected state.
     * @param listener the {@link Consumer} supplied as {@code listener}
     */
    public void onMouseEnter(Consumer<UIElement> listener) {
        this.mouseEnterListener = listener;
    }

    /**
     * Handles mouse exit and updates the affected state.
     * @param listener the {@link Consumer} supplied as {@code listener}
     */
    public void onMouseExit(Consumer<UIElement> listener) {
        this.mouseExitListener = listener;
    }

    /**
     * Handles mouse press and updates the affected state.
     * @param listener the {@link Consumer} supplied as {@code listener}
     */
    public void onMousePress(Consumer<UIElement> listener) {
        this.mousePressListener = listener;
    }

    /**
     * Handles mouse release and updates the affected state.
     * @param listener the {@link Consumer} supplied as {@code listener}
     */
    public void onMouseRelease(Consumer<UIElement> listener) {
        this.mouseReleaseListener = listener;
    }

    /**
     * Handles focus and updates the affected state.
     * @param listener the {@link Consumer} supplied as {@code listener}
     */
    public void onFocus(Consumer<UIElement> listener) {
        this.focusListener = listener;
    }

    /**
     * Handles blur and updates the affected state.
     * @param listener the {@link Consumer} supplied as {@code listener}
     */
    public void onBlur(Consumer<UIElement> listener) {
        this.blurListener = listener;
    }

    /**
     * Returns the sorted children.
     * @return the {@link List} representing the sorted children
     */
    public List<UIElement> getSortedChildren() {
        List<UIElement> sorted = new ArrayList<>(children);
        sorted.sort(Comparator.comparingInt(UIElement::getZIndex));
        return sorted;
    }

    /**
     * Returns the world opacity.
     * @return {@code float}; the world opacity
     */
    public float getWorldOpacity() {
        if (parent == null) {
            return opacity;
        }

        return parent.getWorldOpacity() * opacity;
    }

    /**
     * Checks whether the actually visible condition is met.
     * @return {@code true} if actually visible; otherwise {@code false}
     */
    public boolean isActuallyVisible() {
        if (!visible) {
            return false;
        }

        return parent == null || parent.isActuallyVisible();
    }

    /**
     * Checks whether the actually enabled condition is met.
     * @return {@code true} if actually enabled; otherwise {@code false}
     */
    public boolean isActuallyEnabled() {
        if (!enabled) {
            return false;
        }

        return parent == null || parent.isActuallyEnabled();
    }

    /**
     * Releases the resources associated with this object.
     */
    public void dispose() {
        for (UIElement child : children) {
            child.dispose();
        }

        children.clear();
        parent = null;
    }

    /**
     * {@inheritDoc}
     * Produces the textual or converted representation for to string.
     * @return the {@link String} representing the to string result
     */
    @Override
    public String toString() {
        return "UIElement {" +
                "name=" + getClass().getSimpleName() +
                ", children=" + children.size() +
                ", parent=" + parent.getClass().getSimpleName() +
                ", x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
               '}';
    }

    /**
     * Enumerates the supported anchor values.
     */
    public enum Anchor {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        CENTER_LEFT,
        CENTER,
        CENTER_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT
    }
}
