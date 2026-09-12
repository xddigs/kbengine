package org.kbeng.service;

import org.kbeng.data.SoundGroup;
import org.kbeng.graphics.GraphicsEngine;
import org.kbeng.utils.Settings;

/**
 * Sincroniza lluvia y paisaje sonoro ambiental por frame.
 *
 * <p>Esta clase encapsula el bloque de decisión basado en
 * {@link WeatherService#isRaining()} para que la orquestación de
 * {@code GameMaster} permanezca limpia.
 */
public final class SoundListener {

    /**
     * Aplica lluvia visual y audio ambiental según el clima actual.
     *
     * @param delta          tiempo de frame en segundos
     * @param graphicsEngine motor gráfico usado para avanzar partículas de lluvia
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

