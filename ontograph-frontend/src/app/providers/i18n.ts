import { i18n } from '@/infrastructure/i18n'

export function registerI18n(app: any): void {
  app.use(i18n)
}
