package uz.eduplatform.core.i18n;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Locale key constants for JSONB multilingual fields.
 * Keys used as JSONB object keys in database columns.
 */
public final class LocaleKeys {

    private LocaleKeys() {}

    public static final String UZ_LATN = "uz_latn";
    public static final String UZ_CYRL = "uz_cyrl";
    public static final String EN = "en";
    public static final String RU = "ru";

    public static final String DEFAULT_KEY = UZ_LATN;

    public static final List<String> ALL_KEYS = List.of(UZ_LATN, UZ_CYRL, EN, RU);

    private static final Map<String, String> LOCALE_TAG_TO_KEY = Map.of(
            "uz", UZ_LATN,
            "uz-latn", UZ_LATN,
            "uz-latn-uz", UZ_LATN,
            "uz-cyrl", UZ_CYRL,
            "uz-cyrl-uz", UZ_CYRL,
            "en", EN,
            "en-us", EN,
            "ru", RU,
            "ru-ru", RU
    );

    /**
     * Resolves a Java Locale to our JSONB key.
     * Fallback: uz_latn.
     */
    public static String fromLocale(Locale locale) {
        if (locale == null) return DEFAULT_KEY;

        String tag = locale.toLanguageTag().toLowerCase();
        String key = LOCALE_TAG_TO_KEY.get(tag);
        if (key != null) return key;

        // Try language + script (e.g., "uz-Cyrl" -> "uz_cyrl")
        String script = locale.getScript();
        if (script != null && !script.isEmpty()) {
            String langScript = locale.getLanguage().toLowerCase() + "-" + script.toLowerCase();
            key = LOCALE_TAG_TO_KEY.get(langScript);
            if (key != null) return key;
        }

        // Try language only
        key = LOCALE_TAG_TO_KEY.get(locale.getLanguage().toLowerCase());
        if (key != null) return key;

        return DEFAULT_KEY;
    }
}
