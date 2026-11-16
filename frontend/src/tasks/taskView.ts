import { FINAL_STATUSES, LEVELS, QUADRANTS, STATUSES_OF_TASK, type Quadrant, type Task, type TaskStatus } from '../api/tasks'

export type View = 'matrix' | 'list'

/** What the address says about the tasks shown: the tab and the filters (they live in the URL). */
export interface TaskQuery {
  view: View
  mine: boolean
  status?: TaskStatus
  quadrant?: Quadrant
  page: number
}

/** Reads the query string; a value that is not valid is ignored, so a wrong link never breaks the screen. */
export function parseQuery(params: URLSearchParams): TaskQuery {
  const status = STATUSES_OF_TASK.find((s) => s === params.get('status'))
  const quadrant = QUADRANTS.find((q) => q === params.get('quadrant'))
  const page = Number(params.get('page'))
  return {
    view: params.get('view') === 'list' ? 'list' : 'matrix',
    mine: params.get('mine') === 'true',
    status,
    quadrant,
    page: Number.isInteger(page) && page >= 1 ? page : 1,
  }
}

/** The query string for a query; what is the default is left out. */
export function toParams(query: TaskQuery): URLSearchParams {
  const params = new URLSearchParams()
  if (query.view === 'list') params.set('view', 'list')
  if (query.mine) params.set('mine', 'true')
  if (query.view === 'list' && query.status) params.set('status', query.status)
  if (query.view === 'list' && query.quadrant) params.set('quadrant', query.quadrant)
  if (query.page > 1) params.set('page', String(query.page))
  return params
}

export function isFinal(task: Task): boolean {
  return FINAL_STATUSES.includes(task.status)
}

/** The quadrant of the API; if it is missing it is calculated: levels 3 and 4 are high (F04, R25). */
export function quadrantOf(task: Task): Quadrant {
  if (task.quadrant) return task.quadrant
  const urgent = task.urgency >= 3
  const important = task.importance >= 3
  if (urgent && important) return 'Do first'
  if (important) return 'Schedule'
  if (urgent) return 'Delegate'
  return 'Later'
}

export function levelLabel(level: number): string {
  return LEVELS.find((l) => l.value === level)?.label ?? String(level)
}

export const MATRIX_SIZE = 200
export const LIST_SIZE = 50
export type { TaskStatus }
