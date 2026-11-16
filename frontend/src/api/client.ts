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
 * Which session the requests belong to. It goes up each time who is signed in changes (a sign in, a sign out, a 401).
 * A request takes the number when it starts, and a 401 that arrives for an older number is ignored: it is the answer
 * of a session that is already over, and it must not sign out the one that came after (or show "Your session has
 * ended" to someone who signed out on purpose).
 */
let sessionNumber = 0

/**
 * Tells the client that who is signed in has changed, so the answers of the requests under way no longer count for a
 * 401. It gives back a function that takes it back, for the case that the change did not happen (a sign out that
 * failed): the user is still in the same session, and the 401 of a request under way is still news. It does nothing
 * if the session changed again meanwhile.
 */
export function startNewSession(): () => void {
  const before = sessionNumber
  sessionNumber += 1
  const started = sessionNumber
  return () => {
    if (sessionNumber === started) {
      sessionNumber = before
    }
  }
}

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
  const response = await send(path, options)
  return (response.status === 204 ? undefined : await readJson(response)) as T
}

/** One page of a list: the items and the total of the server, in all the pages (`X-Total-Count`, A31). */
export interface Page<T> {
  items: T[]
  total: number
}

/** Like {@link request} for a list that comes in pages: it also reads the total from the header. */
export async function requestPage<T>(path: string, options: RequestOptions = {}): Promise<Page<T>> {
  const response = await send(path, options)
  const items = (await readJson(response)) as T[]
  const header = Number(response.headers.get('X-Total-Count'))
  // A missing or wrong header is NaN or 0 (or less than what came): the items are the truth
  return { items, total: header >= items.length ? header : items.length }
}

/** Makes the request and gives back the answer when it is a success; every failure is thrown as an ApiError. */
async function send(path: string, options: RequestOptions): Promise<Response> {
  const { method = 'GET', body, signal } = options
  const session = sessionNumber
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
    return response
  }
  const error = await toApiError(response)
  if (response.status === 401 && path !== '/auth/login' && session === sessionNumber) {
    startNewSession() // the session is over: the 401 of any other request under way is the same news (never taken back)
    unauthorizedHandler?.(error)
  }
  throw error
}

/** The JSON of a success answer; a body that is not JSON (a proxy, a bug) is an error of the API like any other. */
async function readJson(response: Response): Promise<unknown> {
  try {
    return await response.json()
  } catch {
    throw new ApiError(response.status, UNEXPECTED_ERROR, 'Something went wrong. Try again in a moment.')
  }
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
