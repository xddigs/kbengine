package com.isofarm.ui;

import com.isofarm.data.*;
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

/**
 * Encapsulates the state and operations required by inventory ui within the game runtime.
 */
@SuppressWarnings("all")
public class InventoryUI extends UIElement {
    private static final int BACKPACK_COLUMNS = 4;
    private static final int BACKPACK_ROWS = 4;
    private static final int GUI_SLICE_SIZE = 3;
    private static final Logger log = LoggerFactory.getLogger(InventoryUI.class);

    private final InventorySlotUI[] slotUIs;
    private final InventorySlot[] creativeSlotData;
    private final Set<InventorySlot> creativeSlots;
    private final List<Item> creativeItems;

    private final List<UIButton> buttons;
    private final UIScrollBar creativeScrollBar;
    private UIButton sortButton;
    private UIButton groupButton;
    private UIButton backpackButton;
    private final Player player = Player.plyr;
    private Inventory inventory;
    private iBlock containerBlock;
    private SpriteSheet seedIcons;
    private SpriteSheet cropIcons;
    private SpriteSheet blockIcons;
    private SpriteSheet toolIcons;
    private SpriteSheet materialIcons;
    private SpriteSheet inventoryIcons;
    private Item carriedItem;
    private HotbarUI hotbarUI;
    private BackpackInventoryUI backpackUI;
    private int carriedAmount;
    private boolean isGodmode;

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
        this.creativeSlotData = new InventorySlot[totalVisualSlots];
        this.creativeSlots = Collections.newSetFromMap(new IdentityHashMap<>());
        this.creativeItems = new ArrayList<>();
        this.buttons = new ArrayList<>();
        this.creativeScrollBar = createCreativeScrollBar();
        setFocusable(true);
        createButtons();
        addChild(creativeScrollBar);

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
     * Returns the backpack height.
     * @return {@code float}; the backpack height
     */
    public static float getBackpackHeight() {
        return Settings.getScaledPadding() * 2.0f + Settings.getScaledHeader() +
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

    /**
     * Creates and returns the buttons.
     */
    private void createButtons() {
        float btnWidth = Settings.getScaledSlot(), btnHeight = Settings.getScaledSlot();
        sortButton = new UIButton(Settings.getScaledPadding(),
                Settings.getScaledPadding() - Settings.getScaledSpacing(), btnWidth, btnHeight);

        groupButton = new UIButton(Settings.getScaledPadding() + btnWidth + Settings.getScaledSpacing(),
                Settings.getScaledPadding() - Settings.getScaledSpacing(), btnWidth, btnHeight);

        backpackButton = new UIButton(Settings.getScaledPadding() + btnWidth * 2 + Settings.getScaledSpacing() * 3,
                Settings.getScaledPadding() - Settings.getScaledSpacing(), btnWidth, btnHeight);

        sortButton.setOnClick(this::sortInventory);
        groupButton.setOnClick(this::groupInventory);
        backpackButton.setOnClick(() -> {
            if (isBackpackOpen && !isBackpackClosing) {
                closeBackpack();
            } else if (!isBackpackOpen) {
                openBackpack(GameUIService.ui.getBackpackInventoryUI());
            }
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

        if (backpackUI != null && backpackUI.isVisible()) {
            float bpX = getX() + (getWidth() - backpackUI.getWidth()) / 2.0f;
            if (Math.abs(backpackTargetY - backpackCurrentY) > 0.1f) {
                backpackCurrentY += (backpackTargetY - backpackCurrentY) * Math.min(1.0f, delta * 15.0f);
            } else {
                backpackCurrentY = backpackTargetY;
                if (isBackpackClosing) {
                    backpackUI.hide();
                    isBackpackOpen = false;
                    isBackpackClosing = false;
                }
            }
            backpackUI.setPosition(bpX, backpackCurrentY);
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

        if (player == null) return;
        boolean wasOpen = isVisible();
        boolean isOpen = GameMaster.game != null && GameMaster.game.isInventoryOpen();

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
            this.defaultX = (GameMaster.game.getWindowWidth() - getWidth()) / 2.0f;
            this.targetX = defaultX;
            this.defaultY = (GameMaster.game.getWindowHeight() - getHeight()) / 2.0f;
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
            backpackUI.hide();
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
        if (isGodmode) {
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

    /**
     * {@inheritDoc}
     * Scrolls the creative catalog by complete inventory rows while the pointer
     * is anywhere over this inventory.
     */
    @Override
    public boolean mouseScrolled(float mouseX, float mouseY,
                                 float scrollX, float scrollY) {
        if (isGodmode && contains(mouseX, mouseY) && scrollY != 0.0f) {
            creativeScrollBar.scrollBy(scrollY > 0.0f ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /**
     * Switches the controls and contents when GODMODE changes.
     */
    private void updateInventoryMode() {
        boolean creative = containerBlock == null
                && player != null && player.getGamemode().isGodmode();
        if (creative == isGodmode) return;

        isGodmode = creative;
        if (creative) {
            if (backpackUI != null) {
                backpackUI.hide();
                isBackpackOpen = false;
                isBackpackClosing = false;
                targetY = defaultY;
            }
            sortButton.hide();
            groupButton.hide();
            backpackButton.hide();
            buildCreativeCatalog();
            creativeScrollBar.show();
        } else {
            creativeScrollBar.hide();
            sortButton.show();
            groupButton.show();
        }
    }

    /**
     * Builds the creative catalog from registered items that have a supported
     * category, visible sprite and localized display name. The resulting order
     * is shared with {@link CraftingBook#sortByType()}.
     */
    private void buildCreativeCatalog() {
        creativeItems.clear();
        if (GameMaster.game == null) return;

        for (String id : GameMaster.game.getItemRegistry().getIds()) {
            Item item = GameMaster.game.getItemRegistry().create(id);
            if (isSupportedCreativeItem(item)) creativeItems.add(item);
        }

        creativeItems.removeIf(MiningComponent.class::isInstance);
        Tier.forEach(tier -> {
            if (tier.isInvalidTier()) return;
            creativeItems.add(new MiningComponent(tier, MaterialID.RAW_ORE));
            creativeItems.add(new MiningComponent(tier, MaterialID.INGOT));
        });
        creativeItems.sort(CraftingBook.itemTypeComparator());
        int totalRows = Math.ceilDiv(creativeItems.size(), K.UI.INVENTORY_COLUMNS);
        creativeScrollBar.setMaximum(Math.max(0,
                totalRows - K.UI.INVENTORY_ROWS));
        creativeScrollBar.setValue(0);
    }

    /**
     * Checks whether an item belongs to one of the five creative categories and
     * has a drawable, translated inventory representation.
     * @param item the item to validate
     * @return {@code true} when the item may be shown in the creative catalog
     */
    private boolean isSupportedCreativeItem(Item item) {
        boolean supportedCategory = item instanceof Block
                || item instanceof Tool
                || item instanceof Usable
                || item instanceof Material
                || item instanceof iBlock;
        if (!supportedCategory || ResourceManager.getItemSpriteSheet(item) == null
                || ResourceManager.getItemFrame(item) < 0) {
            return false;
        }
        String displayName = item.getDisplayName();
        return displayName != null && !displayName.isBlank()
                && !displayName.equals("block." + item.getName())
                && !displayName.startsWith("item.");
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
        float mouseX = Mouse.getX();
        float mouseY = Mouse.getY();

        for (InventorySlotUI slotUI : slotUIs) {
            if (slotUI != null) {
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
        if (!GameMaster.game.isInventoryOpen()) return;

        InventorySlotUI[] hotbarSlots = hotbarUI.getSlotUIs();
        InventorySlotUI[] backpackSlots = (backpackUI != null && backpackUI.isVisible()) ?
                backpackUI.getSlotUIs() : new InventorySlotUI[0];

        InventorySlotUI[] allSlots = new InventorySlotUI[slotUIs.length + hotbarSlots.length + backpackSlots.length];
        System.arraycopy(slotUIs, 0, allSlots, 0, slotUIs.length);
        System.arraycopy(hotbarSlots, 0, allSlots, slotUIs.length, hotbarSlots.length);
        if (backpackSlots.length > 0) {
            System.arraycopy(backpackSlots, 0, allSlots, slotUIs.length + hotbarSlots.length, backpackSlots.length);
        }

        for (InventorySlotUI slotUI : allSlots) {
            if (slotUI == null || !slotUI.isHovered()) continue;
            InventorySlot slot = slotUI.getSlotType();
            if (slot == null) continue;

            if (Controls.isPressed(ControlAction.UI_SELECT)) {
                if (isGodmode && creativeSlots.contains(slot)) {
                    takeCreativeItem(slot);
                    break;
                }
                leftClick(slot);
                break;
            }

            if (Controls.isPressed(ControlAction.UI_CONTEXT)) {
                if (isGodmode && creativeSlots.contains(slot)) {
                    break;
                }
                rightClick(slot);
                break;
            }
        }
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

    /**
     * Transfers or creates the relevant entity or item for take half.
     * @param slot the {@link InventorySlot} supplied as {@code slot}
     */
    private void takeHalf(InventorySlot slot) {
        if (slot.isEmpty()) {
            return;
        }

        int splitAmount = (int) Math.ceil(slot.getAmount() / 2.0);

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
        renderBackground();
        renderChildren();
        renderCarriedItem();

        if (!isGodmode && inventory != null && inventory.getBackpackSlot() != null
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
        if (isGodmode) {
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

    /**
     * Renders the carried item.
     */
    private void renderCarriedItem() {
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
                         SpriteSheet material, SpriteSheet inv) {
        this.seedIcons = seed;
        this.cropIcons = crop;
        this.blockIcons = block;
        this.toolIcons = tool;
        this.materialIcons = material;
        this.inventoryIcons = inv;

        if (hotbarUI != null) {
            hotbarUI.setSeedIcons(seedIcons);
            hotbarUI.setCropIcons(cropIcons);
            hotbarUI.setBlockIcons(blockIcons);
            hotbarUI.setToolIcons(toolIcons);
            hotbarUI.setMaterialIcons(materialIcons);
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
     *
     * @param block the {@link iBlock} argument; the block whose inventory will be displayed
     */
    public void openContainer(iBlock block) {
        if (block == null || GameMaster.game == null) return;
        this.containerBlock = block;
        this.inventory = block.getInventory();
        GameMaster.game.setInventoryOpen(true);
        SoundService.fx.playUseSound(block.getType().getSoundGroup(), 0);
    }

    /**
     * Restores the player's inventory after closing a block container.
     */
    private void closeContainer() {
        if (containerBlock == null) return;
        containerBlock.setActivated(false);
        SoundService.fx.playUseSound(containerBlock.getType()
                .getSoundGroup(), 1);
        containerBlock = null;
        inventory = player == null ? null : player.getInventory();
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
