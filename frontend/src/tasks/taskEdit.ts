import { BLOCK_REASON_MAX, TASK_DESCRIPTION_MAX, TASK_TITLE_MAX, TASK_TITLE_MIN } from '../limits'
import { FINAL_STATUSES, STATUSES_OF_TASK, type Level, type Task, type TaskStatus, type TaskUpdate } from '../api/tasks'
import { today } from '../projects/projectForm'

/** What is typed in the form of a task; levels and the original task are the strings of the lists. */
export interface EditValues {
  title: string
  description: string
  deadline: string
  status: TaskStatus
  urgency: string
  importance: string
  blockReason: string
  originalTaskId: string
}

export type EditKey = keyof EditValues
export type EditErrors = Partial<Record<EditKey, string>>

/** The fields in the order of the form, with the name the user knows them by. */
export const EDIT_FIELDS: ReadonlyArray<{ key: EditKey; label: string }> = [
  { key: 'title', label: 'Task title' },
  { key: 'description', label: 'Description' },
  { key: 'status', label: 'Status' },
  { key: 'deadline', label: 'Deadline' },
  { key: 'urgency', label: 'Urgency' },
  { key: 'importance', label: 'Importance' },
  { key: 'blockReason', label: 'Block reason' },
  { key: 'originalTaskId', label: 'Original task' },
]

export function valuesOf(task: Task): EditValues {
  return {
    title: task.title,
    description: task.description ?? '',
    deadline: task.deadline,
    status: task.status,
    urgency: String(task.urgency),
    importance: String(task.importance),
    blockReason: task.blockReason ?? '',
    originalTaskId: task.originalTaskId ? String(task.originalTaskId) : '',
  }
}

/** The statuses a person can choose: a member cannot reopen a closed task (P8), so on one they only see the final ones. */
export function statusChoices(current: TaskStatus, isManager: boolean): readonly TaskStatus[] {
  return FINAL_STATUSES.includes(current) && !isManager ? FINAL_STATUSES : STATUSES_OF_TASK
}

/** The checks the user can be warned about before the request (the server checks everything again, W13). */
export function validateEdit(values: EditValues, base: Task, timezone: string): EditErrors {
  const errors: EditErrors = {}
  const title = values.title.trim()
  if (title.length < TASK_TITLE_MIN || title.length > TASK_TITLE_MAX) {
    errors.title = `The title must have from ${TASK_TITLE_MIN} to ${TASK_TITLE_MAX} characters and not be only spaces.`
  }
  if (values.description.length > TASK_DESCRIPTION_MAX) {
    errors.description = `The description can have at most ${TASK_DESCRIPTION_MAX} characters.`
  }
  if (values.deadline === '') {
    errors.deadline = 'Choose the deadline.'
  } else if (values.deadline !== base.deadline && values.deadline < today(timezone)) {
    // a deadline already in the past is kept as it is; only a new one is checked (A18, R13)
    errors.deadline = 'The deadline cannot be in the past. Choose today or a later date.'
  }
  if (values.status === 'Blocked') {
    const reason = values.blockReason.trim()
    if (reason === '') errors.blockReason = 'Say why the task is blocked.'
    else if (reason.length > BLOCK_REASON_MAX) errors.blockReason = `The reason can have at most ${BLOCK_REASON_MAX} characters.`
  }
  if (values.status === 'Duplicate' && values.originalTaskId === '') {
    errors.originalTaskId = 'Choose the original task.'
  }
  return errors
}

/** Only what changed (PATCH), without the version. Block reason and original task go only with the status that uses them (A17). */
export function changes(values: EditValues, base: Task): Omit<TaskUpdate, 'version'> {
  const before = valuesOf(base)
  const update: Omit<TaskUpdate, 'version'> = {}
  if (values.title.trim() !== before.title) update.title = values.title.trim()
  if (values.description.trim() !== before.description.trim()) {
    update.description = values.description.trim() === '' ? null : values.description.trim()
  }
  if (values.deadline !== before.deadline) update.deadline = values.deadline
  if (values.status !== before.status) update.status = values.status
  if (values.urgency !== before.urgency) update.urgency = Number(values.urgency) as Level
  if (values.importance !== before.importance) update.importance = Number(values.importance) as Level
  if (values.status === 'Blocked' && (values.status !== before.status || values.blockReason.trim() !== before.blockReason.trim())) {
    update.blockReason = values.blockReason.trim()
  }
  if (values.status === 'Duplicate' && (values.status !== before.status || values.originalTaskId !== before.originalTaskId)) {
    update.originalTaskId = Number(values.originalTaskId)
  }
  return update
}

export function isDirty(values: EditValues, base: Task): boolean {
  return Object.keys(changes(values, base)).length > 0
}

/**
 * After a 409 (R30): the task is read again and the form goes on from the new one. The fields the user did not touch
 * take the new values; the fields the user changed keep what was typed. It says which of those the other person also
 * changed, so the user knows the typed value will replace theirs.
 */
export function mergeAfterConflict(values: EditValues, base: Task, fresh: Task): { values: EditValues; alsoChanged: string[] } {
  const before = valuesOf(base)
  const latest = valuesOf(fresh)
  const merged = { ...latest }
  const alsoChanged: string[] = []
  for (const { key, label } of EDIT_FIELDS) {
    const mine = values[key] !== before[key]
    if (!mine) continue
    ;(merged as Record<EditKey, string>)[key] = values[key]
    if (latest[key] !== before[key] && latest[key] !== values[key]) alsoChanged.push(label)
  }
  return { values: merged, alsoChanged }
}
