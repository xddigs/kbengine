package org.kbeng.rpg.data;

import org.kbeng.engine.utils.Local;

import java.util.Locale;

/**
 * NPCGender declares the canonical npcgender set for the data subsystem.
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public enum NPCGender {
    FEMALE, MALE, NON_BINARY;

    /**
     * Returns the localized display name for this gender.
     * @return the localized gender name
     */
    public String getDisplayName() {
        return Local.lang.t("npc.gender." + name().toLowerCase(Locale.ROOT));
    }

}
