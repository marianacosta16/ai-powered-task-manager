import { request } from './client'
import type { User } from './auth'

/** Member of the contract: a user with the open tasks they have in the project. */
export interface Member extends User {
  openTaskCount: number
  inOtherOpenProject: boolean
}

/** Candidate of the contract: someone who is not in the project yet. */
export interface Candidate {
  user: User
  available: boolean
  unavailableReason?: 'IN_OTHER_OPEN_PROJECT' | null
}

export function listMembers(projectId: number, signal?: AbortSignal): Promise<Member[]> {
  return request<Member[]>(`/projects/${projectId}/members`, { signal })
}

export function listCandidateMembers(projectId: number, signal?: AbortSignal): Promise<Candidate[]> {
  return request<Candidate[]>(`/projects/${projectId}/candidate-members`, { signal })
}
