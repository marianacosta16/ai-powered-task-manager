import { PROJECT_DESCRIPTION_MAX, PROJECT_NAME_MAX, PROJECT_NAME_MIN } from '../limits'
import {
  DEFAULT_TIMEZONE,
  type Project,
  type ProjectCreate,
  type ProjectPhase,
  type ProjectStatus,
  type ProjectUpdate,
} from '../api/projects'

/** What is typed in the form of a project; dates and times are the strings of the native fields. */
export interface FormValues {
  name: string
  description: string
  startDate: string
  startTime: string
  endDate: string
  endTime: string
  timezone: string
  phase: ProjectPhase
  status: ProjectStatus
  managerId: string
}

export type FormErrors = Partial<Record<keyof FormValues, string>>

export const EMPTY_VALUES: FormValues = {
  name: '',
  description: '',
  startDate: '',
  startTime: '',
  endDate: '',
  endTime: '',
  timezone: DEFAULT_TIMEZONE,
  phase: 'Initiation',
  status: 'Not started',
  managerId: '',
}

/** The API sends times as HH:mm:ss; the native time field wants HH:mm. */
function toFieldTime(time: string | null | undefined): string {
  return time ? time.slice(0, 5) : ''
}

export function valuesOf(project: Project): FormValues {
  return {
    name: project.name,
    description: project.description ?? '',
    startDate: project.startDate,
    startTime: toFieldTime(project.startTime),
    endDate: project.endDate,
    endTime: toFieldTime(project.endTime),
    timezone: project.timezone,
    phase: project.phase,
    status: project.status,
    managerId: String(project.manager.id),
  }
}

/**
 * Today as the native date field writes it (yyyy-mm-dd), in the given time zone. The dates of a project and the
 * deadlines of its tasks are read in the time zone of the project (P26, R13, R29), not in the one of the browser, so
 * the form must ask the same question as the server. Without a zone, or with one the browser does not know, it is the
 * browser's own day.
 */
export function today(timezone?: string): string {
  const now = new Date()
  if (timezone) {
    try {
      const parts = new Intl.DateTimeFormat('en-US', { timeZone: timezone, year: 'numeric', month: '2-digit', day: '2-digit' })
        .formatToParts(now)
        .reduce<Record<string, string>>((all, part) => ({ ...all, [part.type]: part.value }), {})
      return `${parts.year}-${parts.month}-${parts.day}` // built from the parts: it does not depend on the pattern of a locale
    } catch {
      // not a zone this browser knows: falls to the browser's day
    }
  }
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

/**
 * The checks the user can be warned about before the request (the server checks everything again, W13). On an edit
 * the start date is checked only if it was changed, as a project already running keeps its past start (R29).
 */
export function validate(values: FormValues, original: Project | null): FormErrors {
  const errors: FormErrors = {}
  const name = values.name.trim()
  if (name.length < PROJECT_NAME_MIN || name.length > PROJECT_NAME_MAX) {
    errors.name = `The name must have from ${PROJECT_NAME_MIN} to ${PROJECT_NAME_MAX} characters and not be only spaces.`
  }
  if (values.description.length > PROJECT_DESCRIPTION_MAX) {
    errors.description = `The description can have at most ${PROJECT_DESCRIPTION_MAX} characters.`
  }
  if (values.startDate === '') {
    errors.startDate = 'Choose the start date.'
  } else if ((original === null || values.startDate !== original.startDate) && values.startDate < today(values.timezone)) {
    errors.startDate = 'The start date cannot be in the past.'
  }
  if (values.endDate === '') {
    errors.endDate = 'Choose the end date.'
  } else if (values.startDate !== '' && values.endDate < values.startDate) {
    errors.endDate = 'The end date cannot be before the start date.'
  }
  return errors
}

export function toCreate(values: FormValues): ProjectCreate {
  const description = values.description.trim()
  return {
    name: values.name.trim(),
    ...(description !== '' ? { description } : {}),
    startDate: values.startDate,
    ...(values.startTime !== '' ? { startTime: values.startTime } : {}),
    endDate: values.endDate,
    ...(values.endTime !== '' ? { endTime: values.endTime } : {}),
    timezone: values.timezone,
    phase: values.phase,
    status: values.status,
  }
}

/** Only what changed (PATCH): that way an untouched start date is not validated again. */
export function toUpdate(values: FormValues, original: Project): ProjectUpdate {
  const before = valuesOf(original)
  const update: ProjectUpdate = {}
  if (values.name.trim() !== before.name) update.name = values.name.trim()
  if (values.description.trim() !== before.description.trim()) {
    update.description = values.description.trim() === '' ? null : values.description.trim()
  }
  if (values.startDate !== before.startDate) update.startDate = values.startDate
  if (values.startTime !== before.startTime) update.startTime = values.startTime === '' ? null : values.startTime
  if (values.endDate !== before.endDate) update.endDate = values.endDate
  if (values.endTime !== before.endTime) update.endTime = values.endTime === '' ? null : values.endTime
  if (values.timezone !== before.timezone) update.timezone = values.timezone
  if (values.phase !== before.phase) update.phase = values.phase
  if (values.status !== before.status) update.status = values.status
  if (values.managerId !== before.managerId) update.managerId = Number(values.managerId)
  return update
}
