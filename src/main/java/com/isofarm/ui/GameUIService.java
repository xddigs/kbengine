package com.isofarm.ui;

import com.isofarm.data.*;
import com.isofarm.entity.NPC;
import com.isofarm.entity.Player;
import com.isofarm.graphics.Framebuffer;
import com.isofarm.graphics.ResourceManager;
import com.isofarm.graphics.Shader;
import com.isofarm.graphics.SpriteSheet;
import com.isofarm.input.CommandCompletionProvider;
import com.isofarm.input.Mouse;
import com.isofarm.item.Item;
import com.isofarm.service.BookService;
import com.isofarm.service.Service;
import com.isofarm.service.TimeService;
import com.isofarm.utils.*;
import com.isofarm.wrld.GameMaster;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * Encapsulates the state and operations required by game uiservice within the game runtime.
 */
@SuppressWarnings("all")
@GodObject
@Singleton
public final class GameUIService implements Service<GameMaster> {
    public static GameUIService ui;
    private static final Logger log = LoggerFactory.getLogger(GameUIService.class);
    private static final float CHAT_HISTORY_DURATION = 3.0f;
    private static final float HARDWARE_UPDATE_INTERVAL = 0.5f;
    private static final float DEBUG_LABEL_WIDTH = 500.0f;
    private static final float DEATH_OVERLAY_MAX_ALPHA = 0.50f;
    private static final float DEATH_FADE_DURATION = 2.0f;
    private static final float DEATH_BLUR_RADIUS = 5.0f;
    private final GameMaster gameMaster;
    private final UIManager uiManager;
    private final InventoryUI inventoryUI;
    private final BackpackInventoryUI backpackUI;
    private final HotbarUI hotbarUI;

    private final UITextField chatField;
    private final UILabel time;
    private final UILabel coords;
    private final UILabel fps;
    private final UILabel gpu;
    private final UILabel cpu;
    private final UILabel cpuTemp;
    private final UILabel memory;
    private final UILabel youDied;
    private final List<String> chatHistory;
    private final SpriteSheet seedIcons;
    private final SpriteSheet cropIcons;
    private final SpriteSheet blockIcons;
    private final SpriteSheet toolIcons;
    private final SpriteSheet materialIcons;
    private final Player player = Player.plyr;
    private final float startingX = 20.0f;
    private final float startingY = 25.0f;
    private NPC trader;
    private float windowWidth;
    private float windowHeight;
    private Vector2i lastActionCell = null;
    private float hotbarLabelTimer = 0.0f;
    private String hotbarLabel = null;
    private float chatHistoryTimer = 0.0f;
    private float hardwareUpdateTimer = 0.0f;
    private int lastRenderedDamageSequence = -1;
    private float flashTimer = 0.0f;
    private float deathOverlayAlpha = 0.0f;
    private boolean deathScreenActive;

    /**
     * Creates a new {@code GameUIService} instance.
     *
     * @param gameMaster     the {@link GameMaster} supplied as {@code gameMaster}
     * @param uiManager      the {@link UIManager} supplied as {@code uiManager}
     * @param seedIcons      the {@link SpriteSheet} supplied as {@code seedIcons}
     * @param cropIcons      the {@link SpriteSheet} supplied as {@code cropIcons}
     * @param blockIcons     the {@link SpriteSheet} supplied as {@code blockIcons}
     * @param toolIcons      the {@link SpriteSheet} supplied as {@code toolIcons}
     * @param materialIcons  the {@link SpriteSheet} supplied as {@code materialIcons}
     * @param inventoryIcons the {@link SpriteSheet} supplied as {@code inventoryIcons}
     */
    private GameUIService(
            GameMaster gameMaster,
            UIManager uiManager,
            SpriteSheet seedIcons,
            SpriteSheet cropIcons,
            SpriteSheet blockIcons,
            SpriteSheet toolIcons,
            SpriteSheet materialIcons,
            SpriteSheet inventoryIcons) {
        this.gameMaster = gameMaster;
        this.uiManager = uiManager;

        this.seedIcons = seedIcons;
        this.cropIcons = cropIcons;
        this.blockIcons = blockIcons;
        this.toolIcons = toolIcons;
        this.materialIcons = materialIcons;

        this.windowWidth = gameMaster.getWindowWidth();
        this.windowHeight = gameMaster.getWindowHeight();

        this.chatHistory = new ArrayList<>();
        this.inventoryUI = new InventoryUI(windowWidth, windowHeight);
        this.hotbarUI = new HotbarUI(windowWidth, windowHeight);

        this.inventoryUI.setPosition(
                windowWidth / 2.0f - inventoryUI.getAbsoluteWidth() / 2.0f,
                windowHeight / 2.0f - inventoryUI.getAbsoluteHeight() / 2.0f);

        this.hotbarUI.setPosition(
                windowWidth / 2.0f - hotbarUI.getAbsoluteWidth() / 2.0f,
                windowHeight - hotbarUI.getAbsoluteHeight() - K.UI.HOTBAR_OFFSET);

        this.backpackUI = new BackpackInventoryUI(0, 0);
        inventoryUI.setInventory(player.getInventory());
        inventoryUI.createSlots();
        hotbarUI.setInventory(player.getInventory());
        backpackUI.setInventory(player.getBackpack());

        SpriteSheet bookSheet = BookUI.canBeAnimated()
                ? ResourceManager.rem.getBookAnimationSheet()
                : ResourceManager.rem.getBookUI();
        float scale = 4.0f;
        float bookWidth = bookSheet.getFrameWidth() * scale;
        float bookHeight = bookSheet.getFrameHeight() * scale;
        float centerX = (windowWidth - bookWidth) * 0.5f;
        float centerY = (windowHeight - bookHeight) * 0.5f;
        BookUI.init(centerX, centerY, bookWidth, bookHeight);

        inventoryUI.setHotbarUI(hotbarUI);
        backpackUI.setHotbarUI(hotbarUI);
        inventoryUI.setBackpackUI(backpackUI);
        inventoryUI.setIcons(seedIcons, cropIcons, blockIcons,
                toolIcons, materialIcons, inventoryIcons);

        uiManager.getRoot().addChild(inventoryUI);
        uiManager.getRoot().addChild(hotbarUI);
        uiManager.getRoot().addChild(backpackUI);
        uiManager.getRoot().addChild(BookUI.bui);

        final float offset = 32.0f;
        this.chatField = new UITextField(0, windowHeight - offset, windowWidth, offset);
        chatField.setCompletionProvider(new CommandCompletionProvider(gameMaster.getCommandRegistry()));
        uiManager.getRoot().addChild(chatField);

        final float lineheight = 32.0f;
        float y = startingY;

        y += lineheight * 2.0f;
        this.time = new UILabel(startingX, y, 100f, 25f, null);
        this.time.show();
        uiManager.getRoot().addChild(time);

        y += lineheight;
        this.coords = new UILabel(startingX, y, 100f, 25f, null);
        this.coords.show();
        uiManager.getRoot().addChild(coords);

        y += lineheight;
        this.fps = new UILabel(startingX, y, 100f, 25f, null);
        this.fps.show();
        uiManager.getRoot().addChild(fps);

        y += lineheight;
        this.cpu = new UILabel(startingX, y, DEBUG_LABEL_WIDTH, 25f, null);
        this.cpu.show();
        uiManager.getRoot().addChild(cpu);

        y += lineheight;
        this.cpuTemp = new UILabel(startingX, y, DEBUG_LABEL_WIDTH, 25f, null);
        this.cpuTemp.show();
        uiManager.getRoot().addChild(cpuTemp);

        y += lineheight;
        this.gpu = new UILabel(startingX, y, DEBUG_LABEL_WIDTH, 25f, null);
        this.gpu.show();
        uiManager.getRoot().addChild(gpu);

        y += lineheight;
        this.memory = new UILabel(startingX, y, DEBUG_LABEL_WIDTH, 25f, null);
        this.memory.show();
        uiManager.getRoot().addChild(memory);

        this.youDied = new UILabel(100, 100, DEBUG_LABEL_WIDTH, 25f, null);
        this.youDied.setPosition(windowWidth / 2 - DEBUG_LABEL_WIDTH / 2,
                windowHeight / 2 - this.youDied.getFont().getSize());
        this.youDied.setHorizontalAlignment(UILabel.HorizontalAlignment.CENTER);
        this.youDied.hide();
        uiManager.getRoot().addChild(youDied);
    }

    /**
     * Initializes the shared UI service after the OpenGL and UI resources are ready.
     * Subsequent calls preserve the original instance.
     */
    public static void init(GameMaster gameMaster, UIManager uiManager,
                            SpriteSheet seedIcons, SpriteSheet cropIcons,
                            SpriteSheet blockIcons, SpriteSheet toolIcons,
                            SpriteSheet materialIcons, SpriteSheet inventoryIcons) {
        if (ui != null) return;
        ui = new GameUIService(gameMaster, uiManager, seedIcons, cropIcons,
                blockIcons, toolIcons, materialIcons, inventoryIcons);
    }

    /**
     * Updates or derives runtime state for debug print according to the supplied arguments.
     */
    private void debugPrint() {
        uiManager.getRoot().getChildren().forEach(uiElement ->
                log.debug(String.valueOf(uiElement)));
    }

    /**
     * Returns the inventory ui.
     *
     * @return the {@link InventoryUI} representing the inventory ui
     */
    public InventoryUI getInventoryUI() {
        return inventoryUI;
    }

    /**
     * Returns the hotbar ui.
     *
     * @return the {@link HotbarUI} representing the hotbar ui
     */
    public HotbarUI getHotbarUI() {
        return hotbarUI;
    }

    /**
     * Returns the backpack inventory ui.
     *
     * @return the {@link BackpackInventoryUI} representing the backpack inventory ui
     */
    public BackpackInventoryUI getBackpackInventoryUI() {
        return backpackUI;
    }

    /** Sets the trader whose inventory is exposed as the shop stock. */
    public void setTrader(NPC trader) {
        this.trader = trader;
    }

    /**
     * Updates the current state.
     *
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void update(float delta) {
        if (!Player.plyr.isAlive()) {
            if (!deathScreenActive) {
                deathScreenActive = true;
                deathOverlayAlpha = 0.0f;
                youDied.setText(DeathManager.dth.onDeath());
            }
            deathOverlayAlpha = Math.min(DEATH_OVERLAY_MAX_ALPHA,
                    deathOverlayAlpha + delta * DEATH_OVERLAY_MAX_ALPHA / DEATH_FADE_DURATION);
            youDied.show();
        } else {
            deathScreenActive = false;
            deathOverlayAlpha = 0.0f;
            youDied.hide();
        }

        if (Settings.doEnableDebugInfo()) {
            hardwareUpdateTimer -= delta;
            if (hardwareUpdateTimer <= 0.0f) {
                hardwareUpdateTimer = HARDWARE_UPDATE_INTERVAL;
                updateHardwareInfo();
            }
        }

        if (player.getDamageSequence() != lastRenderedDamageSequence) {
            lastRenderedDamageSequence = player.getDamageSequence();
            flashTimer = 0.2f;
        }

        if (flashTimer > 0) {
            flashTimer -= delta;
        }

        if (hotbarLabelTimer > 0.0f) {
            hotbarLabelTimer -= delta;

            if (hotbarLabelTimer <= 0.0f) {
                hotbarLabelTimer = 0.0f;
                hotbarLabel = null;
            }
        }

        if (chatHistoryTimer > 0.0f) {
            chatHistoryTimer -= delta;

            if (chatHistoryTimer < 0.0f) {
                chatHistoryTimer = 0.0f;
            }
        }

        time.setText(TimeService.ts.getFormattedTime());
        fps.setText(gameMaster.getFps());
        if (player != null) {
            coords.setText(player.getPositionString());
        }

        if (Settings.doEnableDebugInfo()) {
            time.show();
            coords.show();
            fps.show();
            cpu.show();
            gpu.show();
            cpuTemp.show();
            memory.show();
        } else {
            time.hide();
            coords.hide();
            fps.hide();
            cpu.hide();
            gpu.hide();
            cpuTemp.hide();
            memory.hide();
        }

        uiManager.update(delta);
        if (BookService.bs.isOpen()) {
            BookUI.bui.update(BookService.bs.getOpenedBook(),
                    BookUI.canBeAnimated()
                            ? ResourceManager.rem.getBookAnimationSheet()
                            : ResourceManager.rem.getBookUI());
        }

        ToastFactory.update(delta);

        if (!gameMaster.isInventoryOpen() && !gameMaster.isBackpackOpen()) {
            float scroll = Mouse.getScrollY();

            if (scroll != 0) {
                selectItem(scroll > 0 ? -1 : 1);
            }
        }
    }

    /**
     * Updates the hardware info.
     */
    private void updateHardwareInfo() {
        cpu.setText("CPU: " + Components.getCpu());
        gpu.setText("GPU: " + Components.getGpu());
        cpuTemp.setText("CPU Temp: " + Components.getCpuTemperature());
        memory.setText("RAM: " + Components.getPhysicalMemory());
    }

    /**
     * Renders this object in the requested render pass.
     *
     * @param isHUDShown the {@code boolean} supplied as {@code isHUDShown}
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     */
    public void render(boolean isHUDShown, GameMaster gameMaster) {
        if (!Player.plyr.isAlive()) {
            renderDeathScreen(gameMaster);
            return;
        }
        Frontend.begin(windowWidth, windowHeight);
        SpriteSheet bookSheet = BookUI.canBeAnimated()
                ? ResourceManager.rem.getBookAnimationSheet()
                : ResourceManager.rem.getBookUI();

        if (BookService.bs.isOpen()) {
            BookUI.bui.render(BookService.bs.getOpenedBook(),
                    gameMaster.getGenDelta(), bookSheet);
        }

        if (isHUDShown) {
            uiManager.render();
            float startX = startingX;
            float startY = startingY;
            renderHearts(ResourceManager.rem.getHeartsSpriteSheet(),
                    startX, startY);
            renderHotbarLabel();
            renderToasts();
        } else {
        }

        renderChatHistory();

        if (!gameMaster.isInventoryOpen() && !gameMaster.isBackpackOpen()
                && !BookUI.bui.isOpen()) {
            Frontend.drawCursor(gameMaster);
        }

        Frontend.end();
        glEnable(GL_DEPTH_TEST);
    }

    /**
     * Renders the progressively blurred and red-tinted death screen.
     */
    private void renderDeathScreen(GameMaster gameMaster) {
        float progress = deathOverlayAlpha / DEATH_OVERLAY_MAX_ALPHA;
        float blurRadius = Math.max(0.01f, DEATH_BLUR_RADIUS * progress);
        Vector2f resolution = new Vector2f(windowWidth, windowHeight);
        Shader blurShader = ResourceManager.rem.getBlurShader();
        Framebuffer sceneFbo = gameMaster.getSceneFbo();
        Framebuffer blurFbo = gameMaster.getBlurFbo();

        glDisable(GL_DEPTH_TEST);
        blurFbo.bind();
        glClear(GL_COLOR_BUFFER_BIT);
        blurShader.bind();
        blurShader.setUniform("uResolution", resolution);
        blurShader.setUniform("uDirection", new Vector2f(1.0f, 0.0f));
        blurShader.setUniform("uBlurRadius", blurRadius);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, sceneFbo.getTextureId());
        blurShader.setUniform("screenTexture", 0);
        ResourceManager.rem.getScreenQuadMesh().render();
        blurShader.unbind();
        blurFbo.unbind((int) windowWidth, (int) windowHeight);

        glClear(GL_COLOR_BUFFER_BIT);
        blurShader.bind();
        blurShader.setUniform("uResolution", resolution);
        blurShader.setUniform("uDirection", new Vector2f(0.0f, 1.0f));
        blurShader.setUniform("uBlurRadius", blurRadius);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, blurFbo.getTextureId());
        blurShader.setUniform("screenTexture", 0);
        ResourceManager.rem.getScreenQuadMesh().render();
        blurShader.unbind();

        Frontend.begin(windowWidth, windowHeight);
        Frontend.drawRect(0.0f, 0.0f, windowWidth, windowHeight,
                new Vector4f(0.65f, 0.0f, 0.0f, deathOverlayAlpha));
        youDied.render();
        Frontend.end();
        glEnable(GL_DEPTH_TEST);
    }

    /**
     * Renders the hearts.
     *
     * @param heartsSheet the {@link SpriteSheet} supplied as {@code heartsSheet}
     * @param startX      the {@code float} supplied as {@code startX}
     * @param startY      the {@code float} supplied as {@code startY}
     */
    public void renderHearts(SpriteSheet heartsSheet, float startX,
                             float startY) {
        if (heartsSheet == null) return;
        int currentHp = (int) player.getHitpoints();
        int maxHp = (int) player.getMaxHitpoints();

        int totalHearts = (maxHp + 1) / 2;
        int heartsPerRow = 10;

        float heartSize = Settings.getScaledIcon() / 2f;
        float spacing = Settings.getScaledSpacing() - 5.0f;
        float rowSpacing = spacing;

        heartsSheet.bind();
        for (int i = 0; i < totalHearts; i++) {
            int col = i % heartsPerRow;
            int row = i / heartsPerRow;

            float posX = startX + col * (heartSize + spacing);
            float posY = startY - row * (heartSize + rowSpacing);
            int heartHp = Math.min(2, Math.max(0, currentHp - (i * 2)));
            int frame = (heartHp == 2) ? 0 : (heartHp == 1 ? 1 : 2);
            Vector4f color = new Vector4f(1.0f);

            Frontend.drawSprite(heartsSheet, frame, posX, posY, heartSize, heartSize, color);
            if (flashTimer > 0.0f) {
                int overlayFrame = 3;
                Frontend.drawSprite(heartsSheet, overlayFrame, posX, posY, heartSize, heartSize, color);
            }
        }
    }

    /**
     * Returns the uimanager.
     *
     * @return the {@link UIManager} representing the uimanager
     */
    public UIManager getUIManager() {
        return uiManager;
    }

    /**
     * Renders the chat history.
     */
    private void renderChatHistory() {
        if (chatHistoryTimer <= 0.0f || chatHistory.isEmpty()) {
            return;
        }

        float x = K.UI.CHAT_HISTORY_X;
        float y = chatField.getY() - K.UI.CHAT_HISTORY_OFFSET_Y;

        Vector4f color = K.UI.CHAT_HISTORY_TEXT_COLOR;

        int start = Math.max(
                0,
                chatHistory.size() - K.UI.CHAT_HISTORY_MAX_MESSAGES
        );

        for (int i = chatHistory.size() - 1; i >= start; i--) {
            Frontend.drawNormalString(chatHistory.get(i), x, y, color);
            y -= K.UI.CHAT_HISTORY_LINE_HEIGHT;
        }
    }

    /**
     * Adds the chat message.
     *
     * @param message the {@link String} supplied as {@code message}
     */
    public void addChatMessage(String message) {
        if (message == null || message.isBlank()) return;
        chatHistory.add(message);
        if (chatHistory.size() > 50) {
            chatHistory.remove(0);
        }

        chatHistoryTimer = CHAT_HISTORY_DURATION;
    }

    /**
     * Renders the hotbar label.
     */
    private void renderHotbarLabel() {
        if (hotbarLabel == null || hotbarLabelTimer <= 0.0f) {
            return;
        }

        float alpha = Math.min(1.0f, hotbarLabelTimer * 2.0f);
        Vector4f color = new Vector4f(1.0f, 1.0f, 1.0f, alpha);
        float x = windowWidth / 2.0f - Frontend.getStringWidth(hotbarLabel, Frontend.getNormalFont()) / 2.0f;
        float y = hotbarUI.getY() - K.UI.HOTBAR_LABEL_OFFSET_Y * Settings.getScale();
        Frontend.drawNormalString(hotbarLabel, x, y, color);
    }

    /**
     * Resets hotbar position to its initial runtime state.
     */
    public void resetHotbarPosition() {
        hotbarUI.refreshSize();
        float hotbarX = windowWidth / 2.0f - hotbarUI.getWidth() / 2.0f;
        float hotbarY = windowHeight - hotbarUI.getHeight() - K.UI.HOTBAR_OFFSET;
        hotbarUI.setPosition(hotbarX, hotbarY);
        refreshHotbarLabel();
    }

    /**
     * Refreshes dependent runtime state for refresh hotbar label.
     */
    public void refreshHotbarLabel() {
        Item item = hotbarUI.getSelectedItem();

        if (item != null) {
            showHotbarLabel(item);
        } else {
            hotbarLabel = null;
            hotbarLabelTimer = 0.0f;
        }
    }

    /**
     * Renders the toasts.
     */
    public void renderToasts() {
        if (ToastFactory.isEmpty()) {
            return;
        }

        for (Toast toast : ToastFactory.getToasts()) {
            renderToast(toast);
        }
    }

    /**
     * Updates or derives runtime state for select item according to the supplied arguments.
     *
     * @param direction the {@code int} supplied as {@code direction}
     */
    public void selectItem(int direction) {
        if (player == null) {
            return;
        }

        if (direction > 0) {
            hotbarUI.selectNext();
        } else if (direction < 0) {
            hotbarUI.selectPrevious();
        }

        Item item = Settings.selectedItem;
        if (item != null) {
            showHotbarLabel(item);
        } else {
            hotbarLabel = null;
            hotbarLabelTimer = 0.0f;
        }
    }

    /**
     * Activates hotbar label and prepares any state it requires.
     *
     * @param item the {@link Item} supplied as {@code item}
     */
    private void showHotbarLabel(Item item) {
        if (item == null) {
            hotbarLabel = null;
            hotbarLabelTimer = 0.0f;
            return;
        }

        hotbarLabel = item.getDisplayName();
        hotbarLabelTimer = K.UI.HOTBAR_LABEL_DURATION;
    }

    /**
     * Renders the toast.
     *
     * @param toast the {@link Toast} supplied as {@code toast}
     */
    private void renderToast(Toast toast) {
        UIFont prefixFont = Frontend.getNormalBoldFont();
        UIFont messageFont = Frontend.getNormalFont();

        String prefix = getToastPrefix(toast.getType());
        String message = toast.getMessage();

        float paddingLeft = Settings.scale(K.UI.TOAST_MESSAGE_OFFSET_X);
        float paddingRight = Settings.scale(K.UI.TOAST_PADDING_RIGHT);
        float gap = Settings.scale(K.UI.TOAST_GAP_X);
        float accentWidth = Settings.scale(K.UI.TOAST_ACCENT_WIDTH);

        float prefixWidth = Frontend.getStringWidth(prefix, prefixFont);
        float messageWidth = Frontend.getStringWidth(message, messageFont);

        float x = toast.getX();
        float y = toast.getY();

        float marginFromEdge = Settings.scale(10.0f);
        float maxPossibleWidth = Math.max(100.0f, this.windowWidth - x - marginFromEdge);

        float baseWidth = Settings.scale(K.UI.TOAST_WIDTH);
        float contentWidth = paddingLeft + prefixWidth + gap + messageWidth + paddingRight;

        float width = Math.min(maxPossibleWidth, Math.max(baseWidth, contentWidth));

        float availableTextWidth = Math.max(Settings.scale(100.0f),
                width - paddingLeft - prefixWidth - gap - paddingRight);

        String[] lines = Frontend.wrapText(message, availableTextWidth, messageFont);

        float fontHeight = messageFont.getSize();
        float lineHeight = fontHeight * 1.2f;
        float textBlockHeight = lines.length * lineHeight;

        float minHeight = Settings.scale(36.0f);
        float height = Math.max(minHeight, textBlockHeight + Settings.scale(12.0f));

        float[] bg = getToastBackground(toast.getType());
        float[] acc = getToastAccent(toast.getType());
        Vector4f bgColor = new Vector4f(bg[0], bg[1], bg[2], bg[3]);
        Vector4f accentColor = new Vector4f(acc[0], acc[1], acc[2], acc[3]);

        float cornerRadius = Settings.getScaledCornerRadius();
        Frontend.drawRect(x, y, width, height, bgColor, cornerRadius);
        Frontend.drawRect(x, y, accentWidth, height, accentColor, cornerRadius);

        float messageX = x + paddingLeft + prefixWidth + gap;
        float startY = y + (height - textBlockHeight) * 0.5f + (fontHeight * 0.7f);
        float prefixFontHeight = prefixFont.getSize();
        float prefixY = y + (height - prefixFontHeight) * 0.5f + (prefixFontHeight * 0.7f);
        Frontend.drawString(prefix, x + paddingLeft, prefixY, prefixFont, accentColor, 1.0f);

        for (int i = 0; i < lines.length; i++) {
            Frontend.drawString(lines[i], messageX, startY + (i * lineHeight), messageFont, K.UI.UI_TEXT_COLOR, 1.0f);
        }
    }

    /**
     * Returns the toast accent.
     *
     * @param type the {@link ToastData} supplied as {@code type}
     * @return an array of {@code float} values; the toast accent
     */
    private float[] getToastAccent(ToastData type) {
        return switch (type) {
            case SUCCESS -> K.Style.COLOR_TOAST_SUCCESS;
            case INFO -> K.Style.COLOR_TOAST_INFO;
            case WARNING -> K.Style.COLOR_TOAST_WARNING;
            case ERROR -> K.Style.COLOR_TOAST_ERROR;
            case REWARD -> K.Style.COLOR_TOAST_REWARD;
            case PURCHASE -> K.Style.COLOR_TOAST_SUCCESS;
            case SELL -> K.Style.COLOR_TOAST_REWARD;
        };
    }

    /**
     * Returns the toast background.
     *
     * @param type the {@link ToastData} supplied as {@code type}
     * @return an array of {@code float} values; the toast background
     */
    private float[] getToastBackground(ToastData type) {
        return switch (type) {
            case SUCCESS -> K.Style.COLOR_TOAST_SUCCESS_BG;
            case INFO -> K.Style.COLOR_TOAST_INFO_BG;
            case WARNING -> K.Style.COLOR_TOAST_WARNING_BG;
            case ERROR -> K.Style.COLOR_TOAST_ERROR_BG;
            case REWARD -> K.Style.COLOR_TOAST_REWARD_BG;
            case PURCHASE -> K.Style.COLOR_TOAST_SUCCESS_BG;
            case SELL -> K.Style.COLOR_TOAST_REWARD_BG;
        };
    }

    /**
     * Returns the toast prefix.
     *
     * @param type the {@link ToastData} supplied as {@code type}
     * @return the {@link String} representing the toast prefix
     */
    private String getToastPrefix(ToastData type) {
        return switch (type) {
            case PURCHASE, SUCCESS -> "+";
            case SELL -> "**";
            case INFO -> "i";
            case WARNING -> "!";
            case ERROR -> "X";
            case REWARD -> "*";
        };
    }

    /**
     * Publishes the notification represented by log action.
     *
     * @param cell the {@link BlockPos} supplied as {@code cell}
     */
    public void logAction(BlockPos cell) {
        this.lastActionCell = new Vector2i(cell.x(), cell.y());
    }

    /**
     * Handles resize and updates the affected state.
     *
     * @param width  the {@code int} supplied as {@code width}
     * @param height the {@code int} supplied as {@code height}
     */
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        ToastFactory.setWindowWidth(width);
        ToastFactory.info(Local.lang.f("engine.window_resized", width, height));
        resetHotbarPosition();

        if (inventoryUI != null) {
            float inventoryX = (width - inventoryUI.getAbsoluteWidth()) / 2.0f;
            float inventoryY = (height - inventoryUI.getAbsoluteHeight()) / 2.0f;
            inventoryUI.setPosition(inventoryX, inventoryY);
        }

        if (chatField != null) {
            chatField.setPosition(10, height - 40);
            chatField.setWidth(width - 20);
        }

        if (youDied != null) {
            youDied.setPosition(width / 2.0f - DEBUG_LABEL_WIDTH / 2.0f,
                    height / 2.0f - youDied.getFont().getSize());
        }
    }

    /**
     * Activates chat and prepares any state it requires.
     */
    public void openChat() {
        chatField.clear();
        chatField.show();
        uiManager.setFocusedElement(chatField);
    }

    /**
     * Releases the resources associated with chat.
     */
    public void closeChat() {
        chatField.hide();
        uiManager.clearFocus();
    }

    /**
     * Returns the chat text.
     *
     * @return the {@link String} representing the chat text
     */
    public String getChatText() {
        return chatField.getText();
    }

    /**
     * Processes sell item and updates the affected inventory or currency balances.
     *
     * @param inv  the {@link Inventory} supplied as {@code inv}
     * @param item the {@link Item} supplied as {@code item}
     */
    private void sellItem(Inventory inv, Item item) {
        if (trader == null) return;
        Item targetItem = null;
        int cumulativeAmount = 0;

        for (Map.Entry<Item, Integer> entry :
                new HashMap<>(inv.getItems()).entrySet()) {
            if (entry.getKey().getName().equals(item.getName())) {
                targetItem = entry.getKey();
                cumulativeAmount += entry.getValue();
                player.sell(entry.getKey(), entry.getValue());
            }
        }

        if (targetItem != null && cumulativeAmount > 0) {
            trader.buy(targetItem, cumulativeAmount);
        }
    }

    /**
     * Processes buy item and updates the affected inventory or currency balances.
     *
     * @param stock  the {@link Inventory} supplied as {@code stock}
     * @param item   the {@link Item} supplied as {@code item}
     * @param amount the {@code int} supplied as {@code amount}
     */
    private void buyItem(Inventory stock, Item item, int amount) {
        if (trader == null || amount <= 0) return;

        int totalPrice = item.getValue() * amount;
        if (player.hasWallet() == null || player.hasWallet().coins() < totalPrice) {
            log.warn("Player doesn't have enough money to buy {} x{}",
                    item.getName(), amount);
            ToastFactory.warning(Local.lang.t("toast.item_not_enough_coins"));
            return;
        }

        player.spend(totalPrice);
        trader.earn(totalPrice);

        stock.remove(item, amount);
        player.getInventory().add(item, amount);

        log.info("Player bought {} x{} from shop", item.getName(), amount);
//      @dead-code ToastFactory.success(Local.lang.f("toast.item_bought", amount,item.getName(), totalPrice));
    }
}
