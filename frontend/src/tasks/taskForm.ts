import { TASK_DESCRIPTION_MAX, TASK_TITLE_MAX, TASK_TITLE_MIN } from '../limits'
import type { Level, TaskCreate } from '../api/tasks'
import { today } from '../projects/projectForm'

export interface TaskFormValues {
  title: string
  description: string
  deadline: string
  /** '' until the user chooses: a conscious choice, not a default. */
  urgency: string
  importance: string
  assigneeIds: string[]
}

export type TaskFormErrors = Partial<Record<keyof TaskFormValues, string>>

export const EMPTY_TASK: TaskFormValues = {
  title: '',
  description: '',
  deadline: '',
  urgency: '',
  importance: '',
  assigneeIds: [],
}

/** What can be warned about before the request (the server checks everything again, W13). */
export function validateTask(values: TaskFormValues): TaskFormErrors {
  const errors: TaskFormErrors = {}
  const title = values.title.trim()
  if (title.length < TASK_TITLE_MIN || title.length > TASK_TITLE_MAX) {
    errors.title = `The title must have from ${TASK_TITLE_MIN} to ${TASK_TITLE_MAX} characters and not be only spaces.`
  }
  if (values.description.length > TASK_DESCRIPTION_MAX) {
    errors.description = `The description can have at most ${TASK_DESCRIPTION_MAX} characters.`
  }
  if (values.deadline === '') {
    errors.deadline = 'Choose the deadline.'
  } else if (values.deadline < today()) {
    errors.deadline = 'The deadline cannot be in the past. Choose today or a later date.'
  }
  if (values.urgency === '') errors.urgency = 'Choose the urgency.'
  if (values.importance === '') errors.importance = 'Choose the importance.'
  if (values.assigneeIds.length === 0) errors.assigneeIds = 'Choose at least one assignee.'
  return errors
}

export function toTaskCreate(values: TaskFormValues): TaskCreate {
  const description = values.description.trim()
  return {
    title: values.title.trim(),
    ...(description !== '' ? { description } : {}),
    deadline: values.deadline,
    urgency: Number(values.urgency) as Level,
    importance: Number(values.importance) as Level,
    assigneeIds: values.assigneeIds.map(Number),
  }
}
