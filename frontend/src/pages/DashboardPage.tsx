import { Link } from 'react-router-dom'
import { getDashboard, type DashboardProject, type StatusCounts } from '../api/dashboard'
import { useAuth } from '../auth/useAuth'
import { EmptyState } from '../components/EmptyState'
import { LoadError } from '../components/LoadError'
import { ProgressBar } from '../components/ProgressBar'
import { Spinner } from '../components/Spinner'
import { StatusBadge } from '../components/StatusBadge'
import { useLoad } from '../hooks/useLoad'

const COUNT_LABELS: Array<[keyof StatusCounts, string]> = [
  ['toDo', 'To do'],
  ['inProgress', 'In progress'],
  ['inReview', 'In review'],
  ['blocked', 'Blocked'],
  ['done', 'Done'],
  ['cancelled', 'Cancelled'],
  ['duplicate', 'Duplicate'],
]

/**
 * The home screen (F07, US13; wireframe 2): the open projects of the user with the number of tasks in each status
 * and, for the manager of the project, the progress and the calculated status (Attention or Late).
 */
export function DashboardPage() {
  const { state: auth } = useAuth()
  const { state, retry } = useLoad((signal) => getDashboard(signal), [])
  if (auth.status !== 'signedIn') {
    return null
  }

  return (
    <>
      <div className="toolbar">
        <h1>Dashboard</h1>
        <Link className="button button--secondary" to="/recycle-bin">
          Recycle bin
        </Link>
      </div>
      {state.status === 'loading' ? <Spinner label="Loading your projects..." /> : null}
      {state.status === 'error' ? <LoadError message={state.message} retryable={state.retryable} onRetry={retry} /> : null}
      {state.status === 'ready' ? (
        state.data.length === 0 ? (
          <EmptyState>
            You have no open projects.
            {auth.user.role === 'Project manager' ? ' Create one in Projects.' : ' A manager adds you to a project.'}
          </EmptyState>
        ) : (
          <ul className="cards">
            {state.data.map((item) => (
              <ProjectCard key={item.project.id} item={item} />
            ))}
          </ul>
        )
      ) : null}
      <p className="hint">Completed projects are not shown here: find them in Projects.</p>
    </>
  )
}

function ProjectCard({ item }: { item: DashboardProject }) {
  const { project, counts, progress } = item
  return (
    <li className="card">
      <h2 className="card__title">
        <Link to={`/projects/${project.id}`}>{project.name}</Link>
      </h2>
      <p className="card__meta">
        {project.phase} · <StatusBadge value={project.status} />
        {progress?.calculatedStatus ? <StatusBadge value={progress.calculatedStatus} /> : null} · Manager:{' '}
        {project.manager.name}
      </p>
      {progress ? (
        progress.hasCountableTasks && progress.percent !== null ? (
          <ProgressBar percent={progress.percent} />
        ) : (
          <p className="card__meta">No tasks</p>
        )
      ) : null}
      <dl className="counts">
        {COUNT_LABELS.map(([key, label]) => (
          <div key={key} className="counts__item">
            <dt>{label}</dt>
            <dd>{counts[key]}</dd>
          </div>
        ))}
      </dl>
    </li>
  )
}
