import { useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { getProject } from '../api/projects'
import { QUADRANTS, STATUSES_OF_TASK, listProjectTasks } from '../api/tasks'
import { useAuth } from '../auth/useAuth'
import { Button } from '../components/Button'
import { EmptyState } from '../components/EmptyState'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { Pagination } from '../components/Pagination'
import { Select } from '../components/Select'
import { Spinner } from '../components/Spinner'
import { StatusBadge } from '../components/StatusBadge'
import { useLoad } from '../hooks/useLoad'
import { useNavigationMessage } from '../hooks/useNavigationMessage'
import { DeleteProjectDialog } from '../projects/DeleteProjectDialog'
import { deletedMessage } from '../projects/messages'
import { ProjectEditor } from '../projects/ProjectEditor'
import { TaskCreator } from '../tasks/TaskCreator'
import { TaskList } from '../tasks/TaskList'
import { TaskMatrix } from '../tasks/TaskMatrix'
import { LIST_SIZE, MATRIX_SIZE, parseQuery, toParams, type TaskQuery } from '../tasks/taskView'

type Panel = 'closed' | 'new-task' | 'edit-project'

/**
 * The project (F01, F02, F04, F05, F22; wireframe 4 and the Matrix wireframe): its data, the Matrix and List tabs,
 * the filters and the new task form. The tab and the filters live in the address, so reloading or going back keeps
 * them. The manager of an open project can edit or delete it from here.
 */
export function ProjectDetailPage() {
  const id = Number(useParams().projectId)
  // A key by project: going from one project to another starts from a clean screen (no open form or old message)
  return <ProjectDetail key={id} id={id} />
}

function ProjectDetail({ id }: { id: number }) {
  const { state: auth } = useAuth()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const query = parseQuery(params)
  const [panel, setPanel] = useState<Panel>('closed')
  // A success message describes the list as it was: it belongs to the view and the filters of that moment, and goes when they change
  const sent = useNavigationMessage() // for example a task that was deleted
  const [message, setMessage] = useState<{ text: string; search: string } | null>(() =>
    sent === null ? null : { text: sent, search: params.toString() },
  )
  const search = params.toString()
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const project = useLoad((signal) => getProject(id, signal), [id])
  const tasks = useLoad(
    (signal) =>
      listProjectTasks(
        id,
        query.view === 'matrix'
          ? { mine: query.mine, size: MATRIX_SIZE }
          : { mine: query.mine, status: query.status, quadrant: query.quadrant, page: query.page, size: LIST_SIZE },
        signal,
      ),
    [id, query.view, query.mine, query.status, query.quadrant, query.page],
  )

  if (auth.status !== 'signedIn') {
    return null
  }
  const user = auth.user

  function change(next: Partial<TaskQuery>) {
    setParams(toParams({ ...query, page: 1, ...next }))
  }

  if (project.state.status === 'loading') {
    return <Spinner label="Loading the project..." />
  }
  if (project.state.status === 'error') {
    return <LoadError message={project.state.message} retryable={project.state.retryable} onRetry={project.retry} />
  }
  const data = project.state.data
  const completed = data.status === 'Completed'
  const manages = data.manager.id === user.id && !completed
  const canCreate = data.status !== 'Completed' && data.status !== 'Suspended' // R22
  const filtersOn = query.mine || query.status !== undefined || query.quadrant !== undefined

  return (
    <>
      <p>
        <Link to="/projects">Projects</Link> / {data.name}
      </p>
      <h1>{data.name}</h1>
      {project.state.refreshError ? <Notice kind="error">{project.state.refreshError}</Notice> : null}
      {message && message.search === search ? <Notice kind="success">{message.text}</Notice> : null}
      {deleteError ? <Notice kind="error">{deleteError}</Notice> : null}
      <p>
        {data.phase} · <StatusBadge value={data.status} /> · Manager: {data.manager.name}
      </p>
      <p>
        {data.startDate}
        {data.startTime ? ` ${data.startTime.slice(0, 5)}` : ''} to {data.endDate}
        {data.endTime ? ` ${data.endTime.slice(0, 5)}` : ''} ({data.timezone})
      </p>
      {data.description ? <p className="preline">{data.description}</p> : null}
      {manages ? (
        <div className="actions">
          <Button
            variant="secondary"
            onClick={() => {
              setMessage(null)
              setPanel('edit-project')
            }}
          >
            Edit project
          </Button>
          <Link className="button button--secondary" to={`/projects/${id}/team`}>
            Team
          </Link>
          <Button variant="secondary" onClick={() => setConfirmDelete(true)}>
            Delete project
          </Button>
        </div>
      ) : null}

      <nav className="tabs" aria-label="View of the tasks">
        <Link
          aria-current={query.view === 'matrix' ? 'page' : undefined}
          className="tabs__tab"
          to={{ search: toParams({ ...query, view: 'matrix', page: 1 }).toString() }}
        >
          Matrix
        </Link>
        <Link
          aria-current={query.view === 'list' ? 'page' : undefined}
          className="tabs__tab"
          to={{ search: toParams({ ...query, view: 'list', page: 1 }).toString() }}
        >
          List
        </Link>
      </nav>

      <div className="toolbar">
        <div className="filters">
          <label className="filters__mine">
            <input type="checkbox" checked={query.mine} onChange={(event) => change({ mine: event.target.checked })} />
            My tasks
          </label>
          {query.view === 'list' ? (
            <>
              <Select
                label="Status"
                value={query.status ?? ''}
                options={[{ value: '', label: 'All' }, ...STATUSES_OF_TASK.map((s) => ({ value: s, label: s }))]}
                onChange={(event) => change({ status: STATUSES_OF_TASK.find((s) => s === event.target.value) })}
              />
              <Select
                label="Quadrant"
                value={query.quadrant ?? ''}
                options={[{ value: '', label: 'All' }, ...QUADRANTS.map((q) => ({ value: q, label: q }))]}
                onChange={(event) => change({ quadrant: QUADRANTS.find((q) => q === event.target.value) })}
              />
            </>
          ) : null}
        </div>
        {canCreate ? (
          <Button
            onClick={() => {
              setMessage(null)
              setPanel('new-task')
            }}
          >
            + New task
          </Button>
        ) : null}
      </div>

      <div className="split">
        <div className="split__list">
          {tasks.state.status === 'loading' ? <Spinner label="Loading the tasks..." /> : null}
          {tasks.state.status === 'error' ? <LoadError message={tasks.state.message} retryable={tasks.state.retryable} onRetry={tasks.retry} /> : null}
          {tasks.state.status === 'ready' && tasks.state.refreshError ? (
            <Notice kind="error">{tasks.state.refreshError}</Notice>
          ) : null}
          {tasks.state.status === 'ready' ? (
            tasks.state.data.items.length === 0 ? (
              <>
                <EmptyState>
                  {query.page > 1
                    ? 'There are no tasks on this page.'
                    : filtersOn
                      ? 'No tasks match the filters.'
                      : 'This project has no tasks yet.'}
                </EmptyState>
                {query.view === 'list' && query.page > 1 ? (
                  <Pagination page={query.page} size={LIST_SIZE} total={tasks.state.data.total}
                  noun="task" onPage={(page) => change({ page })} />
                ) : null}
              </>
            ) : query.view === 'list' ? (
              <>
                <TaskList tasks={tasks.state.data.items} />
                <Pagination
                  page={query.page}
                  size={LIST_SIZE}
                  total={tasks.state.data.total}
                  noun="task"
                  onPage={(page) => change({ page })}
                />
              </>
            ) : (
              <>
                {tasks.state.data.total > tasks.state.data.items.length ? (
                  <Notice kind="info">
                    The Matrix shows the first {tasks.state.data.items.length} of {tasks.state.data.total} tasks. Use
                    the List to see all of them.
                  </Notice>
                ) : null}
                <TaskMatrix tasks={tasks.state.data.items} />
              </>
            )
          ) : null}
        </div>
        {panel === 'new-task' && canCreate ? (
          <TaskCreator
            projectId={id}
            timezone={data.timezone}
            onCreated={(task) => {
              setPanel('closed')
              setMessage({ text: `Task "${task.title}" created.`, search })
              tasks.reload()
            }}
            onCancel={() => setPanel('closed')}
          />
        ) : null}
        {panel === 'edit-project' && manages ? (
          <ProjectEditor
            projectId={id}
            onSaved={(saved) => {
              if (saved.manager.id !== user.id) {
                // handed over: this user no longer sees the project (R19), so the page leaves it
                navigate('/projects', { state: { message: `Project "${saved.name}" saved. ${saved.manager.name} is now its manager.` } })
                return
              }
              setPanel('closed')
              setMessage({ text: `Project "${saved.name}" saved.`, search })
              project.reload()
            }}
            onCancel={() => setPanel('closed')}
          />
        ) : null}
      </div>

      {confirmDelete ? (
        <DeleteProjectDialog
          project={data}
          onDeleted={() => navigate('/projects', { state: { message: deletedMessage(data.name) } })}
          onFailed={(text) => {
            setDeleteError(text)
            setConfirmDelete(false)
          }}
          onCancel={() => setConfirmDelete(false)}
        />
      ) : null}
    </>
  )
}
