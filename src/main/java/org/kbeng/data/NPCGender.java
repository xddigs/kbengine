package org.kbeng.data;

import org.kbeng.utils.Local;

import java.util.Locale;

/**
 * Enumerates the available NPC genders
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
