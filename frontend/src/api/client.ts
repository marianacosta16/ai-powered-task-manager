import { API_URL } from '../config'
import { ApiError, NETWORK_ERROR, UNEXPECTED_ERROR, type FieldIssue } from './errors'

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE'
  /** Sent as JSON. */
  body?: unknown
  signal?: AbortSignal
}

type UnauthorizedHandler = (error: ApiError) => void

let unauthorizedHandler: UnauthorizedHandler | null = null

/**
 * Registers what happens when the API says the session is not valid (401) in a request other than signing in:
 * the application sends the user to the sign in screen with the message of the API (P33).
 */
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler
}

/**
 * Calls the API, with the session cookie, and gives back the JSON of the answer (nothing for a 204). It throws an
 * {@link ApiError} for an error of the API, and for a request that could not be made (code NETWORK_ERROR).
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, signal } = options
  let response: Response
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      credentials: 'include', // the session is a cookie (T3)
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal,
    })
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause
    }
    throw new ApiError(0, NETWORK_ERROR, 'The server cannot be reached. Check your connection and try again.')
  }

  if (response.ok) {
    return (response.status === 204 ? undefined : await response.json()) as T
  }
  const error = await toApiError(response)
  if (response.status === 401 && path !== '/auth/login') {
    unauthorizedHandler?.(error)
  }
  throw error
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as { code?: unknown; message?: unknown; details?: unknown }
    if (typeof body.code === 'string' && typeof body.message === 'string') {
      return new ApiError(response.status, body.code, body.message, toIssues(body.details))
    }
  } catch {
    // not JSON: falls to the generic error below
  }
  return new ApiError(response.status, UNEXPECTED_ERROR, 'Something went wrong. Try again in a moment.')
}

function toIssues(details: unknown): FieldIssue[] {
  if (!Array.isArray(details)) {
    return []
  }
  return details.filter(
    (d): d is FieldIssue => typeof d?.field === 'string' && typeof d?.issue === 'string',
  )
}
