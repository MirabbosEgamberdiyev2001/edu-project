import i18n from '@/i18n';

/**
 * Maps frontend language codes to backend JSONB locale keys.
 */
const LANG_KEY_MAP: Record<string, string> = {
  uzl: 'uz_latn',
  uzc: 'uz_cyrl',
  en: 'en',
  ru: 'ru',
};

/**
 * Get the current JSONB locale key based on active i18n language.
 */
export function currentLocaleKey(): string {
  return LANG_KEY_MAP[i18n.language] || 'uz_latn';
}

/**
 * Convert frontend language code to backend JSONB key.
 */
export function toLocaleKey(frontendLang: string): string {
  return LANG_KEY_MAP[frontendLang] || 'uz_latn';
}

/**
 * Resolve a translated field from a JSONB translations map.
 * Mimics backend TranslatedField.resolve() logic:
 *   1. Exact match for current locale
 *   2. Fallback to uz_latn (default)
 *   3. First non-empty value
 *   4. null
 */
export function resolveTranslation(
  translations: Record<string, string> | null | undefined,
  localeKey?: string,
): string | null {
  if (!translations) return null;

  const key = localeKey || currentLocaleKey();

  // 1. Exact match
  const value = translations[key];
  if (value?.trim()) return value;

  // 2. Fallback to uz_latn
  const fallback = translations['uz_latn'];
  if (fallback?.trim()) return fallback;

  // 3. First non-empty value
  const first = Object.values(translations).find((v) => v?.trim());
  return first || null;
}
