package com.isofarm.ui;

import com.isofarm.data.Inventory;
import com.isofarm.data.SlotType;
import com.isofarm.entity.Player;
import com.isofarm.utils.Settings;

/**
 * Encapsulates the state and operations required by backpack inventory ui within the game runtime.
 */
public class BackpackInventoryUI extends InventoryUI {
    private static final int BACKPACK_SLOTS = 16;
    private final InventorySlotUI[] backpackSlots = new InventorySlotUI[BACKPACK_SLOTS];
    private Inventory backpack;

    /**
     * Creates a new {@code BackpackInventoryUI} instance.
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     */
    public BackpackInventoryUI(float x, float y) {
        super(x, y);
        backpack = Player.plyr.getBackpack();
        setInventory(backpack);
        setBackpackUI(this);
        getButtons().forEach(this::removeChild);

        hide();
        setWidth(getBackpackWidth());
        setHeight(getBackpackHeight());
        setLayer(50);
        createBackpackSlots();
    }

    /**
     * Creates the backpack slots directly below its top padding because this
     * inventory has no control header.
     */
    private void createBackpackSlots() {
        for (int i = 0; i < BACKPACK_SLOTS; i++) {
            int column = i % 4;
            int row = i / 4;
            float x = Settings.getScaledPadding() + column * (Settings.getScaledSlot() + Settings.getScaledSpacing());
            float y = Settings.getScaledPadding()
                    + row * (Settings.getScaledSlot() + Settings.getScaledSpacing());

            InventorySlotUI slotUI = new InventorySlotUI(x, y, Settings.getScaledSlot(), Settings.getScaledSlot(),
                    SlotType.BACKPACK);

            backpackSlots[i] = slotUI;
            addChild(slotUI);
        }
    }

    /**
     * {@inheritDoc}
     * Returns the slot uis.
     * @return an array of {@link InventorySlotUI} values; the slot uis
     */
    @Override
    public InventorySlotUI[] getSlotUIs() {
        return backpackSlots;
    }

    /**
     * Returns the backpack.
     * @return the {@link Inventory} representing the backpack
     */
    public Inventory getBackpack() {
        return backpack;
    }

    /**
     * {@inheritDoc}
     * Updates the current state.
     * @param delta the {@code float} supplied as {@code delta}
     */
    @Override
    public void update(float delta) {
        if (getBackpack() == null) return;
        syncInventory();
        getChildren().forEach(child -> child.update(delta));
    }

    /**
     * {@inheritDoc}
     * Renders this object in the requested render pass.
     */
    @Override
    public void render() {
        if (!isVisible()) return;
        renderBackground();
        renderChildren();
    }
}
