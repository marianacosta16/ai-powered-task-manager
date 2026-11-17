import { useState } from 'react'
import { ApiError } from '../api/errors'
import type { RecycleItem } from '../api/recycle'
import { restoreTask } from '../api/recycle'
import { listAssignableUsers } from '../api/tasks'
import { CheckboxGroup } from '../components/CheckboxGroup'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { Spinner } from '../components/Spinner'
import { useLoad } from '../hooks/useLoad'

interface ReplacementDialogProps {
  item: RecycleItem
  /** What the API said: it names who left the project (P36). */
  reason: string
  onRestored: () => void
  onCancel: () => void
}

/**
 * Restoring a task whose assignees left the project (P36, A42): the message of the API says who left, and the user
 * chooses who takes over, among the people of that project. The restore is sent again with them.
 */
export function ReplacementDialog({ item, reason, onRestored, onCancel }: ReplacementDialogProps) {
  const people = useLoad((signal) => listAssignableUsers(item.projectId, signal), [item.projectId])
  const [chosen, setChosen] = useState<string[]>([])
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function confirm() {
    setPending(true)
    setError(null)
    try {
      await restoreTask(item.id, chosen.map(Number))
      onRestored()
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Something went wrong. Try again in a moment.')
      setPending(false)
    }
  }

  return (
    <ConfirmDialog
      title={`Restore "${item.name}"`}
      confirmLabel="Restore"
      variant="primary"
      confirmDisabled={chosen.length === 0 || people.state.status !== 'ready'}
      busy={pending}
      onConfirm={() => void confirm()}
      onCancel={onCancel}
    >
      <Notice kind="info">{reason}</Notice>
      {error ? <Notice kind="error">{error}</Notice> : null}
      {people.state.status === 'loading' ? <Spinner label="Loading the people of the project..." /> : null}
      {people.state.status === 'error' ? <LoadError message={people.state.message} onRetry={people.retry} /> : null}
      {people.state.status === 'ready' ? (
        <CheckboxGroup
          legend="Who takes over"
          options={people.state.data.map((person) => ({ value: String(person.id), label: person.name }))}
          selected={chosen}
          onChange={setChosen}
        />
      ) : null}
    </ConfirmDialog>
  )
}
