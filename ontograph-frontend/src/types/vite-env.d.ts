/// <reference types="vite/client" />

declare module 'url' {
  export function fileURLToPath(url: URL | string): string
  export class URL {
    constructor(url: string, base?: string)
    pathname: string
    href: string
  }
}
