package com.isofarm.utils;

import com.isofarm.data.Cause;
import com.isofarm.data.Singleton;
import com.isofarm.entity.Player;

import java.util.Map;

/**
 * Provides death manager behavior, such as
 * death messages.
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
