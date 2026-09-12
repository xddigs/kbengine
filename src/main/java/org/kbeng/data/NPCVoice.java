package org.kbeng.data;

/**
 * NPCVoice declares the canonical npcvoice set for the data subsystem.
 *
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public enum NPCVoice {
    HMM_FEMALE("fx/entity/hmm_female.ogg"),
    DISAPPROVING_FEMALE("fx/entity/hmm_female_disapproving.ogg"),
    HURT_FEMALE("fx/entity/hurt_female.ogg"),
    HMM_MALE("fx/entity/hmm_male.ogg"),
    DISAPPROVING_MALE("fx/entity/hmm_male_disapproving.ogg"),
    HURT_MALE("fx/entity/hurt_male.ogg");

    private final String soundPath;

    /** Creates a new {@code NPCVoice} instance. */
    NPCVoice(String soundPath) {
        this.soundPath = soundPath;
    }

    /**
     * Returns the classpath path of this voice recording.
     * @return the voice recording path
     */
    public String getSoundPath() {
        return soundPath;
    }
}
