import { listAssignableUsers, type Task } from '../api/tasks'
import { LoadError } from '../components/LoadError'
import { Spinner } from '../components/Spinner'
import { useLoad } from '../hooks/useLoad'
import { TaskForm } from './TaskForm'

interface TaskCreatorProps {
  projectId: number
  onCreated: (task: Task) => void
  onCancel: () => void
}

/** Loads who can be assigned (listAssignableUsers) and then shows the form of a new task. */
export function TaskCreator({ projectId, onCreated, onCancel }: TaskCreatorProps) {
  const { state, retry } = useLoad((signal) => listAssignableUsers(projectId, signal), [projectId])
  if (state.status === 'loading') {
    return <Spinner label="Loading the project members..." />
  }
  if (state.status === 'error') {
    return <LoadError message={state.message} onRetry={retry} />
  }
  return <TaskForm projectId={projectId} people={state.data} onCreated={onCreated} onCancel={onCancel} />
}
