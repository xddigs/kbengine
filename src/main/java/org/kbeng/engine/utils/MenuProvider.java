package org.kbeng.engine.utils;

/** Contributes options to a terminal menu without coupling the engine to applications. */
public interface MenuProvider {
    /** Configures one or more menu options. */
    void configure(Menu menu);

    /** Controls provider ordering; lower values appear first. */
    default int order() {
        return 0;
    }
}
