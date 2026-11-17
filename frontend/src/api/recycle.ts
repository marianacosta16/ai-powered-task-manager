import { request } from './client'

export type RecycleItemType = 'Project' | 'Task' | 'Subtask'
export type NotRestorableReason = 'PROJECT_IN_RECYCLE_BIN' | 'PARENT_IN_RECYCLE_BIN' | 'PROJECT_COMPLETED'

/** RecycleItem of the contract: something that was deleted and is kept for 30 days (R9, R20). */
export interface RecycleItem {
  itemType: RecycleItemType
  id: number
  name: string
  /** The project of the item; for a project, the project itself. */
  projectId: number
  projectName?: string | null
  deletedAt: string
  daysLeft: number
  /** For a project, the number of tasks that were deleted with it. */
  includedTasks?: number | null
  restorable: boolean
  notRestorableReason?: NotRestorableReason | null
}

export function listRecycleBin(signal?: AbortSignal): Promise<RecycleItem[]> {
  return request<RecycleItem[]>('/recycle-bin', { signal })
}

export function restoreProject(projectId: number): Promise<void> {
  return request<void>(`/recycle-bin/projects/${projectId}/restore`, { method: 'POST' })
}

/** restoreTask: with `assigneeIds` it names who takes over from the people who left the project (A42). */
export function restoreTask(taskId: number, assigneeIds?: number[]): Promise<void> {
  return request<void>(`/recycle-bin/tasks/${taskId}/restore`, {
    method: 'POST',
    ...(assigneeIds ? { body: { assigneeIds } } : {}),
  })
}

/** The code of the refusal that asks who takes over from the people who left (P36). */
export const ASSIGNEE_REQUIRED = 'ASSIGNEE_REQUIRED'
