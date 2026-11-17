import { listAssignableUsers, type Task } from '../api/tasks'
import type { User } from '../api/auth'
import { LoadError } from '../components/LoadError'
import { Spinner } from '../components/Spinner'
import { useLoad } from '../hooks/useLoad'
import { TaskForm } from './TaskForm'

interface TaskCreatorProps {
  projectId: number
  timezone: string
  /** Set to create a subtask of that main task. */
  parent?: { id: number; deadline: string }
  /** The people to choose from, when the screen already has them; otherwise they are loaded. */
  people?: User[]
  onCreated: (task: Task) => void
  onCancel: () => void
  onDirtyChange?: (dirty: boolean) => void
}

/** Loads who can be assigned (listAssignableUsers) and then shows the form of a new task. */
export function TaskCreator({ projectId, timezone, parent, people, onCreated, onCancel, onDirtyChange }: TaskCreatorProps) {
  const { state, retry } = useLoad((signal) => (people ? Promise.resolve(people) : listAssignableUsers(projectId, signal)), [projectId, people])
  if (state.status === 'loading') {
    return <Spinner label="Loading the project members..." />
  }
  if (state.status === 'error') {
    return <LoadError message={state.message} onRetry={retry} />
  }
  return <TaskForm projectId={projectId} timezone={timezone} parent={parent} people={state.data} onCreated={onCreated} onCancel={onCancel} onDirtyChange={onDirtyChange} />
}
