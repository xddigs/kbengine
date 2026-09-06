package com.isofarm.ui;

import com.isofarm.data.*;
import com.isofarm.graphics.SpriteSheet;
import com.isofarm.graphics.ResourceManager;
import com.isofarm.graphics.Texture;
import com.isofarm.item.Bucket;
import com.isofarm.item.Block;
import com.isofarm.item.Item;
import com.isofarm.item.Tool;
import com.isofarm.utils.K;
import com.isofarm.utils.Settings;
import org.joml.Vector4f;

import java.lang.Character;

/**
 * Encapsulates the state and operations required by inventory slot ui within the game runtime.
 */
@SuppressWarnings("all")
public class InventorySlotUI extends UIElement {
    private static final int GUI_SLICE_SIZE = 3;
    private final SlotType slotType;
    private InventorySlot slot;
    private InventorySlot backpackSlot;
    private SpriteSheet spriteSheet;
    private int spriteFrame;
    private boolean selected;
    private boolean hovered;
    private boolean selectedOutline = false;
    private Vector4f selectionOutlineColor = K.UI.UI_HOTBAR_SELECTED_COLOR;
    private float selectionOutlineThickness = Settings.getScaledThickness() * 2f;
    private int lastAmount = 0;
    private Item lastItem = null;
    private float squishTimer = 0.0f;
    /**
     * Creates a new {@code InventorySlotUI} instance.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param width the {@code float} supplied as {@code width}
     * @param height the {@code float} supplied as {@code height}
     * @param slotType the {@link SlotType} supplied as {@code slotType}
     */
    public InventorySlotUI(float x, float y, float width, float height, SlotType slotType) {
        super(x, y, width, height);
        this.slotType = slotType;
        setFocusable(true);
        setSlot(getSlotType());
    }

    /**
     * {@inheritDoc}
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    @Override
    public void update(float delta) {
        super.update(delta);
        int currentAmount = slot != null ? slot.getAmount() : 0;
        Item currentItem = getItem();

        if ((currentItem instanceof Bucket
                || (currentItem instanceof Block block && block.getType() == BlockData.TORCH))
                && spriteSheet != null) {
            spriteFrame = ResourceManager.getItemFrame(currentItem);
        }

        if (currentAmount != lastAmount || currentItem != lastItem) {
            if (currentAmount > 0) {
                triggerSquish();
            }
            lastAmount = currentAmount;
            lastItem = currentItem;
        }

        if (squishTimer > 0.0f) {
            squishTimer -= delta;
            if (squishTimer < 0.0f) {
                squishTimer = 0.0f;
            }
        }
    }

    /**
     * Updates or derives runtime state for trigger squish according to the supplied arguments.
     */
    public void triggerSquish() {
        this.squishTimer = K.UI.SQUISH_DURATION;
    }

    /**
     * {@inheritDoc}
     * Renders this object in the requested render pass.
     */
    @Override
    public void render() {
        float x = getAbsoluteX();
        float y = getAbsoluteY();
        float width = getAbsoluteWidth();
        float height = getAbsoluteHeight();

        int textureWidth = Math.max(GUI_SLICE_SIZE * 2,
                Math.round(width / Settings.getScale()));
        int textureHeight = Math.max(GUI_SLICE_SIZE * 2,
                Math.round(height / Settings.getScale()));
        Texture slotTexture = Frontend.createNineSliceTexture(
                ResourceManager.rem.getBackgroundUI(), textureWidth,
                textureHeight, GUI_SLICE_SIZE);
        Frontend.drawTexture(slotTexture, x, y, width, height,
                new Vector4f(1.0f, 1.0f, 1.0f, getWorldOpacity()));

        if (!isEmpty() && spriteSheet != null) {
            renderItem();
        }

        renderChildren();
    }

    /**
     * Returns the slot type.
     * @return the {@link InventorySlot} representing the slot type
     */
    public InventorySlot getSlotType() {
        return switch (slotType) {
            case BACKPACK -> backpackSlot;
            case INVENTORY, HOTBAR -> slot;
            default -> null;
        };
    }

    /**
     * Sets the slot.
     * @param slot the {@link InventorySlot} supplied as {@code slot}
     */
    public void setSlot(InventorySlot slot) {
        if (slotType == SlotType.BACKPACK) {
            this.backpackSlot = slot;
        } else {
            this.slot = slot;
        }
    }

    /**
     * Renders the item.
     */
    private void renderItem() {
        float iconSize = Settings.getScaledIcon();
        float scaleX = 1.0f;
        float scaleY = 1.0f;

        if (squishTimer > 0.0f) {
            float progress = squishTimer / K.UI.SQUISH_DURATION;
            float deformation = (float) Math.sin(progress * Math.PI * 2.0) * progress * 0.25f;
            scaleX = 1.0f + deformation;
            scaleY = 1.0f - deformation;
        }

        float renderWidth = iconSize * scaleX;
        float renderHeight = iconSize * scaleY;
        float x = Math.round(getAbsoluteX() + (getAbsoluteWidth() - renderWidth) * 0.5f);
        float y = Math.round(getAbsoluteY() + (getAbsoluteHeight() - renderHeight) * 0.5f);

        if (hovered) {
            Frontend.drawSpriteOutline(spriteSheet, spriteFrame, x, y,
                    renderWidth, renderHeight, Settings.getScale(),
                    new Vector4f(1.0f, 1.0f, 1.0f, getWorldOpacity()));
        }

        Frontend.drawSprite(spriteSheet, spriteFrame, x, y, renderWidth, renderHeight,
                new Vector4f(K.UI.UI_ITEM_TINT.x, K.UI.UI_ITEM_TINT.y, K.UI.UI_ITEM_TINT.z,
                        K.UI.UI_ITEM_TINT.w * getWorldOpacity()));

        renderToolDurability();
        renderAmount();
    }

    /**
     * Renders the tool durability.
     */
    private void renderToolDurability() {
        Item item = getItem();
        if (!(item instanceof Tool tool)) return;

        float maxDurability = tool.getMaxDurability();
        if (maxDurability <= 0.0f) return;

        float durability = Math.max(0.0f, Math.min(tool.getDurability(), maxDurability));
        if (durability >= maxDurability) return;
        float progress = durability / maxDurability;

        float padding = Settings.scale(2.0f);
        float barHeight = Settings.getScaledBorder() - 2f;
        float barWidth = getAbsoluteWidth() - padding * 2.0f;
        float x = getAbsoluteX() + padding;
        float y = getAbsoluteY() + getAbsoluteHeight() - barHeight - padding;
        Frontend.drawRect(x, y, barWidth, barHeight,
                new Vector4f(0.0f, 0.0f, 0.0f, getWorldOpacity()));

        if (progress <= 0.0f) return;
        float red, green, blue = 0.0f;
        if (progress > 0.5f) {
            float t = (progress - 0.5f) * 2.0f;
            red = 1.0f - t;
            green = 1.0f;
        } else if (progress > 0.25f) {
            float t = (progress - 0.25f) * 4.0f;
            red = 1.0f;
            green = 0.5f + t * 0.5f;
        } else {
            float t = progress * 4.0f;
            red = 1.0f;
            green = t * 0.5f;
        }

        Frontend.drawRect(x, y, barWidth * progress, barHeight,
                new Vector4f(red, green, blue, getWorldOpacity()));
    }

    /**
     * Renders the amount.
     */
    private void renderAmount() {
        Item item = getItem();
        InventorySlot currentSlot = getSlotType();

        if (currentSlot == null) return;
        if (item == null || currentSlot.getAmount() <= 1) {
            return;
        }

        String amount = String.valueOf(currentSlot.getAmount());
        final float offset = 5.0f;
        float textWidth = getTextWidth(amount);
        float textHeight = Frontend.getNormalFont().getSize();
        float paddingX = Settings.scale(2.0f);
        float paddingY = Settings.scale(1.0f);
        float x = getAbsoluteX() + getAbsoluteWidth() - textWidth - paddingX;
        float y = getAbsoluteY() + getAbsoluteHeight() - textHeight - paddingY;

        Frontend.drawNormalString(amount, x, y + textHeight, new Vector4f(K.UI.UI_TEXT_COLOR.x,
                K.UI.UI_TEXT_COLOR.y, K.UI.UI_TEXT_COLOR.z, K.UI.UI_TEXT_COLOR.w * getWorldOpacity()));
    }

    /**
     * Returns the text width.
     * @param value the {@link String} supplied as {@code value}
     * @return {@code float}; the text width
     */
    private float getTextWidth(String value) {
        float width = 0.0f;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            var glyph = Frontend.getNormalFont().getGlyph(codePoint);

            if (glyph != null) {
                width += glyph.xadvance();
            }

            i += Character.charCount(codePoint);
        }

        return width;
    }

    /**
     * Returns the slot.
     * @return the {@link InventorySlot} representing the slot
     */
    public InventorySlot getSlot() {
        return slot;
    }

    /**
     * Returns the backpack slot.
     * @return the {@link InventorySlot} representing the backpack slot
     */
    public InventorySlot getBackpackSlot() {
        return backpackSlot;
    }

    /**
     * Returns the item.
     * @return the {@link Item} representing the item
     */
    public Item getItem() {
        InventorySlot currentSlot = getSlotType();
        return currentSlot != null ? currentSlot.getItem() : null;
    }

    /**
     * Checks whether the empty condition is met.
     * @return {@code true} if empty; otherwise {@code false}
     */
    public boolean isEmpty() {
        InventorySlot currentSlot = getSlotType();
        return currentSlot == null || currentSlot.isEmpty();
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
     * Returns the sprite frame.
     * @return {@code int}; the sprite frame
     */
    public int getSpriteFrame() {
        return spriteFrame;
    }

    /**
     * Sets the sprite frame.
     * @param spriteFrame the {@code int} supplied as {@code spriteFrame}
     */
    public void setSpriteFrame(int spriteFrame) {
        this.spriteFrame = Math.max(0, spriteFrame);
    }

    /**
     * Checks whether the selected condition is met.
     * @return {@code true} if selected; otherwise {@code false}
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the selected.
     * @param selected the {@code boolean} supplied as {@code selected}
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
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
        this.hovered = hovered;
    }
}
