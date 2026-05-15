// i18n configuration
import { createI18n } from 'vue-i18n'
import enUS from './locales/en-US'
import zhCN from './locales/zh-CN'
import zhTW from './locales/zh-TW'
import jaJP from './locales/ja-JP'

export type LocaleKey = 'en-US' | 'zh-CN' | 'zh-TW' | 'ja-JP'

export const SUPPORTED_LOCALES: { key: LocaleKey; label: string; flag: string }[] = [
  { key: 'en-US', label: 'English', flag: '🇺🇸' },
  { key: 'zh-CN', label: '简体中文', flag: '🇨🇳' },
  { key: 'zh-TW', label: '繁體中文', flag: '🇹🇼' },
  { key: 'ja-JP', label: '日本語', flag: '🇯🇵' },
]

const LOCALE_STORAGE_KEY = 'graphiti-locale'

function getDefaultLocale(): LocaleKey {
  const stored = localStorage.getItem(LOCALE_STORAGE_KEY)
  if (stored && SUPPORTED_LOCALES.some(l => l.key === stored)) {
    return stored as LocaleKey
  }
  return 'en-US'
}

export const i18n = createI18n({
  legacy: false,
  locale: getDefaultLocale(),
  fallbackLocale: 'en-US',
  messages: {
    'en-US': enUS,
    'zh-CN': zhCN,
    'zh-TW': zhTW,
    'ja-JP': jaJP,
  },
})

export function setLocale(locale: LocaleKey) {
  i18n.global.locale.value = locale
  localStorage.setItem(LOCALE_STORAGE_KEY, locale)
  document.documentElement.lang = locale
}

export function getCurrentLocale(): LocaleKey {
  return i18n.global.locale.value as LocaleKey
}
