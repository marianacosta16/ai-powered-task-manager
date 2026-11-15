/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Address of the backend (src/config.ts); without it, the development address is used. */
  readonly VITE_API_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
