package com.isofarm.data;

import com.isofarm.utils.Local;

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

    /**
     * Selects the matching NPC voice from {@link SoundGroup#NPC}. Non-binary
     * characters alternate between the two available voice sets until a
     * dedicated recording is supplied.
     *
     * @param disapproving whether the disapproving variant should be selected
     * @return the index of the matching sound in the NPC sound group
     */
    public int getSoundIndex(boolean disapproving) {
        int voiceOffset = switch (this) {
            case FEMALE -> 0;
            case MALE -> 2;
            case NON_BINARY -> Math.random() < 0.5 ? 0 : 2;
        };
        return voiceOffset + (disapproving ? 1 : 0);
    }
}
