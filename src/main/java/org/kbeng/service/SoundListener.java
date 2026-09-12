package org.kbeng.service;

import org.kbeng.data.SoundGroup;
import org.kbeng.graphics.GraphicsEngine;
import org.kbeng.utils.Settings;

/**
 * Applies weather-driven ambient audio policy once per frame.
 *
 * <p>When rain is active, this component advances rain visuals and switches the
 * background loop to the rain ambience. When rain is not active, it restores
 * the nature ambience. If music is disabled in settings, it explicitly clears
 * any background loop.
 */
public final class SoundListener {

    /**
     * Updates weather ambience and rain simulation side effects for the frame.
     *
     * @param delta elapsed frame time in seconds
     * @param graphicsEngine graphics owner used to tick the rain system
     */
    public void update(float delta, GraphicsEngine graphicsEngine) {
        if (WeatherService.isRaining()) {
            graphicsEngine.updateRain(delta);
            if (Settings.doEnableMusic()) {
                SoundService.fx.setBackgroundSound(SoundGroup.RAIN);
            } else {
                SoundService.fx.setBackgroundSound(null);
            }
            return;
        }

        if (Settings.doEnableMusic()) {
            SoundService.fx.setBackgroundSound(SoundGroup.NATURE);
        } else {
            SoundService.fx.setBackgroundSound(null);
        }
    }
}
