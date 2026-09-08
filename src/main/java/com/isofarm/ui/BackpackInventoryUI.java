package com.isofarm.ui;

import com.isofarm.data.Inventory;
import com.isofarm.data.SlotType;
import com.isofarm.entity.Player;
import com.isofarm.utils.Settings;
import com.isofarm.wrld.GameMaster;

/**
 * Encapsulates the state and operations required by backpack inventory ui within the game runtime.
 */
public class BackpackInventoryUI extends InventoryUI {
    private static final int BACKPACK_SLOTS = 16;
    private final InventorySlotUI[] backpackSlots = new InventorySlotUI[BACKPACK_SLOTS];
    private final Inventory backpack;
    private float targetY;
    private boolean closing;
    private boolean attachedToInventory;

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
        if (getBackpack() == null || GameMaster.game == null) {
            hideAttached();
            return;
        }

        if (attachedToInventory) {
            syncInventory();
            getChildren().forEach(child -> child.update(delta));
            return;
        }

        if (!GameMaster.game.isBackpackOpen()) {
            startClosingAnimation();
            animatePosition(delta);
            return;
        }

        if (!isVisible()) {
            float centeredX = (GameMaster.game.getWindowWidth() - getWidth()) / 2.0f;
            setPosition(centeredX, GameMaster.game.getWindowHeight());
            targetY = (GameMaster.game.getWindowHeight() - getHeight()) / 2.0f;
            closing = false;
            show();
        }
        animatePosition(delta);
        syncInventory();
        updateSlots();
        slotInteract();
        getChildren().forEach(child -> child.update(delta));
    }

    /** Displays this panel alongside another inventory, such as a trader's stock. */
    public void showAttached(float x, float y) {
        attachedToInventory = true;
        closing = false;
        setPosition(x, y);
        show();
    }

    /** Hides this panel after it has been attached to another inventory. */
    public void hideAttached() {
        attachedToInventory = false;
        closing = false;
        hide();
    }

    /** Returns whether this panel is currently embedded beside another inventory. */
    public boolean isAttachedToInventory() {
        return attachedToInventory;
    }

    private void startClosingAnimation() {
        if (!isVisible() || closing) return;
        closing = true;
        targetY = GameMaster.game.getWindowHeight();
    }

    private void animatePosition(float delta) {
        float currentY = getY();
        if (Math.abs(targetY - currentY) > 0.1f) {
            setPosition(getX(), currentY + (targetY - currentY)
                    * Math.min(1.0f, delta * 15.0f));
            return;
        }
        setPosition(getX(), targetY);
        if (closing) {
            closing = false;
            hide();
        }
    }

    /** Synchronizes the standalone panel directly with the equipped backpack. */
    @Override
    protected void syncInventory() {
        for (int i = 0; i < backpackSlots.length; i++) {
            InventorySlotUI slotUI = backpackSlots[i];
            slotUI.setSlot(i < backpack.getSlots().size() ? backpack.getSlot(i) : null);
            updateItemSprite(slotUI);
        }
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
        renderQuickMoveAnimations();
    }
}
