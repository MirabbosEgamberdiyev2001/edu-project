import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

import uzlAuth from './locales/uzl/auth.json';
import uzlCommon from './locales/uzl/common.json';
import uzlValidation from './locales/uzl/validation.json';
import uzlSubject from './locales/uzl/subject.json';
import uzlTopic from './locales/uzl/topic.json';
import uzlQuestion from './locales/uzl/question.json';
import uzlTest from './locales/uzl/test.json';
import uzlAdmin from './locales/uzl/admin.json';
import uzlProfile from './locales/uzl/profile.json';

import uzcAuth from './locales/uzc/auth.json';
import uzcCommon from './locales/uzc/common.json';
import uzcValidation from './locales/uzc/validation.json';
import uzcSubject from './locales/uzc/subject.json';
import uzcTopic from './locales/uzc/topic.json';
import uzcQuestion from './locales/uzc/question.json';
import uzcTest from './locales/uzc/test.json';
import uzcAdmin from './locales/uzc/admin.json';
import uzcProfile from './locales/uzc/profile.json';

import enAuth from './locales/en/auth.json';
import enCommon from './locales/en/common.json';
import enValidation from './locales/en/validation.json';
import enSubject from './locales/en/subject.json';
import enTopic from './locales/en/topic.json';
import enQuestion from './locales/en/question.json';
import enTest from './locales/en/test.json';
import enAdmin from './locales/en/admin.json';
import enProfile from './locales/en/profile.json';

import ruAuth from './locales/ru/auth.json';
import ruCommon from './locales/ru/common.json';
import ruValidation from './locales/ru/validation.json';
import ruSubject from './locales/ru/subject.json';
import ruTopic from './locales/ru/topic.json';
import ruQuestion from './locales/ru/question.json';
import ruTest from './locales/ru/test.json';
import ruAdmin from './locales/ru/admin.json';
import ruProfile from './locales/ru/profile.json';

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      uzl: { auth: uzlAuth, common: uzlCommon, validation: uzlValidation, subject: uzlSubject, topic: uzlTopic, question: uzlQuestion, test: uzlTest, admin: uzlAdmin, profile: uzlProfile },
      uzc: { auth: uzcAuth, common: uzcCommon, validation: uzcValidation, subject: uzcSubject, topic: uzcTopic, question: uzcQuestion, test: uzcTest, admin: uzcAdmin, profile: uzcProfile },
      en: { auth: enAuth, common: enCommon, validation: enValidation, subject: enSubject, topic: enTopic, question: enQuestion, test: enTest, admin: enAdmin, profile: enProfile },
      ru: { auth: ruAuth, common: ruCommon, validation: ruValidation, subject: ruSubject, topic: ruTopic, question: ruQuestion, test: ruTest, admin: ruAdmin, profile: ruProfile },
    },
    fallbackLng: 'uzl',
    defaultNS: 'common',
    ns: ['auth', 'common', 'validation', 'subject', 'topic', 'question', 'test', 'admin', 'profile'],
    interpolation: { escapeValue: false },
    detection: {
      order: ['localStorage', 'navigator'],
      lookupLocalStorage: 'i18nextLng',
      caches: ['localStorage'],
    },
  });

export default i18n;
