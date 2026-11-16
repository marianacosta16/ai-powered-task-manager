/** The backend of the development environment, used when VITE_API_URL is not set. */
export const DEFAULT_API_URL = 'http://localhost:8081'

/** The address of the backend without a trailing slash, so that paths can be added with "/". */
export function resolveApiUrl(value: string | undefined): string {
  const url = value && value.trim() !== '' ? value.trim() : DEFAULT_API_URL
  return url.replace(/\/+$/, '')
}

/**
 * The only place that knows where the backend is. Everything that talks to the API goes through src/api/client.ts,
 * which uses this value. It comes from VITE_API_URL (see .env.example).
 */
export const API_URL = resolveApiUrl(import.meta.env.VITE_API_URL)
