import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/errors'
import { LEVELS, TASK_CHANGED, getTask, updateTask, type Task } from '../api/tasks'
import { Button } from '../components/Button'
import { Notice } from '../components/Notice'
import { Select } from '../components/Select'
import { StatusBadge } from '../components/StatusBadge'
import { TextArea } from '../components/TextArea'
import { TextField } from '../components/TextField'
import { BLOCK_REASON_MAX, TASK_DESCRIPTION_MAX } from '../limits'
import { today } from '../projects/projectForm'
import { OriginalPicker } from './OriginalPicker'
import {
  changes,
  isDirty,
  mergeAfterConflict,
  statusChoices,
  validateEdit,
  valuesOf,
  type EditErrors,
  type EditValues,
} from './taskEdit'
import { quadrantOf } from './taskView'

interface TaskEditFormProps {
  task: Task
  /** The time zone of the project: the day for a new deadline (P26). */
  timezone: string
  isManager: boolean
  /** Nothing can be changed (the project is completed, R28). */
  readOnly: boolean
  /** The task as the server has it after a save or a reload. */
  onTaskChange: (task: Task) => void
  /** Whether there are changes not saved: the page asks before leaving. */
  onDirtyChange: (dirty: boolean) => void
}

/**
 * The data of a task (F02, F03; wireframe 5). One Save changes for all the fields, with the version that was read
 * (R30). If someone else saved meanwhile (409) the message of the API comes with Reload latest, which keeps what was
 * typed and takes the rest from the server. The task text is shown as text, never as HTML (NF09).
 */
export function TaskEditForm({ task, timezone, isManager, readOnly, onTaskChange, onDirtyChange }: TaskEditFormProps) {
  const [base, setBase] = useState(task)
  const [values, setValues] = useState<EditValues>(() => valuesOf(task))
  const [errors, setErrors] = useState<EditErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [conflict, setConflict] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [pending, setPending] = useState(false)
  const submitting = useRef(false)
  const formRef = useRef<HTMLFormElement>(null)
  const [focusToken, setFocusToken] = useState(0)

  const dirty = !readOnly && isDirty(values, base)
  useEffect(() => {
    onDirtyChange(dirty)
  }, [dirty, onDirtyChange])
  useEffect(() => {
    if (focusToken > 0) {
      formRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus()
    }
  }, [focusToken])

  function set<K extends keyof EditValues>(key: K, value: EditValues[K]) {
    setValues((current) => ({ ...current, [key]: value }))
    setErrors((current) => ({ ...current, [key]: undefined }))
  }

  function adopt(next: Task, nextValues: EditValues) {
    setBase(next)
    setValues(nextValues)
    onTaskChange(next)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (submitting.current || readOnly) {
      return
    }
    setServerError(null)
    setNotice(null)
    const found = validateEdit(values, base, timezone)
    setErrors(found)
    if (Object.keys(found).length > 0) {
      setFocusToken((token) => token + 1)
      return
    }
    submitting.current = true
    setPending(true)
    try {
      const saved = await updateTask(base.id, { version: base.version, ...changes(values, base) })
      adopt(saved, valuesOf(saved))
      setConflict(false)
      setNotice('Changes saved.')
    } catch (cause) {
      if (cause instanceof ApiError) {
        setServerError(cause.message)
        setConflict(cause.code === TASK_CHANGED)
        setErrors({
          title: cause.issueFor('title'),
          description: cause.issueFor('description'),
          deadline: cause.issueFor('deadline'),
          blockReason: cause.issueFor('blockReason'),
          originalTaskId: cause.issueFor('originalTaskId'),
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

  async function reloadLatest() {
    setPending(true)
    try {
      const fresh = await getTask(base.id)
      const merged = mergeAfterConflict(values, base, fresh)
      adopt(fresh, merged.values)
      setConflict(false)
      setServerError(null)
      setNotice(
        merged.alsoChanged.length > 0
          ? `The task was reloaded. What you typed was kept. Someone else also changed: ${merged.alsoChanged.join(', ')}. Saving replaces their value.`
          : 'The task was reloaded. What you typed was kept and the rest is up to date.',
      )
    } catch (cause) {
      setServerError(cause instanceof ApiError ? cause.message : 'Something went wrong. Try again in a moment.')
    } finally {
      setPending(false)
    }
  }

  const levels = LEVELS.map((level) => ({ value: String(level.value), label: level.label }))
  const choices = statusChoices(base.status, isManager)
  const statusOptions = choices.includes(values.status) ? choices : [values.status, ...choices]
  const quadrant = quadrantOf({ ...base, urgency: Number(values.urgency) as Task['urgency'], importance: Number(values.importance) as Task['importance'], quadrant: undefined })

  return (
    <form ref={formRef} onSubmit={handleSubmit} noValidate aria-label="Task">
      {serverError ? <Notice kind="error">{serverError}</Notice> : null}
      {conflict ? (
        <p>
          <Button variant="secondary" busy={pending} onClick={() => void reloadLatest()}>
            Reload latest
          </Button>
        </p>
      ) : null}
      {notice ? <Notice kind="success">{notice}</Notice> : null}
      <TextField
        label="Task title *"
        name="title"
        value={values.title}
        error={errors.title}
        disabled={readOnly}
        onChange={(event) => set('title', event.target.value)}
      />
      <p>
        Quadrant: <strong>{quadrant}</strong>
      </p>
      <TextArea
        label="Description"
        name="description"
        value={values.description}
        error={errors.description}
        disabled={readOnly}
        placeholder={`max ${TASK_DESCRIPTION_MAX} characters`}
        onChange={(event) => set('description', event.target.value)}
      />
      <div className="field-row">
        <Select
          label="Status"
          name="status"
          value={values.status}
          options={statusOptions.map((status) => ({ value: status, label: status }))}
          disabled={readOnly}
          onChange={(event) => set('status', event.target.value as EditValues['status'])}
        />
        <TextField
          label="Deadline *"
          name="deadline"
          type="date"
          min={values.deadline !== base.deadline ? today(timezone) : undefined}
          value={values.deadline}
          error={errors.deadline}
          disabled={readOnly}
          onChange={(event) => set('deadline', event.target.value)}
        />
      </div>
      <div className="field-row">
        <Select label="Urgency" name="urgency" value={values.urgency} options={levels} disabled={readOnly} onChange={(event) => set('urgency', event.target.value)} />
        <Select
          label="Importance"
          name="importance"
          value={values.importance}
          options={levels}
          disabled={readOnly}
          onChange={(event) => set('importance', event.target.value)}
        />
      </div>
      {values.status === 'Blocked' ? (
        <TextField
          label="Block reason *"
          name="blockReason"
          value={values.blockReason}
          error={errors.blockReason}
          disabled={readOnly}
          placeholder={`max ${BLOCK_REASON_MAX} characters`}
          onChange={(event) => set('blockReason', event.target.value)}
        />
      ) : null}
      {values.status === 'Duplicate' ? (
        <>
          <OriginalPicker
            projectId={base.projectId}
            taskId={base.id}
            value={values.originalTaskId}
            error={errors.originalTaskId}
            disabled={readOnly}
            onChange={(value) => set('originalTaskId', value)}
          />
          {base.originalEliminated ? <Notice kind="info">The original task was eliminated.</Notice> : null}
          {base.originalInRecycleBin ? <Notice kind="info">The original task is in the recycle bin.</Notice> : null}
          {base.status === 'Duplicate' && base.originalTaskId && !base.originalInRecycleBin ? (
            <p>
              <Link to={`/tasks/${base.originalTaskId}`}>Open the original task</Link>
            </p>
          ) : null}
        </>
      ) : null}
      {readOnly ? (
        <Notice kind="info">
          The project is completed, so the task can only be read. <StatusBadge value={base.status} />
        </Notice>
      ) : (
        <div className="actions">
          <Button type="submit" busy={pending} disabled={!dirty}>
            {pending ? 'Saving...' : 'Save changes'}
          </Button>
        </div>
      )}
    </form>
  )
}
