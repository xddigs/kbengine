package com.isofarm.data;

import com.isofarm.utils.Local;

/**
 * Enumerates the supported reputation values.
 */
@DataClass
public enum Reputation {
    HOSTILE, NEUTRAL, FRIENDLY;

    /**
     * Returns the localized display name for this reputation.
     * @return the localized {@link String} reputation name
     */
    public String getDisplayName() {
        return Local.lang.t("reputation." + name().toLowerCase() + ".name");
    }
}
