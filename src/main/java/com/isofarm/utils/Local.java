package com.isofarm.utils;

import com.isofarm.data.Languages;
import com.isofarm.data.Singleton;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;

/**
 * Represents the local component of the Isofarm runtime.
 *
 * <p>This type centralizes the state, lifecycle and behavior required by its callers,
 * keeping domain rules together with the data they operate on.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li><b>State:</b> Owns the data needed to represent the component consistently.</li>
 *   <li><b>Lifecycle:</b> Exposes the operations used to create, update and release its state.</li>
 *   <li><b>Integration:</b> Coordinates with the surrounding game, rendering or UI systems through its public API.</li>
 * </ul>
 *
 * <p>Callers should use the documented public operations and allow this type to preserve
 * its invariants rather than modifying implementation details directly.
 */
@Utils
@Singleton
public class Local {
    private static final Languages[] LANGS = Languages.values();
    public static final Local lang = new Local();
    private final Properties properties = new Properties();
    private Languages currentLanguage;

    /**
     * Creates a new {@code Local} instance.
     */
    private Local() {
        String sysLang = Locale.getDefault().getLanguage();
        Languages initialLang = Languages.EN;
        for (Languages l : LANGS) {
            if (l.getCode().startsWith(sysLang)) {
                initialLang = l;
                break;
            }
        }
        setLanguage(initialLang);
    }

    /**
     * Sets the language.
     * @param language the {@link Languages} supplied as {@code language}
     */
    public void setLanguage(Languages language) {
        if (language == null) return;
        String file = "/lang/lang_" + language.getCode() + ".properties";
        try (InputStream input = Local.class.getResourceAsStream(file)) {
            if (input == null) {
                throw new FileNotFoundException("Localization file not found: " + file);
            }

            properties.clear();
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            this.currentLanguage = language;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates text or selection state for next language.
     * @return the {@link Languages} representing the next language result
     */
    public Languages nextLanguage() {
        int nextIndex = (currentLanguage.ordinal() + 1) % LANGS.length;
        Languages nextLang = LANGS[nextIndex];
        setLanguage(nextLang);
        return nextLang;
    }

    /**
     * Returns the current language.
     * @return the {@link Languages} representing the current language
     */
    public Languages getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Produces the textual or converted representation for t.
     * @param key the {@link String} supplied as {@code key}
     * @return the {@link String} representing the t result
     */
    public String t(String key) {
        return properties.getProperty(key, key);
    }

    /**
     * Produces the textual or converted representation for f.
     * @param s the {@link String} supplied as {@code s}
     * @param args an array of {@link Object} values supplied as {@code args}
     * @return the {@link String} representing the f result
     */
    public String f(String s, Object... args) {
        return MessageFormat.format(t(s), args);
    }

    /**
     * Creates or returns item from the supplied arguments.
     * @param itemKey the {@link String} supplied as {@code itemKey}
     * @param tierKey the {@link String} supplied as {@code tierKey}
     * @return the {@link String} representing the item result
     */
    public String item(String itemKey, String tierKey) {
        String itemName = t(itemKey);
        if (tierKey == null || tierKey.isBlank()) {
            return itemName;
        }
        String tierName = t(tierKey);
        return f("item.format.tiered", tierName, itemName);
    }

    /**
     * Formats an enchanted item name according to the active language's
     * grammatical ordering.
     *
     * @param itemKey the {@link String} argument; localization key of the base item
     * @param enchantmentKey the {@link String} argument; common enchantment key without the
     *                       {@code .adjective} suffix
     * @return the {@link String} representing the localized enchanted item name
     */
    public static String enchanted(String itemKey, String enchantmentKey) {
        String itemName = lang.t(itemKey);
        String adjective = lang.t(enchantmentKey + ".adjective");
        return lang.f("item.enchanted.format", adjective, itemName);
    }
}
