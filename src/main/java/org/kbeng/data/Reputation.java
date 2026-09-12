package org.kbeng.data;

import org.kbeng.utils.Local;

/**
 * Reputation declares the canonical reputation set for the data subsystem.
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
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
