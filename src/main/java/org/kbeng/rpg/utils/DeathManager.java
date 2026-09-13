package org.kbeng.rpg.utils;

import org.kbeng.engine.utils.Local;
import org.kbeng.engine.utils.Utils;

import org.kbeng.rpg.data.Cause;
import org.kbeng.rpg.data.Singleton;
import org.kbeng.rpg.entity.Player;

import java.util.Map;

/**
 * DeathManager provides death manager capabilities within the utils subsystem.
 * It centralizes reusable utilities such as constants, localization, settings, and small cross-cutting helpers.
 * The manager coordinates lifecycle and ordering concerns across dependent runtime components.
 */
@Singleton
@Utils
public class DeathManager {
    public static final DeathManager dth = new DeathManager();
    private static final Map<Byte, String> messages = Map.of(
            Cause.NULL.getId(), "death.reason.unknown",
            Cause.ENTITY.getId(), "death.reason.entity",
            Cause.SELF.getId(), "death.reason.self",
            Cause.BURN.getId(), "death.reason.burned",
            Cause.DROWN.getId(), "death.reason.drown",
            Cause.FALL.getId(), "death.reason.high_fall",
            Cause.VOID.getId(), "death.reason.void",
            Cause.STARVATION.getId(), "death.reason.starvation");
    private Cause lastCauseOfDeath = Cause.NULL;

    /**
     * Stores the cause that produced the player's latest death.
     * @param cause the {@link Cause} argument; the lethal damage cause
     */
    public void setCauseOfDeath(Cause cause) {
        lastCauseOfDeath = cause == null ? Cause.NULL : cause;
    }

    /**
     * Returns the localized message for the latest death cause.
     * @return the {@link String} representing the localized death message
     */
    public String onDeath() {
        String translationKey = messages.getOrDefault(lastCauseOfDeath.getId(),
                messages.get(Cause.NULL.getId()));
        return Local.lang.f(translationKey, Player.plyr.getName());
    }

    /**
     * Returns the death-message translation map.
     * @return the {@link Map} representing the death-message translation map
     */
    public static Map<Byte, String> getMessages() {
        return messages;
    }

    /**
     * Returns cause of death according to the current object state.
     * @return the {@link Cause} representing the cause of the player's latest death
     */
    public Cause getCauseOfDeath() {
        return lastCauseOfDeath;
    }
}
