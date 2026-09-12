package org.kbeng.ui;

import org.kbeng.data.Inventory;
import org.kbeng.data.InventorySlot;
import org.kbeng.data.SlotType;
import org.kbeng.entity.Player;
import org.kbeng.graphics.ResourceManager;
import org.kbeng.graphics.SpriteSheet;
import org.kbeng.graphics.Texture;
import org.kbeng.input.ControlAction;
import org.kbeng.input.Controls;
import org.kbeng.input.Mouse;
import org.kbeng.item.Backpack;
import org.kbeng.item.Item;
import org.kbeng.utils.K;
import org.kbeng.utils.Settings;
import org.kbeng.wrld.GameMaster;
import org.joml.Vector4f;

/**
 * HotbarUI provides hotbar ui capabilities within the ui subsystem.
 *
 * It drives retained UI composition, user interaction handling, and rendering behavior for in-game screens.
 *
 * The UI component owns presentation behavior and mediates interaction between input and visual state.
 *
 * It extends UIElement, inheriting shared behavior while specializing subsystem-specific logic.
 */
public class HotbarUI extends UIElement {
    private final InventorySlotUI[] slotUIs = new InventorySlotUI[K.UI.INVENTORY_COLUMNS];
    private final Player player = Player.plyr;
    private Inventory inventory;
    private InventorySlotUI backpackSlotUI;
    private InventorySlotUI shieldSlotUI;
    private Item lastSelectedItem;

    private SpriteSheet seedIcons;
    private SpriteSheet cropIcons;
    private SpriteSheet blockIcons;
    private SpriteSheet toolIcons;
    private SpriteSheet materialIcons;
    private SpriteSheet armorIcons;
    private SpriteSheet inventoryIcons;

    private int selectedSlot = 0;
    private boolean inventoryMode = false;

    /**
     * Creates a new {@code HotbarUI} instance.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     */
    public HotbarUI(float x, float y) {
        super(x, y, getInitialHotbarWidth(), getHotbarHeight());
        setFocusable(true);
        createSlots();
        setLayer(50);
    }

    /**
     * {@inheritDoc}
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    @Override
    public void update(float delta) {
        super.update(delta);

        if (player == null) {
            return;
        }

        syncInventory();
        syncExtraSlots();
        updateSlots();
        interact();
        updateSelectedItem();
    }

    /**
     * {@inheritDoc}
     * Renders this object in the requested render pass.
     */
    @Override
    public void render() {
        renderChildren();
        renderSelector();
    }

    /**
     * Returns the selected item.
     * @return the {@link Item} representing the selected item
     */
    public Item getSelectedItem() {
        InventorySlot slot = getSelectedInventorySlot();
        return slot != null ? slot.getItem() : null;
    }

    /**
     * Updates the selected item.
     */
    private void updateSelectedItem() {
        InventorySlot selected = getSelectedInventorySlot();
        Item item = selected != null ? selected.getItem() : null;

        if (item != lastSelectedItem) {
            lastSelectedItem = item;
            Settings.selectedItem = item;
        }
    }

    /**
     * Returns the initial hotbar width.
     * @return {@code float}; the initial hotbar width
     */
    private static float getInitialHotbarWidth() {
        float padding = Settings.getScaledPadding();
        float slot = Settings.getScaledSlot();
        float spacing = Settings.getScaledSpacing();

        return padding * 2.0f + K.UI.INVENTORY_COLUMNS * slot
                + (K.UI.INVENTORY_COLUMNS - 1) * spacing;
    }

    /**
     * Returns the hotbar width.
     * @return {@code float}; the hotbar width
     */
    public float getHotbarWidth() {
        float padding = Settings.getScaledPadding();
        float slot = Settings.getScaledSlot();
        float spacing = Settings.getScaledSpacing();
        float width = padding * 2.0f
                + K.UI.INVENTORY_COLUMNS * slot
                + (K.UI.INVENTORY_COLUMNS - 1) * spacing;

        if (inventory != null && inventory.hasBackpackEquipped()) {
            width += spacing * 2.0f;
            width += slot;
        }

        return width;
    }

    /**
     * Returns the hotbar height.
     * @return {@code float}; the hotbar height
     */
    private static float getHotbarHeight() {
        return Settings.getScaledPadding() * 2.0f +
                Settings.getScaledSlot();
    }

    /**
     * Returns the max selectable slots.
     * @return {@code int}; the max selectable slots
     */
    public int getMaxSelectableSlots() {
        int max = K.UI.INVENTORY_COLUMNS;
        if (inventory != null && inventory.hasBackpackEquipped()) {
            max++;
        }
        return max;
    }

    /**
     * Updates or derives runtime state for select slot according to the supplied arguments.
     * @param index the {@code int} supplied as {@code index}
     */
    public void selectSlot(int index) {
        if (index < 0 || index >= getMaxSelectableSlots()) {
            return;
        }
        selectedSlot = index;
        updateSelectedItem();
    }

    /**
     * Updates or derives runtime state for select next according to the supplied arguments.
     */
    public void selectNext() {
        selectSlot((selectedSlot + 1) % getMaxSelectableSlots());
    }

    /**
     * Updates or derives runtime state for select previous according to the supplied arguments.
     */
    public void selectPrevious() {
        int max = getMaxSelectableSlots();
        selectSlot((selectedSlot - 1 + max) % max);
    }

    /**
     * Refreshes dependent runtime state for refresh size.
     */
    public void refreshSize() {
        setWidth(getHotbarWidth());
    }

    /**
     * Returns the extra slot index.
     * @param item the {@link Item} supplied as {@code item}
     * @return {@code int}; the extra slot index
     */
    private int getExtraSlotIndex(Item item) {
        if (inventory == null || item == null) {
            return -1;
        }
        return inventory.getBackpackSlot().getItem() == item
                ? K.UI.INVENTORY_COLUMNS : -1;
    }

    /**
     * Returns the extra slot ui.
     * @param index the {@code int} supplied as {@code index}
     * @return the {@link InventorySlotUI} representing the extra slot ui
     */
    private InventorySlotUI getExtraSlotUI(int index) {
        if (inventory == null) {
            return null;
        }

        return index == K.UI.INVENTORY_COLUMNS
                && inventory.hasBackpackEquipped() ? backpackSlotUI : null;
    }

    /**
     * Creates and returns the slots.
     */
    private void createSlots() {
        for (int i = 0; i < K.UI.INVENTORY_COLUMNS; i++) {
            float x = Settings.getScaledPadding() + i * (Settings.getScaledSlot() +
                    Settings.getScaledSpacing());

            float y = Settings.getScaledPadding();
            InventorySlotUI slotUI = new InventorySlotUI(x, y, Settings.getScaledSlot(),
                    Settings.getScaledSlot(),
                    SlotType.HOTBAR);
            slotUIs[i] = slotUI;
            addChild(slotUI);
        }

        float backpackX = Settings.getScaledPadding() +
                K.UI.INVENTORY_COLUMNS * (Settings.getScaledSlot() +
                        Settings.getScaledSpacing()) +
                Settings.getScaledSpacing() * 2.0f;

        backpackSlotUI = new InventorySlotUI(backpackX, Settings.getScaledPadding(),
                Settings.getScaledSlot(), Settings.getScaledSlot(),
                SlotType.HOTBAR);

        backpackSlotUI.hide();
        addChild(backpackSlotUI);

        float shieldX = -Settings.getScaledSlot() - Settings.getScaledSpacing();
        shieldSlotUI = new InventorySlotUI(shieldX, Settings.getScaledPadding(),
                Settings.getScaledSlot(), Settings.getScaledSlot(), SlotType.SHIELD);
        shieldSlotUI.hide();
        addChild(shieldSlotUI);
    }

    /**
     * Returns the inventory.
     * @return the {@link Inventory} representing the inventory
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Sets the inventory.
     * @param inventory the {@link Inventory} supplied as {@code inventory}
     */
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Sets the inventory mode.
     * @param inventoryMode the {@code boolean} supplied as {@code inventoryMode}
     */
    public void setInventoryMode(boolean inventoryMode) {
        this.inventoryMode = inventoryMode;
    }

    /**
     * Refreshes dependent runtime state for sync inventory.
     */
    private void syncInventory() {
        if (player == null) {
            return;
        }

        int hotbarStart = inventory.getHotbarStart();
        for (int i = 0; i < K.UI.INVENTORY_COLUMNS; i++) {
            InventorySlotUI slotUI = slotUIs[i];
            int inventoryIndex = hotbarStart + i;
            slotUI.setSlot(inventoryIndex < inventory.getSlots().size() ?
                    inventory.getSlot(inventoryIndex) : null);
            updateItemSprite(slotUI);
        }
    }

    /**
     * Refreshes dependent runtime state for sync extra slots.
     */
    private void syncExtraSlots() {
        backpackSlotUI.hide();
        shieldSlotUI.hide();

        if (inventory == null) {
            return;
        }

        if (inventory.getShield() != null || inventoryMode) {
            shieldSlotUI.setSlot(inventory.getShieldSlot());
            updateItemSprite(shieldSlotUI);
            shieldSlotUI.show();
        }

        if (inventory.hasBackpackEquipped()) {
            backpackSlotUI.setSlot(inventory.getBackpackSlot());
            updateItemSprite(backpackSlotUI);
            backpackSlotUI.show();
        }

        int maxSlots = getMaxSelectableSlots();
        if (selectedSlot >= maxSlots) {
            selectedSlot = Math.max(0, maxSlots - 1);
        }
    }

    /**
     * Updates the item sprite.
     * @param slotUI the {@link InventorySlotUI} supplied as {@code slotUI}
     */
    private void updateItemSprite(InventorySlotUI slotUI) {
        Item item = slotUI.getItem();

        if (item == null) {
            slotUI.setSpriteSheet(null);
            slotUI.setSpriteFrame(0);
            slotUI.setTooltipText(null);
            return;
        }

        SpriteSheet spriteSheet = ResourceManager.getItemSpriteSheet(item);
        if (spriteSheet == null) {
            slotUI.setSpriteSheet(null);
            slotUI.setSpriteFrame(0);
            slotUI.setTooltipText(null);
            return;
        }

        slotUI.setSpriteSheet(spriteSheet);
        slotUI.setSpriteFrame(ResourceManager.getItemFrame(item));
        slotUI.setTooltipText(item.getDisplayName());
    }

    /**
     * Updates the slots.
     */
    private void updateSlots() {
        for (InventorySlotUI slotUI : slotUIs) {
            slotUI.setSelected(false);
            slotUI.setHovered(isSlotHovered(slotUI));
        }

        backpackSlotUI.setSelected(false);
        backpackSlotUI.setHovered(isSlotHovered(backpackSlotUI));
        shieldSlotUI.setSelected(false);
        shieldSlotUI.setHovered(shieldSlotUI.isVisible() && isSlotHovered(shieldSlotUI));

        if (player == null || inventory == null) {
            return;
        }

        if (selectedSlot >= 0 &&
                selectedSlot < K.UI.INVENTORY_COLUMNS) {
            slotUIs[selectedSlot].setSelected(true);
            return;
        }

        if (selectedSlot == K.UI.INVENTORY_COLUMNS
                && inventory.hasBackpackEquipped()) {
            backpackSlotUI.setSelected(true);
        }
    }

    /**
     * Handles interact and applies its effect to the current interaction state.
     */
    private void interact() {
        boolean isLeftClick = Controls.isPressed(ControlAction.UI_SELECT);
        boolean isCtrlHeld = Controls.isDown(ControlAction.MODIFIER);

        if (isLeftClick) {
            if (backpackSlotUI.isVisible() && isSlotHovered(backpackSlotUI)) {
                int index = getExtraSlotIndex(backpackSlotUI.getItem());
                if (index >= 0) {
                    selectSlot(index);
                }
                if (backpackSlotUI.getItem() instanceof Backpack backpack) {
                    backpack.use(GameMaster.game, isCtrlHeld);
                }
                return;
            }
        }

        if (inventoryMode) return;
        if (!isLeftClick) return;
        for (int i = 0; i < slotUIs.length; i++) {
            if (slotUIs[i].isHovered()) {
                selectSlot(i);
                break;
            }
        }
    }

    /**
     * Checks whether the slot hovered condition is met.
     * @param slotUI the {@link InventorySlotUI} supplied as {@code slotUI}
     * @return {@code true} if slot hovered; otherwise {@code false}
     */
    private boolean isSlotHovered(InventorySlotUI slotUI) {
        float mouseX = Mouse.getX();
        float mouseY = Mouse.getY();

        float x = slotUI.getAbsoluteX();
        float y = slotUI.getAbsoluteY();
        float width = slotUI.getAbsoluteWidth();
        float height = slotUI.getAbsoluteHeight();
        return mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    /**
     * Returns the selected inventory slot.
     * @return the {@link InventorySlot} representing the selected inventory slot
     */
    public InventorySlot getSelectedInventorySlot() {
        if (player == null || inventory == null) {
            return null;
        }

        if (selectedSlot == K.UI.INVENTORY_COLUMNS) {
            return inventory.hasBackpackEquipped()
                    ? inventory.getBackpackSlot() : null;
        }

        int hotbarStart = inventory.getHotbarStart();
        int inventoryIndex = hotbarStart + selectedSlot;
        if (inventoryIndex < 0 ||
                inventoryIndex >= inventory.getSlots().size()) {
            return null;
        }
        return inventory.getSlot(inventoryIndex);
    }

    /**
     * Renders the hotbar selector.
     */
    private void renderSelector() {
        InventorySlotUI slot;
        if (selectedSlot < K.UI.INVENTORY_COLUMNS) {
            if (selectedSlot < 0 || selectedSlot >= slotUIs.length) {
                return;
            }
            slot = slotUIs[selectedSlot];
        } else {
            slot = getExtraSlotUI(selectedSlot);
            if (slot == null || !slot.isVisible()) {
                return;
            }
        }

        Texture selector = ResourceManager.rem.getSelectorUI();
        if (selector == null) {
            return;
        }

        float padding = Settings.getScaledPadding();
        float width = slot.getAbsoluteWidth() + (padding * 2.0f);
        float height = slot.getAbsoluteHeight() + (padding * 2.0f);
        float x = slot.getAbsoluteX() - padding;
        float y = slot.getAbsoluteY() - padding;
        Frontend.drawTexture(selector, x, y, width, height, new Vector4f(1.0f));
    }

    /**
     * Returns the seed icons.
     * @return the {@link SpriteSheet} representing the seed icons
     */
    public SpriteSheet getSeedIcons() {
        return seedIcons;
    }

    /**
     * Sets the seed icons.
     * @param seedIcons the {@link SpriteSheet} supplied as {@code seedIcons}
     */
    public void setSeedIcons(SpriteSheet seedIcons) {
        this.seedIcons = seedIcons;
    }

    /**
     * Returns the crop icons.
     * @return the {@link SpriteSheet} representing the crop icons
     */
    public SpriteSheet getCropIcons() {
        return cropIcons;
    }

    /**
     * Sets the crop icons.
     * @param cropIcons the {@link SpriteSheet} supplied as {@code cropIcons}
     */
    public void setCropIcons(SpriteSheet cropIcons) {
        this.cropIcons = cropIcons;
    }

    /**
     * Returns the block icons.
     * @return the {@link SpriteSheet} representing the block icons
     */
    public SpriteSheet getBlockIcons() {
        return blockIcons;
    }

    /**
     * Sets the block icons.
     * @param blockIcons the {@link SpriteSheet} supplied as {@code blockIcons}
     */
    public void setBlockIcons(SpriteSheet blockIcons) {
        this.blockIcons = blockIcons;
    }

    /**
     * Returns the tool icons.
     * @return the {@link SpriteSheet} representing the tool icons
     */
    public SpriteSheet getToolIcons() {
        return toolIcons;
    }

    /**
     * Sets the tool icons.
     * @param toolIcons the {@link SpriteSheet} supplied as {@code toolIcons}
     */
    public void setToolIcons(SpriteSheet toolIcons) {
        this.toolIcons = toolIcons;
    }

    /**
     * Returns the material icons.
     * @return the {@link SpriteSheet} representing the material icons
     */
    public SpriteSheet getMaterialIcons() {
        return materialIcons;
    }

    /**
     * Sets the material icons.
     * @param materialIcons the {@link SpriteSheet} supplied as {@code materialIcons}
     */
    public void setMaterialIcons(SpriteSheet materialIcons) {
        this.materialIcons = materialIcons;
    }

    /**
     * Returns the {@code armorIcons} value
     * @return {@link SpriteSheet} value of armorIcons
     */
    public SpriteSheet getArmorIcons() {
        return armorIcons;
    }

    /**
     * Sets the armorIcons value
     * @return {@link SpriteSheet} value of armorIcons
     */
    public HotbarUI setArmorIcons(SpriteSheet armorIcons) {
        this.armorIcons = armorIcons;
        return this;
    }

    /**
     * Returns the inventory icons.
     * @return the {@link SpriteSheet} representing the inventory icons
     */
    public SpriteSheet getInventoryIcons() {
        return inventoryIcons;
    }

    /**
     * Sets the inventory icons.
     * @param inventoryIcons the {@link SpriteSheet} supplied as {@code inventoryIcons}
     */
    public void setInventoryIcons(SpriteSheet inventoryIcons) {
        this.inventoryIcons = inventoryIcons;
    }

    /**
     * Returns the selected slot.
     * @return {@code int}; the selected slot
     */
    public int getSelectedSlot() {
        return selectedSlot;
    }

    /**
     * Returns the slot uis.
     * @return an array of {@link InventorySlotUI} values; the slot uis
     */
    public InventorySlotUI[] getSlotUIs() {
        return slotUIs.clone();
    }

    /** Returns the left-hand equipment slot rendered beside the hotbar. */
    public InventorySlotUI getShieldSlotUI() {
        return shieldSlotUI;
    }
}
