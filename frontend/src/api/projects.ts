import { request } from './client'
import type { User } from './auth'

export const PHASES = ['Initiation', 'Planning', 'Execution', 'Monitoring', 'Evaluation'] as const
export type ProjectPhase = (typeof PHASES)[number]

/** The states the manager sets; Attention and Late are calculated and never chosen (R12). */
export const STATUSES = ['Not started', 'In progress', 'Suspended', 'Completed'] as const
export type ProjectStatus = (typeof STATUSES)[number]

export const DEFAULT_TIMEZONE = 'Europe/Lisbon'

/** ProjectSummary of the contract. */
export interface ProjectSummary {
  id: number
  name: string
  phase: ProjectPhase
  status: ProjectStatus
  manager: User
}

/** Project of the contract. */
export interface Project extends ProjectSummary {
  description?: string | null
  startDate: string
  startTime?: string | null
  endDate: string
  endTime?: string | null
  timezone: string
}

/** ProjectCreate: the manager is not sent, it is the creator (A44). */
export interface ProjectCreate {
  name: string
  description?: string
  startDate: string
  startTime?: string
  endDate: string
  endTime?: string
  timezone?: string
  phase: ProjectPhase
  status: ProjectStatus
}

/** ProjectUpdate: only the fields to change; null clears the description and the times. */
export interface ProjectUpdate {
  name?: string
  description?: string | null
  startDate?: string
  startTime?: string | null
  endDate?: string
  endTime?: string | null
  timezone?: string
  phase?: ProjectPhase
  status?: ProjectStatus
  managerId?: number
}

/** listProjects: the accessible projects, filtered by the text of the name when there is one (P29). */
export function listProjects(search: string, signal?: AbortSignal): Promise<ProjectSummary[]> {
  const query = search === '' ? '' : `?search=${encodeURIComponent(search)}`
  return request<ProjectSummary[]>(`/projects${query}`, { signal })
}

export function getProject(id: number, signal?: AbortSignal): Promise<Project> {
  return request<Project>(`/projects/${id}`, { signal })
}

export function createProject(body: ProjectCreate): Promise<Project> {
  return request<Project>('/projects', { method: 'POST', body })
}

export function updateProject(id: number, body: ProjectUpdate): Promise<Project> {
  return request<Project>(`/projects/${id}`, { method: 'PATCH', body })
}

export function deleteProject(id: number): Promise<void> {
  return request<void>(`/projects/${id}`, { method: 'DELETE' })
}
