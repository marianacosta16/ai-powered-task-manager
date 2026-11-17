import { useState } from 'react'
import type { User } from '../api/auth'
import { messageOf } from '../api/errors'
import { addAssignee, getTask, removeAssignee } from '../api/tasks'
import { Button } from '../components/Button'
import { Notice } from '../components/Notice'
import { Select } from '../components/Select'

interface AssigneeListProps {
  taskId: number
  assignees: User[]
  /** The members of the project and its manager (R21). */
  people: User[]
  /** Not in a completed project (R28). */
  canEdit: boolean
  onChange: (assignees: User[]) => void
}

/**
 * The assignees of a task (R21, R26): who is assigned, with the way to remove one, and a list to add another. Each
 * action goes to the API at once. The last assignee cannot be removed, so its button does not appear.
 */
export function AssigneeList({ taskId, assignees, people, canEdit, onChange }: AssigneeListProps) {
  const [choice, setChoice] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const available = people.filter((person) => !assignees.some((assignee) => assignee.id === person.id))

  async function run(action: () => Promise<void>) {
    setPending(true)
    setError(null)
    try {
      await action()
    } catch (cause) {
      setError(messageOf(cause))
      try {
        onChange((await getTask(taskId)).assignees) // show who is assigned now, as the answer may mean the list was old
      } catch {
        // the message above is what the user needs
      }
    } finally {
      setPending(false)
    }
  }

  return (
    <fieldset className="assignees">
      <legend className="field__label">Assignees (at least one)</legend>
      {error ? <Notice kind="error">{error}</Notice> : null}
      <ul className="chips">
        {assignees.map((person) => (
          <li key={person.id} className="chip">
            {person.name}
            {canEdit && assignees.length > 1 ? (
              <button
                type="button"
                className="chip__remove"
                aria-label={`Remove ${person.name}`}
                disabled={pending}
                onClick={() =>
                  void run(async () => {
                    await removeAssignee(taskId, person.id)
                    onChange(assignees.filter((a) => a.id !== person.id))
                  })
                }
              >
                ×
              </button>
            ) : null}
          </li>
        ))}
      </ul>
      {canEdit && available.length > 0 ? (
        <div className="actions">
          <Select
            label="Add assignee"
            value={choice}
            options={[{ value: '', label: 'Choose...' }, ...available.map((person) => ({ value: String(person.id), label: person.name }))]}
            onChange={(event) => setChoice(event.target.value)}
          />
          <Button
            variant="secondary"
            busy={pending}
            disabled={choice === ''}
            onClick={() =>
              void run(async () => {
                onChange(await addAssignee(taskId, Number(choice)))
                setChoice('')
              })
            }
          >
            Add
          </Button>
        </div>
      ) : null}
      {canEdit && assignees.length === 1 ? <p className="hint">The last assignee cannot be removed: assign someone else first.</p> : null}
    </fieldset>
  )
}
