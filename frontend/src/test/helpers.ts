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

/**
 * An answer of the API that comes later: `fetch` is the function to give to mockFetch, and `respond` sends the answer
 * when the test decides. It is for the requests that are still under way when something else happens.
 */
export function deferredResponse() {
  let send: (response: Response) => void = () => undefined
  const promise = new Promise<Response>((resolve) => {
    send = resolve
  })
  return { fetch: () => promise, respond: (response: Response) => send(response) }
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

type Answer = Response | (() => Promise<Response>) | Error | DOMException

/**
 * Like mockFetch, but the answers are chosen by the request ("GET /dashboard", "POST /auth/login"; the query string is
 * left out). A route with a list answers one by one, the last one for the calls after. A request nobody expected makes
 * the test fail, instead of getting the answer of another call. The calls of a route can be read with `callsTo`.
 */
export function mockApi(routes: Record<string, Answer | Answer[]>) {
  const counters = new Map<string, number>()
  const calls: Array<{ key: string; url: string; body: unknown }> = []
  const fn = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(String(input))
    const key = `${init?.method ?? 'GET'} ${url.pathname}`
    calls.push({ key, url: url.pathname + url.search, body: init?.body ? JSON.parse(String(init.body)) : undefined })
    const route = routes[key]
    if (route === undefined) {
      throw new Error(`Request not expected by the test: ${key}`)
    }
    const list = Array.isArray(route) ? route : [route]
    const count = counters.get(key) ?? 0
    counters.set(key, count + 1)
    const next = list[Math.min(count, list.length - 1)]
    if (next instanceof Error || next instanceof DOMException) {
      throw next
    }
    return typeof next === 'function' ? next() : next.clone()
  })
  vi.stubGlobal('fetch', fn)
  return { fetch: fn, calls, callsTo: (key: string) => calls.filter((call) => call.key === key) }
}

export const PROJECT_CONTRACT_DEFAULTS = {
  description: null,
  startDate: '2026-12-01',
  startTime: null,
  endDate: '2026-12-31',
  endTime: null,
  timezone: 'Europe/Lisbon',
}

/** A ProjectSummary of the tests. */
export function summary(id: number, name: string, overrides: Record<string, unknown> = {}) {
  return { id, name, phase: 'Execution', status: 'In progress', manager: MARTA, ...overrides }
}

/** A Project of the tests (the summary and the dates). */
export function fullProject(id: number, name: string, overrides: Record<string, unknown> = {}) {
  return { ...summary(id, name), ...PROJECT_CONTRACT_DEFAULTS, ...overrides }
}

export const NO_COUNTS = { toDo: 0, inProgress: 0, inReview: 0, blocked: 0, done: 0, cancelled: 0, duplicate: 0 }

/** A Task of the contract for the tests; a main task in the quadrant Do first unless said otherwise. */
export function task(id: number, title: string, overrides: Record<string, unknown> = {}) {
  return {
    id,
    version: 1,
    projectId: 1,
    parentTaskId: null,
    title,
    description: null,
    status: 'To do',
    deadline: '2099-03-01',
    urgency: 3,
    importance: 4,
    quadrant: 'Do first',
    blockReason: null,
    originalEliminated: false,
    originalInRecycleBin: false,
    assignees: [ANA],
    subtasks: [],
    ...overrides,
  }
}

/** The answer of a list in pages: the items, and the total of the server in `X-Total-Count`. */
export function pageResponse(items: unknown[], total = items.length): Response {
  return new Response(JSON.stringify(items), {
    status: 200,
    headers: { 'Content-Type': 'application/json', 'X-Total-Count': String(total) },
  })
}

/** A Comment of the contract for the tests. */
export function comment(id: number, content: string, author: { id: number; name: string; role: string } = ANA, createdAt = '2026-11-16T10:00:00Z') {
  return { id, taskId: 10, author, content, createdAt }
}

/** A Member of the contract for the tests. */
export function member(id: number, name: string, overrides: Record<string, unknown> = {}) {
  return { id, name, role: 'Team member', openTaskCount: 0, inOtherOpenProject: false, ...overrides }
}

/** A RecycleItem of the contract for the tests; a task of project 1 unless said otherwise. */
export function binItem(id: number, name: string, overrides: Record<string, unknown> = {}) {
  return {
    itemType: 'Task',
    id,
    name,
    projectId: 1,
    projectName: 'Website Redesign',
    deletedAt: '2026-11-10T10:00:00Z',
    daysLeft: 23,
    restorable: true,
    ...overrides,
  }
}
