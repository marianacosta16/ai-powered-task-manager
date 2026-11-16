import { Link, useParams } from 'react-router-dom'
import { getProject } from '../api/projects'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { Spinner } from '../components/Spinner'
import { StatusBadge } from '../components/StatusBadge'
import { useLoad } from '../hooks/useLoad'

/** The data of a project. The tasks of the project (list and matrix) come with the next increments. */
export function ProjectDetailPage() {
  const { projectId } = useParams()
  const id = Number(projectId)
  const { state, retry } = useLoad((signal) => getProject(id, signal), [id])

  return (
    <>
      <p>
        <Link to="/projects">← Projects</Link>
      </p>
      {state.status === 'loading' ? <Spinner label="Loading the project..." /> : null}
      {state.status === 'error' ? <LoadError message={state.message} onRetry={retry} /> : null}
      {state.status === 'ready' ? (
        <>
          <h1>{state.data.name}</h1>
          <p>
            {state.data.phase} · <StatusBadge value={state.data.status} /> · Manager: {state.data.manager.name}
          </p>
          <p>
            {state.data.startDate}
            {state.data.startTime ? ` ${state.data.startTime}` : ''} to {state.data.endDate}
            {state.data.endTime ? ` ${state.data.endTime}` : ''} ({state.data.timezone})
          </p>
          {state.data.description ? <p className="preline">{state.data.description}</p> : null}
          <Notice kind="info">The tasks of the project come with the next increments.</Notice>
        </>
      ) : null}
    </>
  )
}
