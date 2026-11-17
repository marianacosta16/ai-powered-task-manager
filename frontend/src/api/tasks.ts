import { request, requestPage, type Page } from './client'
import type { User } from './auth'

export const STATUSES_OF_TASK = ['To do', 'In progress', 'In review', 'Blocked', 'Done', 'Cancelled', 'Duplicate'] as const
export type TaskStatus = (typeof STATUSES_OF_TASK)[number]
/** The final statuses: the Matrix hides them (P10) and the list shows them after the open ones. */
export const FINAL_STATUSES: readonly TaskStatus[] = ['Done', 'Cancelled', 'Duplicate']

/** Matrix quadrants, in the order of the screen: top left, top right, bottom left, bottom right (F04). */
export const QUADRANTS = ['Do first', 'Schedule', 'Delegate', 'Later'] as const
export type Quadrant = (typeof QUADRANTS)[number]

/** Urgency or importance: 1 = Low, 2 = Medium, 3 = High, 4 = Critical (F03). */
export type Level = 1 | 2 | 3 | 4
export const LEVELS: ReadonlyArray<{ value: Level; label: string }> = [
  { value: 1, label: '1 - Low' },
  { value: 2, label: '2 - Medium' },
  { value: 3, label: '3 - High' },
  { value: 4, label: '4 - Critical' },
]

/** Task of the contract: a main task or a subtask. */
export interface Task {
  id: number
  version: number
  projectId: number
  parentTaskId: number | null
  title: string
  description?: string | null
  status: TaskStatus
  deadline: string
  urgency: Level
  importance: Level
  quadrant?: Quadrant
  blockReason?: string | null
  originalTaskId?: number | null
  originalEliminated?: boolean
  originalInRecycleBin?: boolean
  assignees: User[]
  subtasks?: Task[]
}

export interface TaskCreate {
  /** Set to create a subtask of that main task. */
  parentTaskId?: number
  title: string
  description?: string
  deadline: string
  urgency: Level
  importance: Level
  assigneeIds: number[]
}

export interface TaskFilters {
  mine?: boolean
  status?: TaskStatus
  quadrant?: Quadrant
  page?: number
  size?: number
}

/** listProjectTasks: one page of the main tasks, with the total of the server (A30, A31). */
export function listProjectTasks(projectId: number, filters: TaskFilters, signal?: AbortSignal): Promise<Page<Task>> {
  const query = new URLSearchParams()
  if (filters.mine) query.set('mine', 'true')
  if (filters.status) query.set('status', filters.status)
  if (filters.quadrant) query.set('quadrant', filters.quadrant)
  if (filters.page) query.set('page', String(filters.page))
  if (filters.size) query.set('size', String(filters.size))
  const text = query.toString()
  return requestPage<Task>(`/projects/${projectId}/tasks${text === '' ? '' : `?${text}`}`, { signal })
}

export function createTask(projectId: number, body: TaskCreate): Promise<Task> {
  return request<Task>(`/projects/${projectId}/tasks`, { method: 'POST', body })
}

/** listAssignableUsers: the members of the project and its manager (R21). */
export function listAssignableUsers(projectId: number, signal?: AbortSignal): Promise<User[]> {
  return request<User[]>(`/projects/${projectId}/assignable-users`, { signal })
}

export function getTask(id: number, signal?: AbortSignal): Promise<Task> {
  return request<Task>(`/tasks/${id}`, { signal })
}

/** TaskUpdate: the `version` read and only the fields that change; null clears the description. */
export interface TaskUpdate {
  version: number
  title?: string
  description?: string | null
  status?: TaskStatus
  deadline?: string
  urgency?: Level
  importance?: Level
  blockReason?: string | null
  originalTaskId?: number | null
}

export function updateTask(id: number, body: TaskUpdate): Promise<Task> {
  return request<Task>(`/tasks/${id}`, { method: 'PATCH', body })
}

export function deleteTask(id: number): Promise<void> {
  return request<void>(`/tasks/${id}`, { method: 'DELETE' })
}

/** addAssignee: gives back the assignees after the change. */
export function addAssignee(taskId: number, userId: number): Promise<User[]> {
  return request<User[]>(`/tasks/${taskId}/assignees`, { method: 'POST', body: { userId } })
}

export function removeAssignee(taskId: number, userId: number): Promise<void> {
  return request<void>(`/tasks/${taskId}/assignees/${userId}`, { method: 'DELETE' })
}

/** The code of the 409 that says the task was changed by someone else after it was read (R30). */
export const TASK_CHANGED = 'TASK_CHANGED'

/** Every main task of a project: the pages of the server one after the other, until the total is reached. */
export async function listAllProjectTasks(projectId: number, signal?: AbortSignal): Promise<Task[]> {
  const size = 200
  const all: Task[] = []
  for (let page = 1; ; page += 1) {
    const result = await listProjectTasks(projectId, { page, size }, signal)
    all.push(...result.items)
    if (result.items.length === 0 || all.length >= result.total) {
      return all
    }
  }
}
