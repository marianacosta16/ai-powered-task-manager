import { useState } from 'react'
import type { User } from '../api/auth'
import type { Task } from '../api/tasks'
import { Button } from '../components/Button'
import { Notice } from '../components/Notice'
import { StatusBadge } from '../components/StatusBadge'
import { TaskCreator } from './TaskCreator'
import { TaskLink } from './TaskLink'

interface SubtaskSectionProps {
  task: Task
  timezone: string
  people: User[]
  /** The task is open and the project is not suspended or completed (R22, A14). */
  canAdd: boolean
  /** Reads the subtasks again; it rejects when that fails. */
  onCreated: () => Promise<void>
  onDirtyChange: (dirty: boolean) => void
}

/** The subtasks of a main task (F08, R2 to R5): each one opens in this same screen, and a new one is added here. */
export function SubtaskSection({ task, timezone, people, canAdd, onCreated, onDirtyChange }: SubtaskSectionProps) {
  const [adding, setAdding] = useState(false)
  const [message, setMessage] = useState<{ kind: 'success' | 'error'; text: string } | null>(null)
  const subtasks = task.subtasks ?? []
  return (
    <section aria-labelledby="subtasks-title">
      <h2 id="subtasks-title">Subtasks</h2>
      {message ? <Notice kind={message.kind}>{message.text}</Notice> : null}
      {subtasks.length === 0 ? (
        <p className="empty">This task has no subtasks.</p>
      ) : (
        <ul className="subtasks">
          {subtasks.map((subtask) => (
            <li key={subtask.id} className="subtask">
              <TaskLink id={subtask.id} title={subtask.title} /> <StatusBadge value={subtask.status} /> {subtask.deadline}{' '}
              <span className="subtask__who">{subtask.assignees.map((person) => person.name).join(', ')}</span>
            </li>
          ))}
        </ul>
      )}
      {canAdd && !adding ? (
        <Button
          variant="secondary"
          onClick={() => {
            setMessage(null)
            setAdding(true)
          }}
        >
          + Add subtask
        </Button>
      ) : null}
      {adding ? (
        <TaskCreator
          projectId={task.projectId}
          timezone={timezone}
          parent={{ id: task.id, deadline: task.deadline }}
          people={people}
          onDirtyChange={onDirtyChange}
          onCreated={(created) => {
            setAdding(false)
            onCreated().then(
              () => setMessage({ kind: 'success', text: `Subtask "${created.title}" created.` }),
              () =>
                setMessage({
                  kind: 'error',
                  text: `Subtask "${created.title}" was created, but the list could not be refreshed. Reload the page to see it.`,
                }),
            )
          }}
          onCancel={() => setAdding(false)}
        />
      ) : null}
    </section>
  )
}
