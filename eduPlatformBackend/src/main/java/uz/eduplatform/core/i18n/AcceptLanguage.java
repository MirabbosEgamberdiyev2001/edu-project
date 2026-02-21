package uz.eduplatform.core.i18n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum AcceptLanguage {

    UZL("uzl", "O'zbek (Lotin)", "uz"),
    UZC("uzc", "Ўзбек (Кирилл)", "uz-Cyrl"),
    RU("ru", "Русский", "ru"),
    EN("en", "English", "en");

    private final String code;
    private final String displayName;
    private final String localeTag;

    public Locale toLocale() {
        return Locale.forLanguageTag(localeTag);
    }

    public String toLocaleKey() {
        return LocaleKeys.fromLocale(toLocale());
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static AcceptLanguage fromCode(String code) {
        if (code == null || code.isBlank()) {
            return UZL;
        }
        String normalized = code.trim().toLowerCase();
        for (AcceptLanguage lang : values()) {
            if (lang.code.equals(normalized)) {
                return lang;
            }
        }
        return UZL;
    }
}
