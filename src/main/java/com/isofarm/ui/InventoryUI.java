package com.isofarm.ui;

import com.isofarm.data.*;
import com.isofarm.entity.NPC;
import com.isofarm.entity.Player;
import com.isofarm.graphics.ResourceManager;
import com.isofarm.graphics.SpriteSheet;
import com.isofarm.graphics.Texture;
import com.isofarm.input.ControlAction;
import com.isofarm.input.Controls;
import com.isofarm.input.Mouse;
import com.isofarm.item.*;
import com.isofarm.service.SoundService;
import com.isofarm.utils.K;
import com.isofarm.utils.Settings;
import com.isofarm.wrld.GameMaster;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.joml.Math.lerp;

/**
 * Encapsulates the state and operations required by inventory ui within the game runtime.
 */
@SuppressWarnings("all")
@GodObject
public class InventoryUI extends UIElement {
    private enum CreativeFilter {
        ALL(null),
        FOOD("inventory.filter.food"),
        BLOCKS("inventory.filter.blocks"),
        TOOLS("inventory.filter.tools"),
        MATERIALS("inventory.filter.materials"),
        PRODUCE("inventory.filter.produce"),
        SEEDS("inventory.filter.seeds"),
        USABLES("inventory.filter.usables");

        private final String translationKey;

        CreativeFilter(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    private static final int BACKPACK_COLUMNS = 4;
    private static final int BACKPACK_ROWS = 4;
    private static final int GUI_SLICE_SIZE = 3;
    private static final float QUICK_MOVE_ANIMATION_DURATION_SECONDS = 0.12f;
    private static final float ANIMATION_COMPLETE = 1.0f;

    private static final Logger log = LoggerFactory.getLogger(InventoryUI.class);

    private final InventorySlotUI[] slotUIs;
    private final InventorySlotUI[] armorSlotUIs;
    private final InventorySlotUI[] containerSlotUIs;
    private final InventorySlot[] creativeSlotData;
    private final Set<InventorySlot> creativeSlots;
    private final List<Item> creativeItems;
    private final List<QuickMoveAnimation> quickMoveAnimations;

    private final List<UIButton> buttons;
    private final UIScrollBar creativeScrollBar;
    private final Player player = Player.plyr;
    private final Map<CreativeFilter, UIButton> creativeFilterButtons =
            new EnumMap<>(CreativeFilter.class);
    private UIButton sortButton;
    private UIButton groupButton;
    private UIButton backpackButton;
    private UIButton inventoryModeButton;
    private Inventory inventory;
    private iBlock containerBlock;
    private Inventory externalInventory;
    private SpriteSheet seedIcons;
    private SpriteSheet cropIcons;
    private SpriteSheet blockIcons;
    private SpriteSheet toolIcons;
    private SpriteSheet materialIcons;
    private SpriteSheet armorIcons;
    private SpriteSheet inventoryIcons;
    private Item carriedItem;
    private HotbarUI hotbarUI;
    private BackpackInventoryUI backpackUI;
    private int carriedAmount;
    private boolean isGodmode;
    private boolean isCreativeInventoryVisible;
    private CreativeFilter creativeFilter = CreativeFilter.ALL;

    private float defaultX;
    private float targetX;
    private float defaultY;
    private float targetY;
    private boolean isClosing = false;

    private boolean isBackpackOpen = false;
    private boolean isBackpackClosing = false;
    private float backpackTargetY;
    private float backpackCurrentY;

    /**
     * Creates a new {@code InventoryUI} instance.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     */
    public InventoryUI(float x, float y) {
        super(x, y, getInventoryWidth(), getInventoryHeight());
        defaultX = x;
        targetX = x;
        defaultY = y;
        targetY = y + 1000.0f;
        setPosition(x, targetY);

        int totalVisualSlots = K.UI.INVENTORY_SLOTS;
        this.slotUIs = new InventorySlotUI[totalVisualSlots];
        this.armorSlotUIs = new InventorySlotUI[ArmorSlot.values().length];
        this.containerSlotUIs = new InventorySlotUI[totalVisualSlots];
        this.creativeSlotData = new InventorySlot[totalVisualSlots];
        this.creativeSlots = Collections.newSetFromMap(new IdentityHashMap<>());
        this.creativeItems = new ArrayList<>();
        this.quickMoveAnimations = new ArrayList<>();
        this.buttons = new ArrayList<>();
        this.creativeScrollBar = createCreativeScrollBar();
        setFocusable(true);
        addChild(creativeScrollBar);
        createButtons();

        setLayer(150);
        hide();
    }

    /**
     * Returns the inventory width.
     * @return {@code float}; the inventory width
     */
    private static float getInventoryWidth() {
        return Settings.getScaledPadding() * 2.0f +
                K.UI.INVENTORY_COLUMNS * Settings.getScaledSlot() +
                (K.UI.INVENTORY_COLUMNS - 1) * Settings.getScaledSpacing();
    }

    /**
     * Returns the inventory height.
     * @return {@code float}; the inventory height
     */
    private static float getInventoryHeight() {
        return Settings.getScaledPadding() * 2.0f + Settings.getScaledHeader() +
                K.UI.INVENTORY_ROWS * Settings.getScaledSlot() +
                (K.UI.INVENTORY_ROWS - 1) * Settings.getScaledSpacing();
    }

    /**
     * Creates the row-aligned scroll bar used by the creative catalog.
     * Its track covers only the slot rows and their intervening spacing.
     * @return the configured creative scroll bar
     */
    private UIScrollBar createCreativeScrollBar() {
        float spacing = Settings.getScaledSpacing();
        float x = Settings.getScaledPadding()
                + K.UI.INVENTORY_COLUMNS * Settings.getScaledSlot()
                + K.UI.INVENTORY_COLUMNS * spacing;
        float y = Settings.getScaledPadding() + Settings.getScaledHeader();
        float width = Settings.getScaledSlot();
        float height = K.UI.INVENTORY_ROWS * Settings.getScaledSlot()
                + (K.UI.INVENTORY_ROWS - 1) * spacing;
        UIScrollBar scrollBar = new UIScrollBar(x, y, width, height)
                .setOnValueChanged(ignored -> syncCreativeInventory());
        scrollBar.hide();
        return scrollBar;
    }

    /**
     * Returns the backpack width.
     * @return {@code float}; the backpack width
     */
    public static float getBackpackWidth() {
        return Settings.getScaledPadding() * 2.0f +
                BACKPACK_COLUMNS * Settings.getScaledSlot() +
                (BACKPACK_COLUMNS - 1) * Settings.getScaledSpacing();
    }

    /**
     * Returns the backpack height without an inventory-control header.
     * @return {@code float}; the backpack height
     */
    public static float getBackpackHeight() {
        return Settings.getScaledPadding() * 2.0f +
                BACKPACK_ROWS * Settings.getScaledSlot() +
                (BACKPACK_ROWS - 1) * Settings.getScaledSpacing();
    }

    /**
     * Returns the slot uis.
     * @return an array of {@link InventorySlotUI} values; the slot uis
     */
    public InventorySlotUI[] getSlotUIs() {
        return slotUIs;
    }

    /** Returns the armor slot views in ArmorSlot ordinal order. */
    public InventorySlotUI[] getArmorSlotUIs() {
        return armorSlotUIs.clone();
    }

    /**
     * Creates and returns the buttons.
     */
    public void createButtons() {
        float size = Settings.getScaledSlot();
        sortButton = new UIButton(Settings.getScaledPadding(),
                Settings.getScaledPadding() - Settings.getScaledSpacing(), size, size)
                .setCanDrawBackground(false);

        groupButton = new UIButton(Settings.getScaledPadding() + size + Settings.getScaledSpacing(),
                Settings.getScaledPadding() - Settings.getScaledSpacing(), size, size)
                .setCanDrawBackground(false);

        backpackButton = new UIButton(Settings.getScaledPadding() + size * 2 + Settings.getScaledSpacing(),
                Settings.getScaledPadding() - Settings.getScaledSpacing(), size, size)
                .setCanDrawBackground(false);

        sortButton.setOnClick(this::sortInventory);
        groupButton.setOnClick(this::groupInventory);
        backpackButton.setOnClick(() -> {
            if (GameMaster.game != null) GameMaster.game.setBackpackOpen(true);
        });

        sortButton.setTooltipText("inventory.sort");
        groupButton.setTooltipText("inventory.group");
        backpackButton.setTooltipText("inventory.backpack");
        backpackButton.hide();

        buttons.add(sortButton);
        buttons.add(groupButton);
        buttons.add(backpackButton);

        addChild(sortButton);
        addChild(groupButton);
        addChild(backpackButton);
        createInventoryModeButton();
        createCreativeFilterButtons();

    }

    /**
     * Creates the large backgroundless toggle attached to the inventory's
     * bottom-right corner. It is only exposed while the player is in godmode.
     */
    private void createInventoryModeButton() {
        float offset = 48.0f;
        float spacing = Settings.getScaledSpacing() + offset;
        float size = Settings.getScaledSlot() * 2.0f;
        inventoryModeButton = new UIButton(getWidth() - offset,
                getHeight() - spacing, size, size)
                .setCanDrawBackground(false)
                .setOnClick(this::toggleGodmodeInventory);
        inventoryModeButton.setZIndex(1);
        inventoryModeButton.hide();
        buttons.add(inventoryModeButton);
        addChild(inventoryModeButton);
    }

    /** Creates the category filters distributed across the creative header. */
    private void createCreativeFilterButtons() {
        float size = Settings.getScaledSlot();
        float spacing = Settings.getScaledSpacing();
        float startX = Settings.getScaledPadding();
        float y = Settings.getScaledPadding() - spacing;
        int index = 0;

        for (CreativeFilter filter : CreativeFilter.values()) {
            if (filter == CreativeFilter.ALL) continue;
            Item icon = getCreativeFilterIcon(filter);
            UIButton button = new UIButton(startX + index * (size + spacing),
                    y, size, size)
                    .setOnClick(() -> toggleCreativeFilter(filter));
            button.setSpriteSheet(ResourceManager.getItemSpriteSheet(icon));
            button.setSpriteColumn(ResourceManager.getItemFrame(icon));
            button.setTooltipText(filter.translationKey);
            button.setZIndex(1);
            button.hide();
            creativeFilterButtons.put(filter, button);
            buttons.add(button);
            addChild(button);
            index++;
        }
    }

    /** Selects one creative filter, or clears it when pressed a second time. */
    private void toggleCreativeFilter(CreativeFilter filter) {
        if (!isGodmode || !isCreativeInventoryVisible) return;
        creativeFilter = creativeFilter == filter ? CreativeFilter.ALL : filter;
        buildCreativeCatalog();
        syncCreativeInventory();
        updateCreativeFilterAppearance();
    }

    private Item getCreativeFilterIcon(CreativeFilter filter) {
        return switch (filter) {
            case FOOD -> new Food(FoodData.BREAD);
            case BLOCKS -> new Block(BlockData.GRASS);
            case TOOLS -> new Pickaxe(Tier.WOODEN);
            case MATERIALS -> new Material(MaterialID.STICK);
            case PRODUCE -> new Produce(CropType.CARROT);
            case SEEDS -> new Seed(CropType.WHEAT);
            case USABLES -> new Backpack();
            case ALL -> throw new IllegalArgumentException("ALL has no filter button");
        };
    }

    private void setCreativeFiltersVisible(boolean visible) {
        creativeFilterButtons.values().forEach(button -> {
            if (visible) button.show();
            else button.hide();
        });
    }

    private void updateCreativeFilterAppearance() {
        creativeFilterButtons.forEach((filter, button) -> {
            boolean selected = creativeFilter == filter;
            float color = selected ? 0.55f : 1.0f;
            button.setNormalColor(color, color, color, 1.0f);
        });
    }

    /**
     * Creates and returns the slots.
     */
    public void createSlots() {
        for (int i = 0; i < slotUIs.length; i++) {
            int column = i % K.UI.INVENTORY_COLUMNS;
            int row = i / K.UI.INVENTORY_COLUMNS;
            float x = Settings.getScaledPadding() + column * (Settings.getScaledSlot() + Settings.getScaledSpacing());
            float y = Settings.getScaledPadding() + Settings.getScaledHeader() + row * (Settings.getScaledSlot() + Settings.getScaledSpacing());
            InventorySlotUI slotUI = new InventorySlotUI(x, y, Settings.getScaledSlot(), Settings.getScaledSlot(),
                    SlotType.INVENTORY);

            slotUIs[i] = slotUI;
            addChild(slotUI);
        }
        createArmorSlots();
        createContainerSlots();
    }

    /** Creates the vertically aligned armor equipment slots in ArmorSlot order. */
    private void createArmorSlots() {
        float slotSize = Settings.getScaledSlot();
        float spacing = Settings.getScaledSpacing();
        float x = getArmorPanelX() + Settings.getScaledPadding();
        float y = getArmorPanelY() + Settings.getScaledPadding();
        for (ArmorSlot armorSlot : ArmorSlot.values()) {
            int index = armorSlot.ordinal();
            InventorySlotUI slotUI = new InventorySlotUI(x, y + index * (slotSize + spacing),
                    slotSize, slotSize,
                    SlotType.ARMOR);
            armorSlotUIs[index] = slotUI;
            addChild(slotUI);
        }
    }

    /** Returns the armor panel x-position immediately to the inventory's right. */
    private float getArmorPanelX() {
        return getWidth() + Settings.getScaledSpacing();
    }

    /** Centers the armor panel vertically against the complete inventory panel. */
    private float getArmorPanelY() {
        return (getHeight() - getArmorPanelHeight()) * 0.5f;
    }

    private float getArmorPanelWidth() {
        return Settings.getScaledSlot() + Settings.getScaledPadding() * 2.0f;
    }

    private float getArmorPanelHeight() {
        return Settings.getScaledPadding() * 2.0f
                + armorSlotUIs.length * Settings.getScaledSlot()
                + (armorSlotUIs.length - 1) * Settings.getScaledSpacing();
    }

    /** Creates the chest slots in a separate panel above the player inventory. */
    private void createContainerSlots() {
        float panelOffset = getContainerPanelHeight()
                + Settings.getScaledSpacing() * 2.0f;
        for (int i = 0; i < containerSlotUIs.length; i++) {
            int column = i % K.UI.INVENTORY_COLUMNS;
            int row = i / K.UI.INVENTORY_COLUMNS;
            float x = Settings.getScaledPadding()
                    + column * (Settings.getScaledSlot() + Settings.getScaledSpacing());
            float y = -panelOffset + Settings.getScaledPadding()
                    + row * (Settings.getScaledSlot() + Settings.getScaledSpacing());
            InventorySlotUI slotUI = new InventorySlotUI(x, y,
                    Settings.getScaledSlot(), Settings.getScaledSlot(), SlotType.INVENTORY);
            slotUI.hide();
            containerSlotUIs[i] = slotUI;
            addChild(slotUI);
        }
    }

    /** Returns the height of the headerless chest panel. */
    private float getContainerPanelHeight() {
        return getInventoryHeight() - Settings.getScaledHeader();
    }

    /**
     * Returns the buttons.
     * @return the {@link List} representing the buttons
     */
    public List<UIButton> getButtons() {
        return buttons;
    }

    /**
     * Reorganizes inventory state for sort inventory.
     */
    public void sortInventory() {
        if (player != null && inventory != null) {
            inventory.sort();
        }
    }

    /**
     * Creates or returns group inventory from the supplied arguments.
     */
    public void groupInventory() {
        if (player != null && inventory != null) {
            inventory.group();
        }
    }

    /**
     * Updates the position.
     * @param delta the {@code float} supplied as {@code delta}
     */
    private void updatePosition(float delta) {
        float currentX = getX();
        float currentY = getY();

        if (Math.abs(targetX - currentX) > 0.1f) {
            float newX = currentX + (targetX - currentX) * Math.min(1.0f, delta * 15.0f);
            setPosition(newX, getY());
        } else {
            setPosition(targetX, getY());
        }

        if (Math.abs(targetY - currentY) > 0.1f) {
            float newY = currentY + (targetY - currentY) * Math.min(1.0f, delta * 15.0f);
            setPosition(getX(), newY);
        } else {
            setPosition(getX(), targetY);
            if (isClosing) {
                super.hide();
                isClosing = false;
            }
        }

        if (backpackUI != null && backpackUI.isAttachedToInventory()) {
            float bpX = getX() - backpackUI.getWidth()
                    - Settings.getScaledSpacing() * 2.0f;
            float bpY = getY() + (getHeight() - backpackUI.getHeight()) / 2.0f;
            if (Math.abs(backpackTargetY - backpackCurrentY) > 0.1f) {
                backpackCurrentY += (backpackTargetY - backpackCurrentY) * Math.min(1.0f, delta * 15.0f);
            } else {
                backpackCurrentY = backpackTargetY;
                if (isBackpackClosing) {
                    backpackUI.hideAttached();
                    isBackpackOpen = false;
                    isBackpackClosing = false;
                }
            }
            backpackUI.setPosition(bpX, bpY);
        }
    }

    /**
     * Activates backpack and prepares any state it requires.
     * @param backpackUI the {@link BackpackInventoryUI} supplied as {@code backpackUI}
     */
    public void openBackpack(BackpackInventoryUI backpackUI) {
        if (backpackUI == null) return;
        this.backpackUI = backpackUI;
        this.isBackpackOpen = true;
        this.isBackpackClosing = false;
        this.backpackUI.show();

        float spacing = Settings.getScaledSpacing() * 2.0f;
        float pushOffset = (backpackUI.getHeight() + spacing) / 2.0f;
        this.targetY = this.defaultY + pushOffset;
        this.backpackCurrentY = -backpackUI.getHeight();
        this.backpackTargetY = this.targetY - backpackUI.getHeight() - spacing;

        this.backpackUI.setPosition(getX() + (getWidth() - backpackUI.getWidth()) / 2.0f, backpackCurrentY);
    }

    /**
     * Releases the resources associated with backpack.
     */
    public void closeBackpack() {
        if (!isBackpackOpen || isBackpackClosing) return;
        this.isBackpackClosing = true;
        this.targetY = this.defaultY;
        if (backpackUI != null) {
            this.backpackTargetY = this.defaultY - backpackUI.getHeight() - Settings.getScaledSpacing() * 2.0f;
        }
    }

    /**
     * {@inheritDoc}
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    @Override
    public void update(float delta) {
        super.update(delta);
        updatePosition(delta);
        updateQuickMoveAnimations(delta);

        if (player == null) return;
        boolean wasOpen = isVisible();
        boolean isOpen = GameMaster.game != null && (this instanceof BackpackInventoryUI
                ? GameMaster.game.isBackpackOpen() : GameMaster.game.isInventoryOpen());

        if (isOpen && !wasOpen) {
            show();
            onOpen();
        } else if (!isOpen && wasOpen) {
            onClose();
        }

        syncInventory();
        updateSlots();
        slotInteract();
    }

    /**
     * Handles open and updates the affected state.
     */
    private void onOpen() {
        isClosing = false;
        closeBackpack();

        if (GameMaster.game != null) {
            float spacing = Settings.getScaledSpacing() * 2.0f;
            float attachedBackpackWidth = backpackUI != null && backpackUI.isAttachedToInventory()
                    ? backpackUI.getWidth() + spacing : 0.0f;
            this.defaultX = (GameMaster.game.getWindowWidth()
                    - getWidth() - attachedBackpackWidth) / 2.0f + attachedBackpackWidth;
            this.targetX = defaultX;
            this.defaultY = (GameMaster.game.getWindowHeight() - getHeight()) / 2.0f;
            if (externalInventory != null) {
                this.defaultY += (getContainerPanelHeight()
                        + Settings.getScaledSpacing() * 2.0f) / 2.0f;
            }
            this.targetY = defaultY;
            setPosition(defaultX, GameMaster.game.getWindowHeight());
        }

        if (hotbarUI != null) {
            hotbarUI.setInventoryMode(true);
        }
    }

    /**
     * Handles close and updates the affected state.
     */
    private void onClose() {
        if (backpackUI != null) {
            backpackUI.hideAttached();
            isBackpackOpen = false;
            isBackpackClosing = false;
        }

        if (GameMaster.game != null) {
            this.targetY = GameMaster.game.getWindowHeight();
            this.isClosing = true;
        }

        if (hotbarUI != null) {
            hotbarUI.setInventoryMode(false);
        }
        returnCarriedItem();
        closeContainer();
    }

    /**
     * {@inheritDoc}
     * Deactivates this object and releases its transient state.
     * @return the {@link UIElement} representing the hide result
     */
    @Override
    public UIElement hide() {
        super.hide();
        this.isClosing = false;
        return this;
    }

    /**
     * Updates or derives runtime state for return carried item according to the supplied arguments.
     */
    private void returnCarriedItem() {
        if (carriedItem == null || carriedAmount <= 0 || player == null) {
            clearCarriedItem();
            return;
        }

        int remaining = inventory.add(carriedItem, carriedAmount);

        if (remaining <= 0) {
            clearCarriedItem();
        } else {
            carriedAmount = remaining;
        }
    }

    /**
     * Clears the carried item.
     */
    private void clearCarriedItem() {
        carriedItem = null;
        carriedAmount = 0;
    }

    /**
     * Refreshes dependent runtime state for sync inventory.
     */
    protected void syncInventory() {
        if (inventory == null) return;

        updateInventoryMode();
        if (isGodmode && isCreativeInventoryVisible) {
            syncArmorSlots(false);
            syncCreativeInventory();
            return;
        }

        for (int i = 0; i < slotUIs.length; i++) {
            InventorySlotUI slotUI = slotUIs[i];
            if (slotUI == null) continue;

            if (i < (inventory.getSlots().size())) {
                slotUI.setSlot((inventory.getSlot(i)));
            } else {
                slotUI.setSlot(null);
            }

            updateItemSprite(slotUI);
        }

        syncArmorSlots(true);

        syncContainerInventory();

        if (backpackUI != null && backpackUI.getSlotUIs() != null) {
            Inventory backpackInv = player.getBackpack();
            for (int i = 0; i < backpackUI.getSlotUIs().length; i++) {
                InventorySlotUI slotUI = backpackUI.getSlotUIs()[i];
                if (slotUI == null) continue;

                if (backpackInv != null && i < backpackInv.getSlots().size()) {
                    slotUI.setSlot(backpackInv.getSlot(i));
                } else {
                    slotUI.setSlot(null);
                }
                updateItemSprite(slotUI);
            }
        }

        if (sortButton.getSpriteSheet() == null && inventoryIcons != null) {
            sortButton.setSpriteSheet(inventoryIcons);
            sortButton.setSpriteColumn(0);

            groupButton.setSpriteSheet(inventoryIcons);
            groupButton.setSpriteColumn(1);

            backpackButton.setSpriteSheet(inventoryIcons);
            backpackButton.setSpriteColumn(2);
        }
    }

    /** Synchronizes the wearable slots with the player inventory equipment data. */
    private void syncArmorSlots(boolean visible) {
        for (ArmorSlot armorSlot : ArmorSlot.values()) {
            InventorySlotUI slotUI = armorSlotUIs[armorSlot.ordinal()];
            if (slotUI == null) continue;
            slotUI.setSlot(visible && inventory != null ? inventory.getArmorSlot(armorSlot) : null);
            updateItemSprite(slotUI);
            if (visible) slotUI.show();
            else slotUI.hide();
        }
    }

    /** Synchronizes and exposes the slots belonging to the open external inventory. */
    private void syncContainerInventory() {
        positionContainerSlots();
        Inventory containerInventory = externalInventory;
        for (int i = 0; i < containerSlotUIs.length; i++) {
            InventorySlotUI slotUI = containerSlotUIs[i];
            if (slotUI == null) continue;

            if (containerInventory != null && i < containerInventory.getSlots().size()) {
                slotUI.setSlot(containerInventory.getSlot(i));
                updateItemSprite(slotUI);
                slotUI.show();
            } else {
                slotUI.setSlot(null);
                updateItemSprite(slotUI);
                slotUI.hide();
            }
        }
    }

    /**
     * {@inheritDoc}
     * Scrolls the creative catalog by complete inventory rows while the pointer
     * is anywhere over this inventory.
     */
    @Override
    public boolean mouseScrolled(float mouseX, float mouseY,
                                 float scrollX, float scrollY) {
        if (isGodmode && isCreativeInventoryVisible
                && contains(mouseX, mouseY) && scrollY != 0.0f) {
            creativeScrollBar.scrollBy(scrollY > 0.0f ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /**
     * Switches the controls and contents when GODMODE changes.
     */
    private void updateInventoryMode() {
        boolean creative = !(this instanceof BackpackInventoryUI)
                && externalInventory == null
                && player != null && player.getGamemode().isGodmode();
        if (creative == isGodmode) return;

        isGodmode = creative;
        if (creative) {
            isCreativeInventoryVisible = true;
            hideBackpackForCreativeInventory();
            buildCreativeCatalog();
        } else {
            isCreativeInventoryVisible = false;
        }
        updateInventoryViewControls();
    }

    /**
     * Toggles between the creative catalog and the player's survival inventory
     * without changing the player's godmode setting.
     */
    private void toggleGodmodeInventory() {
        if (!isGodmode) return;
        if (isCreativeInventoryVisible) {
            isCreativeInventoryVisible = false;
        } else {
            isCreativeInventoryVisible = true;
            hideBackpackForCreativeInventory();
        }
        updateInventoryViewControls();
        syncInventory();
    }

    /**
     * Updates controls and the toggle's destination icon for the selected
     * godmode inventory view.
     */
    private void updateInventoryViewControls() {
        if (!isGodmode) {
            inventoryModeButton.hide();
            setCreativeFiltersVisible(false);
            creativeScrollBar.hide();
            sortButton.show();
            groupButton.show();
            return;
        }

        inventoryModeButton.show();
        Item destination = isCreativeInventoryVisible
                ? new Backpack()
                : new Block(BlockData.GRASS);
        inventoryModeButton.setSpriteSheet(
                ResourceManager.getItemSpriteSheet(destination));
        inventoryModeButton.setSpriteColumn(
                ResourceManager.getItemFrame(destination));

        if (isCreativeInventoryVisible) {
            setCreativeFiltersVisible(true);
            updateCreativeFilterAppearance();
            sortButton.hide();
            groupButton.hide();
            backpackButton.hide();
            creativeScrollBar.show();
        } else {
            setCreativeFiltersVisible(false);
            creativeScrollBar.hide();
            sortButton.show();
            groupButton.show();
        }
    }

    /**
     * Closes the auxiliary backpack panel before displaying creative items.
     */
    private void hideBackpackForCreativeInventory() {
        if (backpackUI == null) return;
        backpackUI.hideAttached();
        isBackpackOpen = false;
        isBackpackClosing = false;
        targetY = defaultY;
    }

    /**
     * Builds the creative catalog from registered items that have a supported
     * category, visible sprite and localized display name. The resulting order
     * is supplied globally by {@link Inventory#sorter()}.
     */
    private void buildCreativeCatalog() {
        creativeItems.clear();
        if (GameMaster.game == null) return;

        for (String id : GameMaster.game.getItemRegistry().getIds()) {
            Item item = GameMaster.game.getItemRegistry().create(id);
            if (isSupportedCreativeItem(item) && isInSelectedCreativeFilter(item)) {
                creativeItems.add(item);
            }
        }

        if (creativeFilter == CreativeFilter.ALL
                || creativeFilter == CreativeFilter.MATERIALS) {
            creativeItems.removeIf(MiningComponent.class::isInstance);
            Tier.forEach(tier -> {
                if (tier.isInvalidTier()) return;
                creativeItems.add(new MiningComponent(tier, MaterialID.RAW_ORE));
                creativeItems.add(new MiningComponent(tier, MaterialID.INGOT));
            });
        }

        creativeItems.sort(Inventory.sorter());
        int totalRows = Math.ceilDiv(creativeItems.size(), K.UI.INVENTORY_COLUMNS);
        creativeScrollBar.setMaximum(Math.max(0,
                totalRows - K.UI.INVENTORY_ROWS));
        creativeScrollBar.setValue(0);
    }

    /**
     * Checks whether an item belongs to a supported creative category and
     * has a drawable, translated inventory representation.
     * @param item the item to validate
     * @return {@code true} when the item may be shown in the creative catalog
     */
    private boolean isSupportedCreativeItem(Item item) {
        boolean supportedCategory = item instanceof Block
                || item instanceof Tool
                || item instanceof Armor
                || item instanceof Usable
                || item instanceof Material
                || item instanceof iBlock
                || item instanceof Food
                || item instanceof Produce
                || item instanceof Seed;
        if (!supportedCategory || ResourceManager.getItemSpriteSheet(item) == null
                || ResourceManager.getItemFrame(item) < 0) {
            return false;
        }
        String displayName = item.getDisplayName();
        return displayName != null && !displayName.isBlank()
                && !displayName.equals("block." + item.getName())
                && !displayName.startsWith("item.");
    }

    private boolean isInSelectedCreativeFilter(Item item) {
        return switch (creativeFilter) {
            case ALL -> true;
            case FOOD -> item instanceof Food;
            case BLOCKS -> item instanceof Block || item instanceof iBlock;
            case TOOLS -> item instanceof Tool || item instanceof Armor;
            case MATERIALS -> item instanceof Material;
            case PRODUCE -> item instanceof Produce;
            case SEEDS -> item instanceof Seed;
            case USABLES -> item instanceof Usable;
        };
    }

    /**
     * Displays the currently visible creative rows in the virtual slots.
     */
    private void syncCreativeInventory() {
        int firstItem = creativeScrollBar.getValue() * K.UI.INVENTORY_COLUMNS;
        for (int i = 0; i < slotUIs.length; i++) {
            InventorySlotUI slotUI = slotUIs[i];
            if (slotUI == null) continue;

            InventorySlot slot = creativeSlotData[i];
            if (slot == null) {
                slot = new InventorySlot();
                creativeSlotData[i] = slot;
                creativeSlots.add(slot);
            }

            int itemIndex = firstItem + i;
            if (itemIndex < creativeItems.size()) {
                Item item = creativeItems.get(itemIndex);
                slot.setItem(item);
                slot.setAmount(1);
            } else {
                slot.clear();
            }
            slotUI.setSlot(slot);
            updateItemSprite(slotUI);
        }
    }

    /**
     * Updates the item sprite.
     * @param slotUI the {@link InventorySlotUI} supplied as {@code slotUI}
     */
    protected void updateItemSprite(InventorySlotUI slotUI) {
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
    protected void updateSlots() {
        float mouseX = Mouse.getX();
        float mouseY = Mouse.getY();

        for (InventorySlotUI slotUI : slotUIs) {
            if (slotUI != null) {
                slotUI.setHovered(slotUI.contains(mouseX, mouseY));
            }
        }

        for (InventorySlotUI slotUI : armorSlotUIs) {
            if (slotUI != null && slotUI.isVisible()) {
                slotUI.setHovered(slotUI.contains(mouseX, mouseY));
            }
        }

        InventorySlotUI shieldSlotUI = hotbarUI == null ? null : hotbarUI.getShieldSlotUI();
        if (shieldSlotUI != null && shieldSlotUI.isVisible()) {
            shieldSlotUI.setHovered(shieldSlotUI.contains(mouseX, mouseY));
        }

        for (InventorySlotUI slotUI : containerSlotUIs) {
            if (slotUI != null && slotUI.isVisible()) {
                slotUI.setHovered(slotUI.contains(mouseX, mouseY));
            }
        }

        if (backpackUI != null && backpackUI.isVisible()) {
            for (InventorySlotUI slotUI : backpackUI.getSlotUIs()) {
                if (slotUI != null) {
                    slotUI.setHovered(slotUI.contains(mouseX, mouseY));
                }
            }
        }

        if (hotbarUI != null) {
            for (InventorySlotUI slotUI : hotbarUI.getSlotUIs()) {
                if (slotUI != null) {
                    slotUI.setHovered(slotUI.contains(mouseX, mouseY));
                }
            }
        }
    }

    /**
     * Handles slot interact and applies its effect to the current interaction state.
     */
    public void slotInteract() {
        if (hotbarUI == null) return;
        if (!GameMaster.game.isInventoryOpen()
                && !(this instanceof BackpackInventoryUI && GameMaster.game.isBackpackOpen())) return;

        InventorySlotUI[] hotbarSlots = hotbarUI.getSlotUIs();
        InventorySlotUI[] backpackSlots = (backpackUI != null && backpackUI.isVisible()) ?
                backpackUI.getSlotUIs() : new InventorySlotUI[0];

        int containerSlotCount = externalInventory == null ? 0 : containerSlotUIs.length;
        InventorySlotUI shieldSlotUI = hotbarUI.getShieldSlotUI();
        int shieldSlotCount = shieldSlotUI != null && shieldSlotUI.isVisible() ? 1 : 0;
        InventorySlotUI[] allSlots = new InventorySlotUI[slotUIs.length + armorSlotUIs.length
                + containerSlotCount + hotbarSlots.length + backpackSlots.length
                + shieldSlotCount];
        System.arraycopy(slotUIs, 0, allSlots, 0, slotUIs.length);
        System.arraycopy(armorSlotUIs, 0, allSlots, slotUIs.length, armorSlotUIs.length);
        int armorOffset = slotUIs.length + armorSlotUIs.length;
        if (containerSlotCount > 0) {
            System.arraycopy(containerSlotUIs, 0, allSlots,
                    armorOffset, containerSlotCount);
        }
        int hotbarOffset = armorOffset + containerSlotCount;
        System.arraycopy(hotbarSlots, 0, allSlots, hotbarOffset, hotbarSlots.length);
        if (backpackSlots.length > 0) {
            System.arraycopy(backpackSlots, 0, allSlots,
                    hotbarOffset + hotbarSlots.length, backpackSlots.length);
        }
        if (shieldSlotCount > 0) {
            allSlots[allSlots.length - 1] = shieldSlotUI;
        }

        for (InventorySlotUI slotUI : allSlots) {
            if (slotUI == null || !slotUI.isHovered()) continue;
            InventorySlot slot = slotUI.getSlotType();
            if (slot == null) continue;

            if (Controls.isPressed(ControlAction.UI_SELECT)) {
                if (isGodmode && isCreativeInventoryVisible
                        && creativeSlots.contains(slot)) {
                    takeCreativeItem(slot);
                    break;
                }
                if (Controls.isDown(ControlAction.MODIFIER)
                        && carriedItem == null) {
                    quickMove(slotUI);
                } else {
                    leftClick(slot);
                }
                break;
            }

            if (Controls.isPressed(ControlAction.UI_CONTEXT)) {
                if (isGodmode && isCreativeInventoryVisible
                        && creativeSlots.contains(slot)) {
                    break;
                }
                rightClick(slot);
                break;
            }
        }
    }

    /**
     * Moves a complete stack directly between the storage areas currently
     * exposed by the inventory screen.
     *
     * @param sourceUI slot selected with control-left-click
     */
    private void quickMove(InventorySlotUI sourceUI) {
        InventorySlot source = sourceUI.getSlotType();
        if (source.isEmpty() || player == null) return;

        NPC trader = getExternalTrader();
        if (trader != null) {
            if (ownsSlot(externalInventory, source)) {
                buyFromTrader(trader, source.getItem(), source.getAmount(), source);
            } else if (isPlayerInventorySlot(source)) {
                sellToTrader(trader, source.getItem(), source.getAmount(), source);
            }
            return;
        }

        Inventory playerInventory = player.getInventory();
        Inventory backpackInventory = player.getBackpack();
        Inventory containerInventory = externalInventory;
        Inventory destination;
        List<SlotRange> destinationRanges = new ArrayList<>();

        if (ownsSlot(containerInventory, source)
                || ownsSlot(backpackInventory, source)) {
            destination = playerInventory;
            destinationRanges.add(new SlotRange(0, playerInventory.getHotbarStart()));
            destinationRanges.add(new SlotRange(
                    playerInventory.getHotbarStart(), playerInventory.getSlots().size()));
        } else {
            int playerSlot = playerInventory.getSlots().indexOf(source);
            if (playerSlot < 0) return;

            if (isStandaloneBackpackOpen()) {
                destination = backpackInventory;
                destinationRanges.add(new SlotRange(0, destination.getSlots().size()));
            } else if (containerInventory != null) {
                destination = containerInventory;
                destinationRanges.add(new SlotRange(0, destination.getSlots().size()));
            } else if (playerSlot >= playerInventory.getHotbarStart()) {
                destination = playerInventory;
                destinationRanges.add(new SlotRange(0, playerInventory.getHotbarStart()));
            } else if (backpackUI != null && backpackUI.isVisible()) {
                destination = backpackInventory;
                destinationRanges.add(new SlotRange(0, destination.getSlots().size()));
            } else {
                destination = playerInventory;
                destinationRanges.add(new SlotRange(
                        playerInventory.getHotbarStart(), playerInventory.getSlots().size()));
            }
        }

        Item item = source.getItem();
        int originalAmount = source.getAmount();
        InventorySlot animationTarget = findAvailableSlot(
                destination, item, destinationRanges);
        if (animationTarget == null) return;

        int remaining = originalAmount;
        for (SlotRange range : destinationRanges) {
            if (remaining <= 0) break;
            remaining = destination.addToRange(
                    item, remaining, range.startInclusive(), range.endExclusive());
        }
        if (remaining == originalAmount) return;

        source.setAmount(remaining);
        InventorySlotUI destinationUI = findSlotUI(destination, animationTarget);
        if (destinationUI != null) {
            quickMoveAnimations.add(new QuickMoveAnimation(
                    item, originalAmount - remaining,
                    centerX(sourceUI), centerY(sourceUI),
                    centerX(destinationUI), centerY(destinationUI)));
        }
    }

    /** Finds the first stack or empty slot that can receive an item. */
    private InventorySlot findAvailableSlot(Inventory destination, Item item,
                                             List<SlotRange> ranges) {
        int maxStack = destination.getMaxStack(item);
        for (SlotRange range : ranges) {
            for (int index = range.startInclusive(); index < range.endExclusive(); index++) {
                InventorySlot slot = destination.getSlot(index);
                if (!slot.isEmpty() && isSameType(slot.getItem(), item)
                        && slot.getAmount() < maxStack) {
                    return slot;
                }
            }
            for (int index = range.startInclusive(); index < range.endExclusive(); index++) {
                InventorySlot slot = destination.getSlot(index);
                if (slot.isEmpty()) return slot;
            }
        }
        return null;
    }

    /** Resolves a data slot back to the UI element that displays it. */
    private InventorySlotUI findSlotUI(Inventory owner, InventorySlot slot) {
        int index = owner.getSlots().indexOf(slot);
        if (index < 0) return null;

        Inventory playerInventory = player.getInventory();
        if (owner == playerInventory) {
            if (index < playerInventory.getHotbarStart()) {
                return index < slotUIs.length ? slotUIs[index] : null;
            }
            int hotbarIndex = index - playerInventory.getHotbarStart();
            InventorySlotUI[] hotbarSlots = hotbarUI.getSlotUIs();
            return hotbarIndex < hotbarSlots.length ? hotbarSlots[hotbarIndex] : null;
        }
        if (owner == player.getBackpack()) {
            InventorySlotUI[] backpackSlots = backpackUI.getSlotUIs();
            return index < backpackSlots.length ? backpackSlots[index] : null;
        }
        if (externalInventory != null && owner == externalInventory) {
            return index < containerSlotUIs.length ? containerSlotUIs[index] : null;
        }
        return null;
    }

    /** Advances every quick-move icon towards its destination. */
    protected void updateQuickMoveAnimations(float delta) {
        quickMoveAnimations.removeIf(animation -> animation.update(delta));
    }

    private static float centerX(InventorySlotUI slot) {
        return slot.getAbsoluteX() + slot.getAbsoluteWidth() / 2.0f;
    }

    private static float centerY(InventorySlotUI slot) {
        return slot.getAbsoluteY() + slot.getAbsoluteHeight() / 2.0f;
    }

    /** Returns whether an inventory owns the supplied slot instance. */
    private boolean ownsSlot(Inventory owner, InventorySlot slot) {
        return owner != null && owner.getSlots().contains(slot);
    }

    /**
     * Copies an item from an infinite creative slot to the cursor.
     * @param slot the {@link InventorySlot} argument; the creative source slot
     */
    private void takeCreativeItem(InventorySlot slot) {
        if (slot.isEmpty()) return;
        carriedItem = slot.getItem().copy();
        carriedAmount += Math.max(1, inventory.getMaxStack(slot.getItem()));
    }

    /**
     * Handles left click and applies its effect to the current interaction state.
     * @param slot the {@link InventorySlot} supplied as {@code slot}
     */
    private void leftClick(InventorySlot slot) {
        NPC trader = getExternalTrader();
        if (trader != null && ownsSlot(externalInventory, slot) && carriedItem != null) {
            return;
        }

        if (isShieldSlot(slot)) {
            handleShieldSlotClick(slot);
            return;
        }

        if (isArmorSlot(slot)) {
            handleArmorSlotClick(slot);
            return;
        }

        if (carriedItem == null) {
            pickEntireStack(slot);
            return;
        }

        if (slot.isEmpty()) {
            placeEntireStack(slot);
            return;
        }

        if (isSameType(carriedItem, slot.getItem())) {
            mergeCarriedStack(slot);
        } else {
            swapStacks(slot);
        }
    }

    /**
     * Transfers or creates the relevant entity or item for pick entire stack.
     * @param slot the {@link InventorySlot} supplied as {@code slot}
     */
    private void pickEntireStack(InventorySlot slot) {
        if (slot.isEmpty()) {
            return;
        }

        NPC trader = getExternalTrader();
        if (trader != null) {
            if (ownsSlot(externalInventory, slot)) {
                buyFromTrader(trader, slot.getItem(), slot.getAmount(), slot);
            } else if (isPlayerInventorySlot(slot)) {
                sellToTrader(trader, slot.getItem(), slot.getAmount(), slot);
            }
            return;
        }

        carriedItem = slot.getItem();
        carriedAmount = slot.getAmount();
        slot.clear();
    }

    /**
     * Applies the world or inventory action represented by place entire stack.
     * @param slot the {@link InventorySlot} supplied as {@code slot}
     */
    private void placeEntireStack(InventorySlot slot) {
        slot.setItem(carriedItem);
        slot.setAmount(carriedAmount);
        clearCarriedItem();
    }

    /**
     * Reorganizes inventory state for merge carried stack.
     * @param slot the {@link InventorySlot} supplied as {@code slot}
     */
    private void mergeCarriedStack(InventorySlot slot) {
        int maxStack = K.World.MAX_STACK;
        int space = maxStack - slot.getAmount();

        if (space <= 0) {
            return;
        }

        int moved = Math.min(space, carriedAmount);
        slot.addAmount(moved);
        carriedAmount -= moved;

        if (carriedAmount <= 0) {
            clearCarriedItem();
        }
    }

    /**
     * Reorganizes inventory state for swap stacks.
     * @param slot the {@link InventorySlot} supplied as {@code slot}
     */
    private void swapStacks(InventorySlot slot) {
        Item tempItem = slot.getItem();
        int tempAmount = slot.getAmount();

        slot.setItem(carriedItem);
        slot.setAmount(carriedAmount);

        carriedItem = tempItem;
        carriedAmount = tempAmount;
    }

    /**
     * Handles right click and applies its effect to the current interaction state.
     * @param slot the {@link InventorySlot} supplied as {@code slot}
     */
    private void rightClick(InventorySlot slot) {
        NPC trader = getExternalTrader();
        if (trader != null && ownsSlot(externalInventory, slot) && carriedItem != null) {
            return;
        }

        if (isShieldSlot(slot)) {
            handleShieldSlotClick(slot);
            return;
        }

        if (isArmorSlot(slot)) {
            handleArmorSlotClick(slot);
            return;
        }

        if (carriedItem == null && player != null
                && ownsSlot(player.getBackpack(), slot)
                && (slot.getItem() instanceof CraftingBook
                || slot.getItem() instanceof Wallet)) {
            ((Usable) slot.getItem()).use(GameMaster.game,
                    Controls.isDown(ControlAction.MODIFIER));
            return;
        }

        if (carriedItem == null) {
            takeHalf(slot);
            return;
        }

        if (slot.isEmpty()) {
            placeOne(slot);
            return;
        }

        if (!isSameType(carriedItem, slot.getItem())) {
            return;
        }

        addOneToSlot(slot);
    }

    /** Moves exactly one shield into or out of the dedicated equipment slot. */
    private void handleShieldSlotClick(InventorySlot slot) {
        if (carriedItem == null) {
            pickEntireStack(slot);
            return;
        }
        if (!(carriedItem instanceof Shield) || !slot.isEmpty()) return;
        slot.setItem(carriedItem);
        slot.setAmount(1);
        carriedAmount--;
        if (carriedAmount <= 0) clearCarriedItem();
    }

    private boolean isShieldSlot(InventorySlot slot) {
        return inventory != null && slot == inventory.getShieldSlot();
    }

    /** Moves only the matching armor category into its dedicated single-item slot. */
    private void handleArmorSlotClick(InventorySlot slot) {
        if (carriedItem == null) {
            pickEntireStack(slot);
            return;
        }

        ArmorSlot armorSlot = getArmorSlotType(slot);
        if (!(carriedItem instanceof Armor armor) || armorSlot == null
                || armor.getType() != armorSlot.getEquippable() || !slot.isEmpty()) {
            return;
        }

        slot.setItem(carriedItem);
        slot.setAmount(1);
        carriedAmount--;
        if (carriedAmount <= 0) clearCarriedItem();
    }

    private boolean isArmorSlot(InventorySlot slot) {
        return getArmorSlotType(slot) != null;
    }

    private ArmorSlot getArmorSlotType(InventorySlot slot) {
        if (inventory == null || slot == null) return null;
        for (ArmorSlot armorSlot : ArmorSlot.values()) {
            if (inventory.getArmorSlot(armorSlot) == slot) return armorSlot;
        }
        return null;
    }

    /**
     * Transfers or creates the relevant entity or item for take half.
     * @param slot the {@link InventorySlot} supplied as {@code slot}
     */
    private void takeHalf(InventorySlot slot) {
        if (slot.isEmpty()) {
            return;
        }

        int splitAmount = (int) Math.ceil(slot.getAmount() / 2.0);

        NPC trader = getExternalTrader();
        if (trader != null) {
            if (ownsSlot(externalInventory, slot)) {
                buyFromTrader(trader, slot.getItem(), splitAmount, slot);
            } else if (isPlayerInventorySlot(slot)) {
                sellToTrader(trader, slot.getItem(), splitAmount, slot);
            }
            return;
        }

        carriedItem = slot.getItem();
        carriedAmount = splitAmount;

        slot.setAmount(slot.getAmount() - splitAmount);
    }

    /**
     * Applies the world or inventory action represented by place one.
     * @param slot the {@link InventorySlot} supplied as {@code slot}
     */
    private void placeOne(InventorySlot slot) {
        int maxStack = K.World.MAX_STACK;
        if (maxStack <= 0) {
            return;
        }

        slot.setItem(carriedItem);
        slot.setAmount(1);
        carriedAmount--;
        if (carriedAmount <= 0) {
            clearCarriedItem();
        }
    }

    /**
     * Adds the one to slot.
     * @param slot the {@link InventorySlot} supplied as {@code slot}
     */
    private void addOneToSlot(InventorySlot slot) {
        int maxStack = K.World.MAX_STACK;

        if (slot.getAmount() >= maxStack) {
            return;
        }

        slot.addAmount(1);
        carriedAmount--;

        if (carriedAmount <= 0) {
            clearCarriedItem();
        }
    }

    /** Returns the trader whose stock is currently open, if any. */
    private NPC getExternalTrader() {
        if (externalInventory == null) return null;
        if (externalInventory.getOwner() instanceof NPC npc
                && npc.getJob() == Job.TRADER) {
            return npc;
        }
        return null;
    }

    /** Returns whether this UI is the standalone backpack panel. */
    private boolean isStandaloneBackpackOpen() {
        return this instanceof BackpackInventoryUI && GameMaster.game != null
                && GameMaster.game.isBackpackOpen();
    }

    /** Returns whether a slot belongs to either of the player's visible inventories. */
    private boolean isPlayerInventorySlot(InventorySlot slot) {
        if (player == null || slot == null) return false;
        return ownsSlot(player.getInventory(), slot) || ownsSlot(player.getBackpack(), slot)
                || getArmorSlotType(slot) != null;
    }

    /** Buys a complete or partial stack from a trader into the player's inventory. */
    private void buyFromTrader(NPC trader, Item item, int amount, InventorySlot sourceSlot) {
        if (trader == null || player == null || item == null || amount <= 0) return;
        if (trader.hasWallet() == null || player.hasWallet() == null) return;

        int totalPrice = item.getValue() * amount;
        if (trader.getStock().getAmount(item) < amount
                || player.hasWallet().coins() < totalPrice
                || !canFit(item, amount)) {
            return;
        }

        Inventory destination = canFit(player.getInventory(), item, amount)
                ? player.getInventory() : player.getBackpack();
        InventorySlot targetSlot = findAvailableSlot(destination, item,
                List.of(new SlotRange(0, destination.getSlots().size())));
        InventorySlotUI sourceUI = sourceSlot == null ? null
                : findSlotUI(trader.getStock(), sourceSlot);
        if (!trader.sell(item, amount)) return;
        player.spend(totalPrice);
        addTo(item, amount);
        InventorySlotUI targetUI = targetSlot == null ? null
                : findSlotUI(destination, targetSlot);
        if (sourceUI != null && targetUI != null) {
            quickMoveAnimations.add(new QuickMoveAnimation(item, amount,
                    centerX(sourceUI), centerY(sourceUI), centerX(targetUI), centerY(targetUI)));
        }
    }

    /** Sells a stack from a player's inventory to a trader. */
    private void sellToTrader(NPC trader, Item item, int amount, InventorySlot sourceSlot) {
        if (trader == null || player == null || sourceSlot == null
                || item == null || amount <= 0) return;
        if (trader.hasWallet() == null || player.hasWallet() == null) return;
        if (sourceSlot.isEmpty() || !isSameType(sourceSlot.getItem(), item)
                || sourceSlot.getAmount() < amount) return;

        InventorySlot targetSlot = findAvailableSlot(trader.getStock(), item,
                List.of(new SlotRange(0, trader.getStock().getSlots().size())));
        Inventory sourceInventory = ownsSlot(player.getBackpack(), sourceSlot)
                ? player.getBackpack() : player.getInventory();
        InventorySlotUI sourceUI = findSlotUI(sourceInventory, sourceSlot);

        int amountToSell = canAfford(trader, item, amount);
        while (amountToSell > 0 && !canFit(trader.getStock(), item, amountToSell)) {
            amountToSell--;
        }
        if (amountToSell <= 0 || !trader.buy(item, amountToSell)) return;

        sourceSlot.setAmount(sourceSlot.getAmount() - amountToSell);
        player.earn(item.getValue() * amountToSell);
        InventorySlotUI targetUI = targetSlot == null ? null
                : findSlotUI(trader.getStock(), targetSlot);
        if (sourceUI != null && targetUI != null) {
            quickMoveAnimations.add(new QuickMoveAnimation(item, amountToSell,
                    centerX(sourceUI), centerY(sourceUI), centerX(targetUI), centerY(targetUI)));
        }
    }

    /** Returns the largest quantity the trader can pay for right now. */
    private int canAfford(NPC trader, Item item, int requested) {
        if (item.getValue() <= 0) return requested;
        if (trader.hasMoney()) {
            return Math.min(requested, trader.hasWallet().coins() / item.getValue());
        }
        return 0;
    }

    /** Checks whether an inventory can receive the requested amount without loss. */
    private boolean canFit(Inventory target, Item item, int amount) {
        if (target == null || item == null || amount <= 0) return false;

        int remaining = amount;
        int maxStack = target.getMaxStack(item);
        for (InventorySlot slot : target.getSlots()) {
            if (!slot.isEmpty() && isSameType(slot.getItem(), item)) {
                remaining -= Math.max(0, maxStack - slot.getAmount());
                if (remaining <= 0) return true;
            }
        }
        for (InventorySlot slot : target.getSlots()) {
            if (slot.isEmpty()) {
                remaining -= maxStack;
                if (remaining <= 0) return true;
            }
        }
        return false;
    }

    /** Checks both player storage areas and includes the equipped backpack when present. */
    private boolean canFit(Item item, int amount) {
        if (player == null) return false;
        int remaining = amount;
        Inventory playerInventory = player.getInventory();
        if (canFit(playerInventory, item, remaining)) return true;

        remaining -= availableSpace(playerInventory, item);
        Inventory backpack = player.getBackpack();
        return backpack != null && playerInventory.hasBackpackEquipped()
                && canFit(backpack, item, Math.max(1, remaining));
    }

    /** Returns the number of items that can be inserted into the inventory. */
    private int availableSpace(Inventory target, Item item) {
        if (target == null || item == null) return 0;
        int maxStack = target.getMaxStack(item);
        int space = 0;
        for (InventorySlot slot : target.getSlots()) {
            space += slot.isEmpty() ? maxStack
                    : isSameType(slot.getItem(), item) ? Math.max(0, maxStack - slot.getAmount()) : 0;
        }
        return space;
    }

    /** Inserts an item into player storage after capacity has been checked. */
    private void addTo(Item item, int amount) {
        if (player == null) return;
        int remaining = player.getInventory().add(item, amount);
        if (remaining > 0 && player.getInventory().hasBackpackEquipped()
                && player.getBackpack() != null) {
            player.getBackpack().add(item, remaining);
        }
    }

    /**
     * Checks whether the same type condition is met.
     * @param a the {@link Item} supplied as {@code a}
     * @param b the {@link Item} supplied as {@code b}
     * @return {@code true} if same type; otherwise {@code false}
     */
    private boolean isSameType(Item a, Item b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.getClass() != b.getClass()) {
            return false;
        }

        return switch (a) {
            case Produce p1 when b instanceof Produce p2 -> p1.getType() == p2.getType();
            case Seed s1 when b instanceof Seed s2 -> s1.getType() == s2.getType();
            case Crop c1 when b instanceof Crop c2 -> c1.getCropType() == c2.getCropType();
            case Block b1 when b instanceof Block b2 -> b1.getType() == b2.getType();
            case Tool t1 when b instanceof Tool t2 -> t1.getId() == t2.getId() && t1.getType() == t2.getType();
            default -> a.getName().equals(b.getName());
        };
    }

    /**
     * {@inheritDoc}
     * Renders this object in the requested render pass.
     */
    @Override
    public void render() {
        renderContainerBackground();
        renderBackground();
        renderArmorBackground();
        renderChildren();
        renderQuickMoveAnimations();
        renderCarriedItem();

        if (externalInventory == null && (!isGodmode || !isCreativeInventoryVisible)
                && inventory != null && inventory.getBackpackSlot() != null
                && inventory.getBackpackSlot().getItem() != null) {
            backpackButton.show();
        } else {
            backpackButton.hide();
        }
    }

    /**
     * Renders the scalable inventory background. Creative mode omits the unused
     * header strip that normally contains the inventory controls.
     */
    protected void renderBackground() {
        float width = getAbsoluteWidth();
        float height = getAbsoluteHeight();
        float y = getAbsoluteY();
        if (isGodmode && isCreativeInventoryVisible) {
            float headerHeight = Settings.getScaledHeader();
            y += headerHeight;
            height -= headerHeight;
        }
        int textureWidth = Math.max(GUI_SLICE_SIZE * 2,
                Math.round(width / Settings.getScale()));
        int textureHeight = Math.max(GUI_SLICE_SIZE * 2,
                Math.round(height / Settings.getScale()));
        Texture background = Frontend.createNineSliceTexture(
                ResourceManager.rem.getBackgroundUI(), textureWidth,
                textureHeight, GUI_SLICE_SIZE);
        Frontend.drawTexture(background, getAbsoluteX(), y, width,
                height, new Vector4f(1.0f, 1.0f, 1.0f, getWorldOpacity()));
    }

    /** Draws the compact panel which frames the vertically centered armor slots. */
    private void renderArmorBackground() {
        if (armorSlotUIs.length == 0 || armorSlotUIs[0] == null
                || !armorSlotUIs[0].isVisible()) return;

        float width = getArmorPanelWidth();
        float height = getArmorPanelHeight();
        int textureWidth = Math.max(GUI_SLICE_SIZE * 2,
                Math.round(width / Settings.getScale()));
        int textureHeight = Math.max(GUI_SLICE_SIZE * 2,
                Math.round(height / Settings.getScale()));
        Texture background = Frontend.createNineSliceTexture(
                ResourceManager.rem.getBackgroundUI(), textureWidth,
                textureHeight, GUI_SLICE_SIZE);
        Frontend.drawTexture(background, getAbsoluteX() + getArmorPanelX(),
                getAbsoluteY() + getArmorPanelY(), width, height,
                new Vector4f(1.0f, 1.0f, 1.0f, getWorldOpacity()));
    }

    /** Positions external slots independently when the open inventory belongs to a trader. */
    private void positionContainerSlots() {
        if (externalInventory == null) return;
        float panelX = getContainerPanelX() - getAbsoluteX();
        float panelOffset = getContainerPanelHeight()
                + Settings.getScaledSpacing() * 2.0f;
        for (int i = 0; i < containerSlotUIs.length; i++) {
            InventorySlotUI slotUI = containerSlotUIs[i];
            if (slotUI == null) continue;
            int column = i % K.UI.INVENTORY_COLUMNS;
            int row = i / K.UI.INVENTORY_COLUMNS;
            slotUI.setPosition(panelX + Settings.getScaledPadding()
                            + column * (Settings.getScaledSlot() + Settings.getScaledSpacing()),
                    -panelOffset + Settings.getScaledPadding()
                            + row * (Settings.getScaledSlot() + Settings.getScaledSpacing()));
        }
    }

    /** Returns the external panel's screen-space X independently of the player inventory. */
    private float getContainerPanelX() {
        if (getExternalTrader() != null && GameMaster.game != null) {
            return (GameMaster.game.getWindowWidth() - getWidth()) / 2.0f;
        }
        return getAbsoluteX();
    }

    /** Draws the headerless external-storage panel above the player inventory. */
    private void renderContainerBackground() {
        if (externalInventory == null) return;

        float width = getAbsoluteWidth();
        float height = getContainerPanelHeight();
        float spacing = Settings.getScaledSpacing() * 2.0f;
        float y = getAbsoluteY() - height - spacing;
        int textureWidth = Math.max(GUI_SLICE_SIZE * 2,
                Math.round(width / Settings.getScale()));
        int textureHeight = Math.max(GUI_SLICE_SIZE * 2,
                Math.round(height / Settings.getScale()));
        Texture background = Frontend.createNineSliceTexture(
                ResourceManager.rem.getBackgroundUI(), textureWidth,
                textureHeight, GUI_SLICE_SIZE);
        Frontend.drawTexture(background, getContainerPanelX(), y, width,
                height, new Vector4f(1.0f, 1.0f, 1.0f, getWorldOpacity()));
    }

    /**
     * Renders the carried item.
     */
    protected void renderCarriedItem() {
        if (carriedItem == null || carriedAmount <= 0) {
            return;
        }

        SpriteSheet sheet = ResourceManager.getItemSpriteSheet(carriedItem);
        if (sheet == null) {
            return;
        }

        float iconSize = Settings.getScaledIcon();
        float x = Mouse.getX() - iconSize / 2f;
        float y = Mouse.getY() - iconSize / 2f;
        int frame = ResourceManager.getItemFrame(carriedItem);

        Frontend.drawSprite(sheet, frame, x, y,
                iconSize, iconSize, K.UI.UI_ITEM_TINT);

        if (carriedAmount > 1) {
            String amount = String.valueOf(carriedAmount);

            Frontend.drawString(amount, x + iconSize - 10, y + iconSize - 10,
                    Frontend.getNormalFont(), K.UI.UI_TEXT_COLOR);
        }
    }

    /** Renders the transient icons created by control-left-click transfers. */
    protected void renderQuickMoveAnimations() {
        float iconSize = Settings.getScaledIcon();
        for (QuickMoveAnimation animation : quickMoveAnimations) {
            SpriteSheet sheet = ResourceManager.getItemSpriteSheet(animation.item);
            if (sheet == null) continue;

            float x = animation.currentX - iconSize / 2.0f;
            float y = animation.currentY - iconSize / 2.0f;
            Frontend.drawSprite(sheet, ResourceManager.getItemFrame(animation.item),
                    x, y, iconSize, iconSize, K.UI.UI_ITEM_TINT);
        }
    }

    /** Exclusive slot range used by quick-move destination selection. */
    private record SlotRange(int startInclusive, int endExclusive) {}

    /** Screen-space interpolation state for one completed quick move. */
    private static final class QuickMoveAnimation {
        private final Item item;
        private final int amount;
        private final float startX;
        private final float startY;
        private final float targetX;
        private final float targetY;
        private float currentX;
        private float currentY;
        private float elapsed;

        /**
         * Creates a new quick move animation.
         * @param item the {@link Item} argument; the item to be moved
         * @param amount the {@code int} argument; the amount of the item to be moved
         * @param startX the {@code float} argument; the starting X position
         * @param startY the {@code float} argument; the starting Y position
         * @param targetX the {@code float} argument; the target X position
         * @param targetY the {@code float} argument; the target Y position
         */
        private QuickMoveAnimation(Item item, int amount, float startX, float startY,
                                   float targetX, float targetY) {
            this.item = item;
            this.amount = amount;
            this.startX = startX;
            this.startY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.currentX = startX;
            this.currentY = startY;
        }

        /**
         * Advances the interpolation.
         * @return whether the animation reached its destination
         */
        private boolean update(float delta) {
            elapsed += Math.max(delta, 0.0f);
            float progress = Math.min(
                    elapsed / QUICK_MOVE_ANIMATION_DURATION_SECONDS,
                    ANIMATION_COMPLETE);
            currentX = lerp(startX, targetX, progress);
            currentY = lerp(startY, targetY, progress);
            return progress >= ANIMATION_COMPLETE;
        }
    }

    /**
     * Sets the icons.
     * @param seed the {@link SpriteSheet} supplied as {@code seed}
     * @param crop the {@link SpriteSheet} supplied as {@code crop}
     * @param block the {@link SpriteSheet} supplied as {@code block}
     * @param tool the {@link SpriteSheet} supplied as {@code tool}
     * @param material the {@link SpriteSheet} supplied as {@code material}
     * @param inv the {@link SpriteSheet} supplied as {@code inv}
     */
    public void setIcons(SpriteSheet seed, SpriteSheet crop,
                         SpriteSheet block, SpriteSheet tool,
                         SpriteSheet material, SpriteSheet armor,
                         SpriteSheet inv) {
        this.seedIcons = seed;
        this.cropIcons = crop;
        this.blockIcons = block;
        this.toolIcons = tool;
        this.materialIcons = material;
        this.armorIcons = armor;
        this.inventoryIcons = inv;

        if (hotbarUI != null) {
            hotbarUI.setSeedIcons(seedIcons);
            hotbarUI.setCropIcons(cropIcons);
            hotbarUI.setBlockIcons(blockIcons);
            hotbarUI.setToolIcons(toolIcons);
            hotbarUI.setMaterialIcons(materialIcons);
            hotbarUI.setArmorIcons(armorIcons);
            hotbarUI.setInventoryIcons(inventoryIcons);
        }
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
     * Opens the inventory owned by an interactive block.
     * @param block the {@link iBlock} argument; the block whose inventory will be displayed
     */
    public void openContainer(iBlock block) {
        if (block == null || GameMaster.game == null) return;
        this.containerBlock = block;
        this.externalInventory = block.getInventory();
        this.inventory = player == null ? null : player.getInventory();
        GameMaster.game.setInventoryOpen(true);
        SoundService.fx.playUseSound(block.getType().getSoundGroup(), 0);
    }

    /**
     * Opens a non-block inventory, such as the stock carried by a trader NPC,
     * beside the player's inventory.
     *
     * @param externalInventory the inventory to expose in the storage panel
     */
    public void openExternalInventory(Inventory externalInventory) {
        if (externalInventory == null || GameMaster.game == null) return;
        this.containerBlock = null;
        this.externalInventory = externalInventory;
        this.inventory = player == null ? null : player.getInventory();
        if (getExternalTrader() != null && this.inventory != null
                && this.inventory.hasBackpackEquipped() && backpackUI != null) {
            float x = getX() - backpackUI.getWidth() - Settings.getScaledSpacing() * 2.0f;
            float y = getY() + (getHeight() - backpackUI.getHeight()) / 2.0f;
            backpackUI.showAttached(x, y);
        }
        GameMaster.game.setInventoryOpen(true);
    }

    /**
     * Restores the player-only view after closing a block or NPC inventory.
     */
    private void closeContainer() {
        if (externalInventory == null) return;
        if (containerBlock != null) {
            containerBlock.setActivated(false);
            SoundService.fx.playUseSound(containerBlock.getType()
                    .getSoundGroup(), 1);
        }
        containerBlock = null;
        externalInventory = null;
        if (backpackUI != null) backpackUI.hideAttached();
        syncContainerInventory();
    }

    /**
     * Sets the hotbar ui.
     * @param hotbarUI the {@link HotbarUI} supplied as {@code hotbarUI}
     */
    public void setHotbarUI(HotbarUI hotbarUI) {
        this.hotbarUI = hotbarUI;
    }

    /**
     * Returns the backpack ui.
     * @return the {@link BackpackInventoryUI} representing the backpack ui
     */
    public BackpackInventoryUI getBackpackUI() {
        return backpackUI;
    }

    /**
     * Sets the backpack ui.
     * @param backpackUI the {@link BackpackInventoryUI} supplied as {@code backpackUI}
     */
    public void setBackpackUI(BackpackInventoryUI backpackUI) {
        this.backpackUI = backpackUI;
    }

}
