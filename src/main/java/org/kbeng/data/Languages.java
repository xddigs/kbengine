package org.kbeng.data;

import org.kbeng.utils.Local;

/**
 * Languages declares the canonical languages set for the data subsystem.
 *
 * Each constant provides a stable, type-safe branch token for runtime decisions and avoids string-based switches.
 *
 * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
 */
@DataClass
public enum Languages {
    EN("en_US", "engine.lang.english-US"),
    ES("es_ES", "engine.lang.spanish-ES");

    private final String code;
    private final String name;

    /**
     * Creates a new {@code Languages} instance.
     * @param code the {@link String} supplied as {@code code}
     * @param name the {@link String} supplied as {@code name}
     */
    Languages(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * Returns the code.
     * @return the {@link String} representing the code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the name.
     * @return the {@link String} representing the name
     */
    public String getName() {
        return Local.lang.t(name);
    }
}
