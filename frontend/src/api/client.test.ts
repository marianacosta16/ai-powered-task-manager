import { afterEach, describe, expect, it, vi } from 'vitest'
import { API_URL } from '../config'
import { deferredResponse, jsonResponse, mockFetch } from '../test/helpers'
import { request, setUnauthorizedHandler, startNewSession } from './client'
import { ApiError, NETWORK_ERROR, UNEXPECTED_ERROR } from './errors'

afterEach(() => setUnauthorizedHandler(null))

describe('the client of the API', () => {
  it('calls the backend with the session cookie and asks for JSON', async () => {
    const fetch = mockFetch(jsonResponse(200, { id: 1 }))

    const result = await request<{ id: number }>('/auth/me')

    expect(result).toEqual({ id: 1 })
    const [url, init] = fetch.mock.calls[0] as unknown as [string, RequestInit]
    expect(url).toBe(`${API_URL}/auth/me`)
    expect(init.method).toBe('GET')
    expect(init.credentials).toBe('include')
    expect((init.headers as Record<string, string>).Accept).toBe('application/json')
    expect(init.body).toBeUndefined()
  })

  it('sends a body as JSON', async () => {
    const fetch = mockFetch(jsonResponse(200, {}))

    await request('/auth/login', { method: 'POST', body: { username: 'ana', password: 'x' } })

    const [, init] = fetch.mock.calls[0] as unknown as [string, RequestInit]
    expect(init.body).toBe('{"username":"ana","password":"x"}')
    expect((init.headers as Record<string, string>)['Content-Type']).toBe('application/json')
  })

  it('gives nothing back for a 204', async () => {
    mockFetch(jsonResponse(204))

    await expect(request<void>('/auth/logout', { method: 'POST' })).resolves.toBeUndefined()
  })

  it('throws the error of the contract, with its code, message and fields', async () => {
    mockFetch(
      jsonResponse(400, {
        code: 'VALIDATION_ERROR',
        message: 'Some values are missing or not valid.',
        details: [{ field: 'name', issue: 'Must have 3 to 100 characters.' }, { nonsense: true }],
      }),
    )

    const error = await request('/projects', { method: 'POST', body: {} }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    const api = error as ApiError
    expect(api.status).toBe(400)
    expect(api.code).toBe('VALIDATION_ERROR')
    expect(api.message).toBe('Some values are missing or not valid.')
    expect(api.details).toEqual([{ field: 'name', issue: 'Must have 3 to 100 characters.' }])
    expect(api.issueFor('name')).toBe('Must have 3 to 100 characters.')
    expect(api.issueFor('other')).toBeUndefined()
  })

  it('throws a generic error when the answer is not the error of the contract', async () => {
    mockFetch(new Response('<html>Bad gateway</html>', { status: 502 }))

    const error = (await request('/projects').catch((e: unknown) => e)) as ApiError

    expect(error.status).toBe(502)
    expect(error.code).toBe(UNEXPECTED_ERROR)
    expect(error.message).toBe('Something went wrong. Try again in a moment.')
  })

  it('throws a network error when the server cannot be reached', async () => {
    mockFetch(new TypeError('Failed to fetch'))

    const error = (await request('/projects').catch((e: unknown) => e)) as ApiError

    expect(error.code).toBe(NETWORK_ERROR)
    expect(error.status).toBe(0)
    expect(error.message).toContain('cannot be reached')
  })

  it('lets an aborted request go as it is', async () => {
    mockFetch(new DOMException('Aborted', 'AbortError'))

    await expect(request('/projects')).rejects.toMatchObject({ name: 'AbortError' })
  })

  it('tells the application about a 401, except when signing in', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    mockFetch(jsonResponse(401, { code: 'UNAUTHENTICATED', message: 'Sign in to continue.' }))

    await request('/projects').catch(() => undefined)
    await request('/auth/login', { method: 'POST', body: {} }).catch(() => undefined)

    expect(handler).toHaveBeenCalledTimes(1)
    expect(handler.mock.calls[0][0]).toMatchObject({ status: 401, message: 'Sign in to continue.' })
  })

  it('ignores the 401 of a request that started in a session that is already over', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    const slowAnswer = deferredResponse()
    mockFetch(slowAnswer.fetch)

    const slow = request('/projects').catch(() => undefined) // starts in the first session
    startNewSession() // the user signs out and signs in again
    slowAnswer.respond(jsonResponse(401, { code: 'UNAUTHENTICATED', message: 'Your session has ended.' }))
    await slow

    expect(handler).not.toHaveBeenCalled()
  })

  it('takes back a change of session only if no other change happened since', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    const unauthorized = jsonResponse(401, { code: 'UNAUTHENTICATED', message: 'Your session has ended.' })
    const slowAnswer = deferredResponse()
    mockFetch(slowAnswer.fetch, unauthorized)
    const old = request('/projects').catch(() => undefined) // started in the session before the changes
    const undoFirst = startNewSession() // a sign out that fails later...
    startNewSession() // ...while the user signed in again
    undoFirst() // must not bring the old session back to life

    slowAnswer.respond(unauthorized)
    await old

    expect(handler).not.toHaveBeenCalled()
  })

  it('takes back a change of session that nothing else touched', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    const slowAnswer = deferredResponse()
    mockFetch(slowAnswer.fetch)
    const underWay = request('/projects').catch(() => undefined)
    const undo = startNewSession()
    undo() // the sign out failed: the same session goes on

    slowAnswer.respond(jsonResponse(401, { code: 'UNAUTHENTICATED', message: 'Your session has ended.' }))
    await underWay

    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('counts a 401 once: the other requests under way are of a session that ended', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    const unauthorized = jsonResponse(401, { code: 'UNAUTHENTICATED', message: 'Your session has ended.' })
    mockFetch(unauthorized, unauthorized)

    await Promise.all([request('/projects').catch(() => undefined), request('/dashboard').catch(() => undefined)])

    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('throws an ApiError when a success answer is not JSON', async () => {
    mockFetch(new Response('<html>proxy</html>', { status: 200 }), new Response('', { status: 200 }))

    const html = (await request('/projects').catch((e: unknown) => e)) as ApiError
    const empty = (await request('/projects').catch((e: unknown) => e)) as ApiError

    for (const error of [html, empty]) {
      expect(error).toBeInstanceOf(ApiError)
      expect(error.code).toBe(UNEXPECTED_ERROR)
      expect(error.status).toBe(200)
    }
  })

  it('does not tell the application about other errors', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    mockFetch(jsonResponse(403, { code: 'FORBIDDEN', message: 'No access.' }), jsonResponse(404, { code: 'NOT_FOUND', message: 'Not found.' }))

    await request('/projects/1').catch(() => undefined)
    await request('/projects/2').catch(() => undefined)

    expect(handler).not.toHaveBeenCalled()
  })
})
