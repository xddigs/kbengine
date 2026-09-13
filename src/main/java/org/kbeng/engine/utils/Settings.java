package org.kbeng.engine.utils;

/**
 * Settings provides settings capabilities within the utils subsystem.
 * It centralizes reusable utilities such as constants, localization, settings, and small cross-cutting helpers.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
public class Settings {
    private static final float BASE_ICON_SIZE = 16.0f;
    private static final float[] GUI_SCALES = {1.0f, 2.0f, 3.0f};
    private static final float SHADOW_MAP_SIZE = 8192.0f;
    private static float fov = 80f;
    private static int ticks = 1200;
    private static float mouseSensitivity = 0.4f;
    private static int renderDistance = 16;
    private static int unloadMargin = 4;
    private static int guiScaleIndex = 2;
    private static boolean doKeepInventory = true;
    private static boolean doEnableMotions = false;
    private static boolean doEnableMusic = true;
    private static boolean doEnableDebugInfo = true;
    private static boolean doEnableShadows = true;
    private static boolean doBookAnimation = false;
    private static boolean doEnableIds = true;
    private static boolean doEnablePaper = false;

    private static float maxInteractionDistance = 4.0f;
    private static int interiorDetectionRadius = 6;
    private static float undergroundViewRadius = 6.0f;
    private static float undergroundCutHeight = 2.25f;

    /**
     * Creates a new {@code Settings} instance.
     */
    private Settings() {}

    /**
     * Returns the shadow map size.
     * @return {@code float}; the shadow map size
     */
    public static float getShadowMapSize() {
        return SHADOW_MAP_SIZE;
    }

    /**
     * Returns the scale.
     * @return {@code float}; the scale
     */
    public static float getScale() {
        return GUI_SCALES[guiScaleIndex];
    }

    /**
     * Transforms this object according to the supplied values.
     * @param value the {@code float} supplied as {@code value}
     * @return {@code float}; the scale result
     */
    public static float scale(float value) {
        return value * getScale();
    }

    /**
     * Returns the scaled entity.
     * @return {@code float}; the scaled entity
     */
    public static float getScaledEntity() {
        return scale(0.335f);
    }

    /**
     * Returns the scaled icon.
     * @return {@code float}; the scaled icon
     */
    public static float getScaledIcon() {
        return scale(BASE_ICON_SIZE);
    }

    /**
     * Returns the scaled text.
     * @return {@code float}; the scaled text
     */
    public static float getScaledText() {
        return scale(12.0f);
    }

    /**
     * Returns the scaled button.
     * @return {@code float}; the scaled button
     */
    public static float getScaledButton() {
        return scale(8.0f);
    }

    /**
     * Returns the scaled label.
     * @return {@code float}; the scaled label
     */
    public static float getScaledLabel() {
        return scale(5.6f);
    }

    /**
     * Returns the scaled tooltip.
     * @return {@code float}; the scaled tooltip
     */
    public static float getScaledTooltip() {
        return scale(4.0f);
    }

    /**
     * Returns the scaled border.
     * @return {@code float}; the scaled border
     */
    public static float getScaledBorder() {
        return scale(1.6f);
    }

    /**
     * Returns the scaled frame.
     * @return {@code float}; the scaled frame
     */
    public static float getScaledFrame() {
        return scale(0.8f);
    }

    /**
     * Returns the scaled window.
     * @return {@code float}; the scaled window
     */
    public static float getScaledWindow() {
        return scale(0.4f);
    }

    /**
     * Returns the scaled slot.
     * @return {@code float}; the scaled slot
     */
    public static float getScaledSlot() {
        return getScaledIcon();
    }

    /**
     * Returns the scaled spacing.
     * @return {@code float}; the scaled spacing
     */
    public static float getScaledSpacing() {
        return scale(1.75f);
    }

    /**
     * Returns the scaled padding.
     * @return {@code float}; the scaled padding
     */
    public static float getScaledPadding() {
        return scale(6.0f);
    }

    /**
     * Returns the scaled corner radius.
     * @return {@code float}; the scaled corner radius
     */
    public static float getScaledCornerRadius() {
        return scale(4.0f);
    }

    /**
     * Returns the scaled thickness.
     * @return {@code float}; the scaled thickness
     */
    public static float getScaledThickness() {
        return scale(0.8f);
    }

    /**
     * Returns the scaled header.
     * @return {@code float}; the scaled header
     */
    public static float getScaledHeader() {
        return getScaledIcon();
    }

    /**
     * Returns the max interaction distance.
     * @return {@code float}; the max interaction distance
     */
    public static float getMaxInteractionDistance() {
        return maxInteractionDistance;
    }

    /**
     * Sets the max interaction distance.
     * @param maxInteractionDistance the {@code float} supplied as {@code maxInteractionDistance}
     */
    public static void setMaxInteractionDistance(float maxInteractionDistance) {
        Settings.maxInteractionDistance = maxInteractionDistance;
    }

    /** Maximum distance used while looking for the four walls of an interior. */
    public static int getInteriorDetectionRadius() {
        return interiorDetectionRadius;
    }

    public static void setInteriorDetectionRadius(int radius) {
        interiorDetectionRadius = Math.clamp(radius, 2, 32);
    }

    /** Radius, in world blocks, revealed around the player underground. */
    public static float getUndergroundViewRadius() {
        return undergroundViewRadius;
    }

    public static void setUndergroundViewRadius(float radius) {
        undergroundViewRadius = Math.clamp(radius, 2.0f, 32.0f);
    }

    /** Height of the underground ceiling cut above the player's feet. */
    public static float getUndergroundCutHeight() {
        return undergroundCutHeight;
    }

    public static void setUndergroundCutHeight(float height) {
        undergroundCutHeight = Math.clamp(height, 1.25f, 8.0f);
    }

    /**
     * Returns the fov.
     * @return {@code float}; the fov
     */
    public static float getFov() {
        return fov;
    }

    /**
     * Sets the fov.
     * @param fov the {@code float} supplied as {@code fov}
     */
    public static void setFov(float fov) {
        Settings.fov = fov;
    }

    /**
     * Updates or derives runtime state for do enable shadows according to the supplied arguments.
     * @return {@code boolean}; the do enable shadows result
     */
    public static boolean doEnableShadows() {
        return doEnableShadows;
    }

    /**
     * Sets the do enable shadows.
     * @param doEnableShadows the {@code boolean} supplied as {@code doEnableShadows}
     */
    public static void setDoEnableShadows(boolean doEnableShadows) {
        Settings.doEnableShadows = doEnableShadows;
    }

    /**
     * Returns the mouse sensitivity.
     * @return {@code float}; the mouse sensitivity
     */
    public static float getMouseSensitivity() {
        return mouseSensitivity;
    }

    /**
     * Updates or derives runtime state for do enable motions according to the supplied arguments.
     * @return {@code float}; the do enable motions result
     */
    public static float doEnableMotions() {
        return doEnableMotions ? 0.8f : 0.0f;
    }

    /**
     * Updates or derives runtime state for do keep inventory according to the supplied arguments.
     * @return {@code boolean}; the do keep inventory result
     */
    public static boolean doKeepInventory() {
        return doKeepInventory;
    }

    /**
     * Sets the do keep inventory.
     * @param doKeepInventory the {@code boolean} supplied as {@code doKeepInventory}
     */
    public static void setDoKeepInventory(boolean doKeepInventory) {
        Settings.doKeepInventory = doKeepInventory;
    }

    /**
     * Sets the do enable motions.
     * @param doEnableMotions the {@code boolean} supplied as {@code doEnableMotions}
     */
    public static void setDoEnableMotions(boolean doEnableMotions) {
        Settings.doEnableMotions = doEnableMotions;
    }

    /**
     * Toggles the setting represented by music and applies it immediately.
     */
    public static void toggleMusic() {
        doEnableMusic = !doEnableMusic;
    }

    /**
     * Updates or derives runtime state for do enable music according to the supplied arguments.
     * @return {@code boolean}; the do enable music result
     */
    public static boolean doEnableMusic() {
        return doEnableMusic;
    }

    /**
     * Toggles the setting represented by debug info and applies it immediately.
     */
    public static void toggleDebugInfo() {
        doEnableDebugInfo = !doEnableDebugInfo;
    }

    /**
     * Updates or derives runtime state for do enable debug info according to the supplied arguments.
     * @return {@code boolean}; the do enable debug info result
     */
    public static boolean doEnableDebugInfo() {
        return doEnableDebugInfo;
    }

    /**
     * Retrieves doBookAnimation
     * @return {@link boolean} value of doBookAnimation
     */
    public static boolean doBookAnimation() {
        return doBookAnimation;
    }

    /**
     * Sets the doBookAnimation value
     * @return {@link boolean} value of doBookAnimation
     */
    public static boolean setDoBookAnimation(boolean doBookAnimation) {
        Settings.doBookAnimation = doBookAnimation;
        return Settings.doBookAnimation;
    }

    /**
     * Toggles the setting represented by enable ids and applies it immediately.
     * @return {@code boolean}; the enable ids result
     */
    public static boolean doEnableIds() {
        return doEnableIds;
    }

    /**
     * Sets the do enable ids.
     * @param doEnableIds the {@code boolean} supplied as {@code doEnableIds}
     */
    public static void setDoEnableIds(boolean doEnableIds) {
        Settings.doEnableIds = doEnableIds;
    }

    /**
     * Returns the {@code doEnablePaper} value
     * @return {@link boolean} value of {@code doEnablePaper}
     */
    public static boolean doEnablePaper() {
        return doEnablePaper;
    }

    /**
     * Sets the doEnablePaper value
     */
    public static void setDoEnablePaper(boolean doEnablePaper) {
        Settings.doEnablePaper = doEnablePaper;
    }

    /**
     * Returns the render distance.
     * @return {@code int}; the render distance
     */
    public static int getRenderDistance() {
        return renderDistance;
    }

    /**
     * Sets the render distance.
     * @param renderDistance the {@code int} supplied as {@code renderDistance}
     */
    public static void setRenderDistance(int renderDistance) {
        Settings.renderDistance = renderDistance;
    }

    /**
     * Returns the unload margin.
     * @return {@code int}; the unload margin
     */
    public static int getUnloadMargin() {
        return unloadMargin;
    }

    /**
     * Sets the unload margin.
     * @param unloadMargin the {@code int} supplied as {@code unloadMargin}
     */
    public static void setUnloadMargin(int unloadMargin) {
        Settings.unloadMargin = unloadMargin;
    }

    /**
     * Returns the ui scale index.
     * @return {@code int}; the ui scale index
     */
    public static int getGuiScaleIndex() {
        return guiScaleIndex;
    }

    /**
     * Returns the ticks.
     * @return {@code float}; the ticks
     */
    public static int getTicks() {
        return ticks;
    }
}
