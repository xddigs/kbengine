package org.kbeng.data;

import org.kbeng.utils.Local;

import java.util.Locale;

/**
 * Enumerates the supported growth stage values.
 */
public enum GrowthStage {
    SEED(0),
    BUD(1),
    GROWING(2),
    MATURE(3),
    HARVESTABLE(4);

    private final int frameIndex;

    /**
     * Creates a new {@code GrowthStage} instance.
     * @param frameIndex the {@code int} supplied as {@code frameIndex}
     */
    GrowthStage(int frameIndex) {
        this.frameIndex = frameIndex;
    }

    /**
     * Returns the name.
     * @return the {@link String} result; {@code String} the name
     */
    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the display name.
     * @return the {@link String} result; {@code String} the display name
     */
    public String getDisplayName() {
        return Local.lang.t("growth." + name().toLowerCase(Locale.ROOT));
    }

    /**
     * Returns the frame index.
     * @return {@code int}; the frame index
     */
    public int getFrameIndex() {
        return frameIndex;
    }
}