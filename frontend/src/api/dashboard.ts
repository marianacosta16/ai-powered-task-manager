import { request } from './client'
import type { ProjectSummary } from './projects'

/** The number of main tasks in each status (R18). */
export interface StatusCounts {
  toDo: number
  inProgress: number
  inReview: number
  blocked: number
  done: number
  cancelled: number
  duplicate: number
}

/** Progress of a project; the API gives it only to its manager (P5, P16). */
export interface Progress {
  hasCountableTasks: boolean
  percent: number | null
  calculatedStatus: 'Attention' | 'Late' | null
}

export interface DashboardProject {
  project: ProjectSummary
  counts: StatusCounts
  progress?: Progress | null
}

/** getDashboard: the open projects of the user with the count of tasks by status. */
export function getDashboard(signal?: AbortSignal): Promise<DashboardProject[]> {
  return request<DashboardProject[]>('/dashboard', { signal })
}
