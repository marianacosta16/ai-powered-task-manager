import { listProjectTasks } from '../api/tasks'
import { LoadError } from '../components/LoadError'
import { Select } from '../components/Select'
import { Spinner } from '../components/Spinner'
import { useLoad } from '../hooks/useLoad'
import { MATRIX_SIZE } from './taskView'

interface OriginalPickerProps {
  projectId: number
  /** The task itself: it cannot be its own original. */
  taskId: number
  value: string
  error?: string
  disabled?: boolean
  onChange: (value: string) => void
}

/**
 * The choice of the original task of a duplicate (R6): a main task of the same project, other than this one and not a
 * duplicate itself. The server checks it again.
 */
export function OriginalPicker({ projectId, taskId, value, error, disabled, onChange }: OriginalPickerProps) {
  const { state, retry } = useLoad((signal) => listProjectTasks(projectId, { size: MATRIX_SIZE }, signal), [projectId])
  if (state.status === 'loading') {
    return <Spinner label="Loading the tasks of the project..." />
  }
  if (state.status === 'error') {
    return <LoadError message={state.message} onRetry={retry} />
  }
  const candidates = state.data.items.filter((task) => task.id !== taskId && task.status !== 'Duplicate')
  const options = [{ value: '', label: 'Choose...' }, ...candidates.map((task) => ({ value: String(task.id), label: task.title }))]
  if (value !== '' && !options.some((option) => option.value === value)) {
    options.push({ value, label: `Task ${value}` }) // the original is not in the list (a duplicate, or past the page): it is kept as it is
  }
  return <Select label="Original task *" name="originalTaskId" value={value} options={options} error={error} disabled={disabled} onChange={(event) => onChange(event.target.value)} />
}
