import { vi } from 'vitest'

/** An answer of the API as `fetch` gives it. */
export function jsonResponse(status: number, body?: unknown): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
  })
}

/**
 * Replaces `fetch` (the network, the only thing the tests do not run for real) by a function that answers with the
 * given responses, one per call, and the last one for any further call. Returns it, to look at the calls.
 */
export function mockFetch(...responses: Array<Response | (() => Promise<Response>) | Error | DOMException>) {
  let call = 0
  const fn = vi.fn(async () => {
    const next = responses[Math.min(call, responses.length - 1)]
    call += 1
    if (next instanceof Error || next instanceof DOMException) {
      throw next
    }
    return typeof next === 'function' ? next() : next.clone()
  })
  vi.stubGlobal('fetch', fn)
  return fn
}

export const ANA = { id: 3, name: 'Ana', role: 'Team member' } as const
export const MARTA = { id: 1, name: 'Marta', role: 'Project manager' } as const

export const SESSION_ENDED = {
  code: 'UNAUTHENTICATED',
  message: 'Your session has ended. Sign in again to continue.',
}
export const SIGN_IN_NEEDED = { code: 'UNAUTHENTICATED', message: 'Sign in to continue.' }
export const BAD_CREDENTIALS = {
  code: 'INVALID_CREDENTIALS',
  message: 'Credentials are not correct. Check your username and password and try again.',
}
