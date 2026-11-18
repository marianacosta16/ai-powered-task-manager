import { useState } from 'react'
import { messageOf } from '../api/errors'
import { removeMember, type Candidate, type Member } from '../api/members'
import { listAllProjectTasks } from '../api/tasks'
import { Button } from '../components/Button'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { Select } from '../components/Select'
import { Spinner } from '../components/Spinner'
import { useLoad } from '../hooks/useLoad'
import { MAX_OPEN_TASKS_PER_PERSON } from '../limits'
import { openTasksOf, substitutesFor, type OpenTask, type Substitute } from '../tasks/memberTasks'

interface RemoveMemberDialogProps {
  projectId: number
  member: Member
  /** The other members of the project and the people who can be added: the substitutes come from both. */
  members: Member[]
  candidates: Candidate[]
  /** The member was removed. */
  onRemoved: () => void
  /** Reads the team and the people who can be added again: the open tasks of the substitutes may have changed. */
  onReload: () => void
  onCancel: () => void
}

/**
 * Removing a member (F01, R17; wireframe 6, element 7): every open task and subtask of the member, including the ones
 * shared with others, needs a substitute chosen by the manager. The substitutes are members already in the project, with
 * the number of their open tasks, or free people who join the project; nobody is offered who would pass 5 open tasks or
 * who is already assigned to that task. The server checks it all again, and the choices stay if it refuses.
 */
export function RemoveMemberDialog({ projectId, member, members, candidates, onRemoved, onReload, onCancel }: RemoveMemberDialogProps) {
  const tasks = useLoad((signal) => listAllProjectTasks(projectId, signal), [projectId])
  const [chosen, setChosen] = useState<Record<number, number>>({})
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const ready = tasks.state.status === 'ready'
  const open: OpenTask[] = tasks.state.status === 'ready' ? openTasksOf(member.id, tasks.state.data) : []
  const everyone: Substitute[] = [
    // The member who leaves is in `members` too, but they are assigned to each of these tasks, so they are never offered
    ...members.map((m) => ({ id: m.id, name: m.name, openTasks: m.openTaskCount, isNew: false })),
    ...candidates.filter((c) => c.available).map((c) => ({ id: c.user.id, name: c.user.name, openTasks: 0, isNew: true })),
  ]
  // Only the choices for tasks that are still open count: after a reload some may be gone
  const current = Object.fromEntries(Object.entries(chosen).filter(([id]) => open.some((task) => task.id === Number(id)))) as Record<number, number>
  const complete = ready && open.every((task) => current[task.id] !== undefined)

  async function confirm() {
    setPending(true)
    setError(null)
    try {
      await removeMember(
        projectId,
        member.id,
        open.map((task) => ({ taskId: task.id, newAssigneeId: current[task.id] })),
      )
      onRemoved()
    } catch (cause) {
      setError(messageOf(cause))
      setPending(false)
    }
  }

  return (
    <ConfirmDialog
      // neutral while the tasks are being read: the dialog does not change meaning under the user
      title={!ready ? `Remove ${member.name}` : open.length > 0 ? `Remove ${member.name} and reassign their tasks` : `Remove ${member.name}?`}
      confirmLabel={open.length > 0 ? 'Remove and reassign' : 'Remove'}
      confirmDisabled={!complete}
      busy={pending}
      onConfirm={() => void confirm()}
      onCancel={onCancel}
    >
      {tasks.state.status === 'loading' ? <Spinner label="Reading the tasks of the member..." /> : null}
      {tasks.state.status === 'error' ? <LoadError message={tasks.state.message} retryable={tasks.state.retryable} onRetry={tasks.retry} /> : null}
      {tasks.state.status === 'ready' && tasks.state.refreshError ? <Notice kind="error">{tasks.state.refreshError}</Notice> : null}
      {error ? (
        <>
          <Notice kind="error">{error}</Notice>
          <p>
            <Button
              variant="secondary"
              onClick={() => {
                setError(null)
                tasks.reload() // the tasks may have changed since the dialog was opened; the choices that still apply stay
                onReload() // and so may the open tasks of the substitutes
              }}
            >
              Reload tasks
            </Button>
          </p>
        </>
      ) : null}
      {ready && open.length === 0 ? <p>{member.name} has no open tasks. They leave the project and can join another open one.</p> : null}
      {ready && open.length > 0 ? (
        <>
          <p>
            {member.name} has {open.length} open {open.length === 1 ? 'task or subtask' : 'tasks and subtasks'}. Choose who takes
            over each one (nobody can have more than {MAX_OPEN_TASKS_PER_PERSON}).
          </p>
          <ul className="reassign">
            {open.map((task) => {
              const options = substitutesFor(task, everyone, current, MAX_OPEN_TASKS_PER_PERSON)
              return (
                <li key={task.id} className="reassign__row">
                  <Select
                    label={task.parentTitle ? `Subtask: ${task.title} (of ${task.parentTitle})` : `Task: ${task.title}`}
                    value={current[task.id] === undefined ? '' : String(current[task.id])}
                    options={[
                      { value: '', label: options.length === 0 ? 'Nobody can take it' : 'Choose...' },
                      ...options.map((o) => ({ value: String(o.id), label: o.isNew ? `${o.name} (new member)` : `${o.name} (${o.openTasks} open)` })),
                    ]}
                    onChange={(event) => {
                      const value = event.target.value
                      setChosen((current) => {
                        const next = { ...current }
                        if (value === '') delete next[task.id]
                        else next[task.id] = Number(value)
                        return next
                      })
                    }}
                  />
                </li>
              )
            })}
          </ul>
        </>
      ) : null}
    </ConfirmDialog>
  )
}
