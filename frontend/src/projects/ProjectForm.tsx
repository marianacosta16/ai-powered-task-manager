import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ApiError } from '../api/errors'
import {
  PHASES,
  STATUSES,
  createProject,
  updateProject,
  type Project,
} from '../api/projects'
import type { User } from '../api/auth'
import { Button } from '../components/Button'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { Notice } from '../components/Notice'
import { Select } from '../components/Select'
import { TextArea } from '../components/TextArea'
import { TextField } from '../components/TextField'
import { PROJECT_DESCRIPTION_MAX, PROJECT_NAME_MAX } from '../limits'
import {
  EMPTY_VALUES,
  toCreate,
  toUpdate,
  today,
  validate,
  valuesOf,
  type FormErrors,
  type FormValues,
} from './projectForm'

const TIMEZONES = ['Europe/Lisbon', 'Europe/London', 'Europe/Madrid', 'Europe/Paris', 'Europe/Berlin', 'America/Sao_Paulo', 'UTC']

interface ProjectFormProps {
  /** The project being edited, or null to create one. */
  project: Project | null
  /** Who can receive the project; only used when editing (hand over, A12). */
  managers: User[]
  onSaved: (project: Project) => void
  onCancel: () => void
}

/**
 * The form to create and to edit a project (F01; wireframe 3). The manager field exists only when editing, because
 * the creator is the manager (A44). If the request fails what was typed stays, and while it is under way the buttons
 * wait. Choosing the status Completed asks for confirmation first, as a completed project is frozen (R28).
 */
export function ProjectForm({ project, managers, onSaved, onCancel }: ProjectFormProps) {
  const [values, setValues] = useState<FormValues>(project ? valuesOf(project) : EMPTY_VALUES)
  const [errors, setErrors] = useState<FormErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const submitting = useRef(false)
  const formRef = useRef<HTMLFormElement>(null)
  const [focusToken, setFocusToken] = useState(0)

  // After a refused submit the focus goes to the first field with an error, as in the sign in form
  useEffect(() => {
    if (focusToken > 0) {
      formRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus()
    }
  }, [focusToken])

  function set<K extends keyof FormValues>(key: K, value: FormValues[K]) {
    setValues((current) => ({ ...current, [key]: value }))
    setErrors((current) => ({ ...current, [key]: undefined }))
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setServerError(null)
    const found = validate(values, project)
    setErrors(found)
    if (Object.keys(found).length > 0) {
      setFocusToken((token) => token + 1)
      return
    }
    const becomesCompleted = values.status === 'Completed' && project?.status !== 'Completed'
    if (becomesCompleted) {
      setConfirming(true)
      return
    }
    void save()
  }

  async function save() {
    if (submitting.current) {
      return
    }
    submitting.current = true
    setPending(true)
    try {
      let saved: Project
      if (project === null) {
        saved = await createProject(toCreate(values))
      } else {
        const update = toUpdate(values, project)
        if (Object.keys(update).length === 0) {
          onCancel() // nothing changed: nothing to send
          return
        }
        saved = await updateProject(project.id, update)
      }
      onSaved(saved)
    } catch (cause) {
      setConfirming(false)
      if (cause instanceof ApiError) {
        setServerError(cause.message)
        setErrors({
          name: cause.issueFor('name'),
          description: cause.issueFor('description'),
          startDate: cause.issueFor('startDate'),
          endDate: cause.issueFor('endDate'),
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

  const timezones = TIMEZONES.includes(values.timezone) ? TIMEZONES : [values.timezone, ...TIMEZONES]
  const managerOptions = managers.map((manager) => ({ value: String(manager.id), label: manager.name }))

  return (
    <section className="panel" aria-label={project ? 'Edit project' : 'New project'}>
      <h2>{project ? 'Edit project' : 'New project'}</h2>
      {serverError ? <Notice kind="error">{serverError}</Notice> : null}
      <form ref={formRef} onSubmit={handleSubmit} noValidate>
        <TextField
          label="Name *"
          name="name"
          value={values.name}
          error={errors.name}
          placeholder={`3 to ${PROJECT_NAME_MAX} characters, not only spaces`}
          onChange={(event) => set('name', event.target.value)}
        />
        <TextArea
          label="Description"
          name="description"
          value={values.description}
          error={errors.description}
          placeholder={`max ${PROJECT_DESCRIPTION_MAX} characters`}
          onChange={(event) => set('description', event.target.value)}
        />
        <div className="field-row">
          <TextField
            label="Start date *"
            name="startDate"
            type="date"
            value={values.startDate}
            error={errors.startDate}
            min={project === null ? today() : undefined}
            onChange={(event) => set('startDate', event.target.value)}
          />
          <TextField
            label="Start time"
            name="startTime"
            type="time"
            value={values.startTime}
            onChange={(event) => set('startTime', event.target.value)}
          />
        </div>
        <div className="field-row">
          <TextField
            label="End date *"
            name="endDate"
            type="date"
            value={values.endDate}
            error={errors.endDate}
            min={values.startDate || undefined}
            onChange={(event) => set('endDate', event.target.value)}
          />
          <TextField
            label="End time"
            name="endTime"
            type="time"
            value={values.endTime}
            onChange={(event) => set('endTime', event.target.value)}
          />
        </div>
        <Select
          label="Time zone"
          name="timezone"
          value={values.timezone}
          options={timezones.map((zone) => ({ value: zone, label: zone }))}
          onChange={(event) => set('timezone', event.target.value)}
        />
        <div className="field-row">
          <Select
            label="Phase"
            name="phase"
            value={values.phase}
            options={PHASES.map((phase) => ({ value: phase, label: phase }))}
            onChange={(event) => set('phase', event.target.value as FormValues['phase'])}
          />
          <Select
            label="Status"
            name="status"
            value={values.status}
            options={STATUSES.map((status) => ({ value: status, label: status }))}
            onChange={(event) => set('status', event.target.value as FormValues['status'])}
          />
        </div>
        {project !== null ? (
          <Select
            label="Manager *"
            name="managerId"
            value={values.managerId}
            options={managerOptions}
            onChange={(event) => set('managerId', event.target.value)}
          />
        ) : null}
        <div className="actions">
          <Button type="submit" busy={pending}>
            {pending ? 'Saving...' : 'Save'}
          </Button>
          <Button variant="secondary" disabled={pending} onClick={onCancel}>
            Cancel
          </Button>
        </div>
      </form>
      {confirming ? (
        <ConfirmDialog
          title="Mark the project as completed?"
          confirmLabel="Complete project"
          variant="primary"
          busy={pending}
          onConfirm={() => void save()}
          onCancel={() => setConfirming(false)}
        >
          <p>A completed project cannot be edited, reopened or deleted. Everything stays exactly as it is now.</p>
        </ConfirmDialog>
      ) : null}
    </section>
  )
}
