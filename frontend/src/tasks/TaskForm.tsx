import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ApiError } from '../api/errors'
import { LEVELS, createTask, type Task } from '../api/tasks'
import type { User } from '../api/auth'
import { Button } from '../components/Button'
import { CheckboxGroup } from '../components/CheckboxGroup'
import { Notice } from '../components/Notice'
import { Select } from '../components/Select'
import { TextArea } from '../components/TextArea'
import { TextField } from '../components/TextField'
import { TASK_DESCRIPTION_MAX, TASK_TITLE_MAX, TASK_TITLE_MIN } from '../limits'
import { today } from '../projects/projectForm'
import { EMPTY_TASK, toTaskCreate, validateTask, type TaskFormErrors, type TaskFormValues } from './taskForm'

interface TaskFormProps {
  projectId: number
  /** The time zone of the project: "today" for the deadline is its day (P26). */
  timezone: string
  /** Set to create a subtask of that main task (R2: its deadline cannot be later than the parent's). */
  parent?: { id: number; deadline: string }
  /** The members of the project and its manager (R21). */
  people: User[]
  onCreated: (task: Task) => void
  onCancel: () => void
  /** Whether there is text typed that is not saved: the page asks before leaving. */
  onDirtyChange?: (dirty: boolean) => void
}

/**
 * The form of a new task (F02, F03; wireframe 4). The task starts as "To do" (the API decides). If the request fails
 * what was typed stays, the message of the API is shown with the field it points to, and while the request is under
 * way the button waits.
 */
export function TaskForm({ projectId, timezone, parent, people, onCreated, onCancel, onDirtyChange }: TaskFormProps) {
  const [values, setValues] = useState<TaskFormValues>(EMPTY_TASK)
  const [errors, setErrors] = useState<TaskFormErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)
  const submitting = useRef(false)
  const formRef = useRef<HTMLFormElement>(null)
  const [focusToken, setFocusToken] = useState(0)

  const dirty = JSON.stringify(values) !== JSON.stringify(EMPTY_TASK)
  useEffect(() => {
    onDirtyChange?.(dirty)
    return () => onDirtyChange?.(false) // the form is gone (created or cancelled): nothing is left to lose
  }, [dirty, onDirtyChange])
  useEffect(() => {
    if (focusToken > 0) {
      formRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus()
    }
  }, [focusToken])

  function set<K extends keyof TaskFormValues>(key: K, value: TaskFormValues[K]) {
    setValues((current) => ({ ...current, [key]: value }))
    setErrors((current) => ({ ...current, [key]: undefined }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (submitting.current) {
      return
    }
    setServerError(null)
    const found = validateTask(values, timezone, parent?.deadline)
    setErrors(found)
    if (Object.keys(found).length > 0) {
      setFocusToken((token) => token + 1)
      return
    }
    submitting.current = true
    setPending(true)
    try {
      onCreated(await createTask(projectId, toTaskCreate(values, parent?.id)))
    } catch (cause) {
      if (cause instanceof ApiError) {
        setServerError(cause.message)
        setErrors({
          title: cause.issueFor('title'),
          description: cause.issueFor('description'),
          deadline: cause.issueFor('deadline'),
          assigneeIds: cause.issueFor('assigneeIds'),
        })
        setFocusToken((token) => token + 1)
      } else {
        setServerError('Something went wrong. Try again in a moment.')
      }
    } finally {
      submitting.current = false
      setPending(false)
    }
  }

  const levels = [{ value: '', label: 'Choose...' }, ...LEVELS.map((l) => ({ value: String(l.value), label: l.label }))]
  return (
    <section className="panel" aria-label={parent ? 'New subtask' : 'New task'}>
      <h2>{parent ? 'New subtask' : 'New task'}</h2>
      {serverError ? <Notice kind="error">{serverError}</Notice> : null}
      <form ref={formRef} onSubmit={handleSubmit} noValidate>
        <TextField
          label="Title *"
          name="title"
          value={values.title}
          error={errors.title}
          placeholder={`${TASK_TITLE_MIN} to ${TASK_TITLE_MAX} characters, not only spaces`}
          onChange={(event) => set('title', event.target.value)}
        />
        <TextArea
          label="Description"
          name="description"
          value={values.description}
          error={errors.description}
          placeholder={`max ${TASK_DESCRIPTION_MAX} characters`}
          onChange={(event) => set('description', event.target.value)}
        />
        <TextField
          label="Deadline *"
          name="deadline"
          type="date"
          min={today(timezone)}
          max={parent?.deadline}
          value={values.deadline}
          error={errors.deadline}
          onChange={(event) => set('deadline', event.target.value)}
        />
        <div className="field-row">
          <Select
            label="Urgency *"
            name="urgency"
            value={values.urgency}
            options={levels}
            error={errors.urgency}
            onChange={(event) => set('urgency', event.target.value)}
          />
          <Select
            label="Importance *"
            name="importance"
            value={values.importance}
            options={levels}
            error={errors.importance}
            onChange={(event) => set('importance', event.target.value)}
          />
        </div>
        <CheckboxGroup
          legend="Assignees * (project members)"
          options={people.map((person) => ({ value: String(person.id), label: person.name }))}
          selected={values.assigneeIds}
          error={errors.assigneeIds}
          onChange={(selected) => set('assigneeIds', selected)}
        />
        <div className="actions">
          <Button type="submit" busy={pending}>
            {pending ? 'Creating...' : parent ? 'Create subtask' : 'Create task'}
          </Button>
          <Button variant="secondary" disabled={pending} onClick={onCancel}>
            Cancel
          </Button>
        </div>
      </form>
    </section>
  )
}
