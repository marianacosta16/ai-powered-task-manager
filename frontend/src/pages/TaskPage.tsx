import { Link, useLocation, useParams } from 'react-router-dom'
import { getTask } from '../api/tasks'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { Spinner } from '../components/Spinner'
import { useLoad } from '../hooks/useLoad'

/** The task. For now it only shows the title: the detail, with subtasks and comments, comes with the next increment. */
export function TaskPage() {
  const id = Number(useParams().taskId)
  // The project page sends where the user came from (the Matrix or the List, with its filters)
  const from = (useLocation().state as { from?: unknown } | null)?.from
  const { state, retry } = useLoad((signal) => getTask(id, signal), [id])
  return (
    <>
      {state.status === 'loading' ? <Spinner label="Loading the task..." /> : null}
      {state.status === 'error' ? <LoadError message={state.message} onRetry={retry} /> : null}
      {state.status === 'ready' ? (
        <>
          <p>
            <Link to={typeof from === 'string' ? from : `/projects/${state.data.projectId}`}>← Project</Link>
          </p>
          <h1>{state.data.title}</h1>
          <Notice kind="info">The detail of the task comes with the next increment.</Notice>
        </>
      ) : null}
    </>
  )
}
