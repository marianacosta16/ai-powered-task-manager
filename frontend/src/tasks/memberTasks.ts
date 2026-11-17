import { FINAL_STATUSES, type Task } from '../api/tasks'

/** One open task or subtask of the member who leaves. */
export interface OpenTask {
  id: number
  title: string
  /** The title of the task it belongs to, for a subtask. */
  parentTitle?: string
  /** Who is assigned to it now. */
  assigneeIds: number[]
}

/**
 * The open tasks and subtasks of a member in a project (R17): the ones they are assigned to and that are not in a
 * final status, including those shared with other people. A subtask counts on its own, whoever its parent is assigned to.
 */
export function openTasksOf(memberId: number, tasks: Task[]): OpenTask[] {
  const found: OpenTask[] = []
  for (const task of tasks) {
    if (!FINAL_STATUSES.includes(task.status) && task.assignees.some((a) => a.id === memberId)) {
      found.push({ id: task.id, title: task.title, assigneeIds: task.assignees.map((a) => a.id) })
    }
    for (const subtask of task.subtasks ?? []) {
      if (!FINAL_STATUSES.includes(subtask.status) && subtask.assignees.some((a) => a.id === memberId)) {
        found.push({ id: subtask.id, title: subtask.title, parentTitle: task.title, assigneeIds: subtask.assignees.map((a) => a.id) })
      }
    }
  }
  return found
}

/** Who can take a task over: not the one who leaves, not someone already assigned to it, and not past the limit (R17, P24). */
export interface Substitute {
  id: number
  name: string
  /** The open tasks they have now in the project; a new member has none here. */
  openTasks: number
  isNew: boolean
}

export function substitutesFor(
  task: OpenTask,
  all: Substitute[],
  chosen: Record<number, number>,
  limit: number,
): Substitute[] {
  return all.filter((candidate) => {
    if (task.assigneeIds.includes(candidate.id)) return false
    const others = Object.entries(chosen).filter(([taskId, to]) => Number(taskId) !== task.id && to === candidate.id).length
    return candidate.openTasks + others + 1 <= limit
  })
}
