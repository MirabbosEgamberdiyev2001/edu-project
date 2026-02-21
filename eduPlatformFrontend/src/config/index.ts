export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export const ACCESS_TOKEN_KEY = 'access_token';
export const REFRESH_TOKEN_KEY = 'refresh_token';

export const OTP_LENGTH = 6;
export const OTP_COUNTDOWN_SECONDS = 120;
export const PHONE_PREFIX = '+998';
export const PHONE_PATTERN = /^\+998[0-9]{9}$/;

export const PASSWORD_MIN_LENGTH = 6;
export const PASSWORD_MAX_LENGTH = 64;

// Frontend language code -> Backend Accept-Language header value
export const LANGUAGE_MAP: Record<string, string> = {
  uzl: 'uzl',
  uzc: 'uzc',
  en: 'en',
  ru: 'ru',
};

export const SUPPORTED_LANGUAGES = ['uzl', 'uzc', 'en', 'ru'] as const;
export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number];

export const LANGUAGE_LABELS: Record<string, string> = {
  uzl: "O'zbekcha",
  uzc: 'Ўзбекча',
  en: 'English',
  ru: 'Русский',
};

export const LANGUAGE_SHORT_LABELS: Record<string, string> = {
  uzl: 'UZ',
  uzc: 'ЎЗ',
  en: 'EN',
  ru: 'RU',
};
