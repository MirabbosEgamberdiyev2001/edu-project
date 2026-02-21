package uz.eduplatform.core.i18n;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for resolving locale-specific values from JSONB Map fields.
 * <p>
 * Database format: {"uz_latn": "Matematika", "uz_cyrl": "Математика", "en": "Math", "ru": "Математика"}
 * <p>
 * Resolution chain: exact key -> uz_latn (default) -> first available value -> null
 */
public final class TranslatedField {

    private TranslatedField() {}

    /**
     * Resolve a translated field using the current thread locale.
     */
    public static String resolve(Map<String, String> translations) {
        return resolve(translations, LocaleKeys.fromLocale(LocaleContextHolder.getLocale()));
    }

    /**
     * Resolve a translated field for a specific locale key (e.g., "uz_latn", "en").
     */
    public static String resolve(Map<String, String> translations, String localeKey) {
        if (translations == null || translations.isEmpty()) return null;

        // 1. Exact match
        String value = translations.get(localeKey);
        if (value != null && !value.isBlank()) return value;

        // 2. Fallback to default (uz_latn)
        value = translations.get(LocaleKeys.DEFAULT_KEY);
        if (value != null && !value.isBlank()) return value;

        // 3. Return first non-empty value
        return translations.values().stream()
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);
    }

    /**
     * Wrap a single-locale value into a JSONB-compatible map.
     * Used for backward compatibility when migrating from String to Map.
     */
    public static Map<String, String> wrap(String value) {
        if (value == null) return null;
        Map<String, String> map = new HashMap<>();
        map.put(LocaleKeys.DEFAULT_KEY, value);
        return map;
    }

    /**
     * Wrap a single-locale value with a specific locale key.
     */
    public static Map<String, String> wrap(String value, String localeKey) {
        if (value == null) return null;
        Map<String, String> map = new HashMap<>();
        map.put(localeKey, value);
        return map;
    }

    /**
     * Merge new translations into existing ones (non-null values only).
     */
    public static Map<String, String> merge(Map<String, String> existing, Map<String, String> updates) {
        if (updates == null) return existing;
        if (existing == null) return new HashMap<>(updates);

        Map<String, String> merged = new HashMap<>(existing);
        updates.forEach((key, value) -> {
            if (value != null) {
                merged.put(key, value);
            }
        });
        return merged;
    }

    /**
     * Extract the default locale value from a translatable map.
     * Useful for comparisons and unique constraint checking.
     * Checks both the canonical key (uz_latn) and the frontend alias (uzl).
     */
    public static String defaultValue(Map<String, String> translations) {
        if (translations == null) return null;
        String value = translations.get(LocaleKeys.DEFAULT_KEY);
        if (value != null && !value.isBlank()) return value;
        // Fallback: check frontend alias
        value = translations.get("uzl");
        return (value != null && !value.isBlank()) ? value : null;
    }
    
    /**
     * Clean a translations map: keep only valid locale keys (uz_latn, uz_cyrl, en, ru).
     * Also migrates stale frontend keys ("uzl" -> "uz_latn", "uzc" -> "uz_cyrl").
     */
    public static Map<String, String> clean(Map<String, String> translations) {
        if (translations == null) return null;

        Map<String, String> cleaned = new HashMap<>();

        // Copy known keys
        for (String key : LocaleKeys.ALL_KEYS) {
            String value = translations.get(key);
            if (value != null && !value.isBlank()) {
                cleaned.put(key, value);
            }
        }

        // Migrate stale frontend keys if proper keys are missing
        if (!cleaned.containsKey(LocaleKeys.UZ_LATN)) {
            String uzl = translations.get("uzl");
            if (uzl != null && !uzl.isBlank()) cleaned.put(LocaleKeys.UZ_LATN, uzl);
        }
        if (!cleaned.containsKey(LocaleKeys.UZ_CYRL)) {
            String uzc = translations.get("uzc");
            if (uzc != null && !uzc.isBlank()) cleaned.put(LocaleKeys.UZ_CYRL, uzc);
        }

        return cleaned.isEmpty() ? null : cleaned;
    }
}
