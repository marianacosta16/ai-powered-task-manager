import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useBlocker, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/errors'
import { getProject } from '../api/projects'
import { FINAL_STATUSES, deleteTask, getTask, listAssignableUsers, type Task } from '../api/tasks'
import { useAuth } from '../auth/useAuth'
import { Button } from '../components/Button'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { Spinner } from '../components/Spinner'
import { useLoad } from '../hooks/useLoad'
import { useNavigationMessage } from '../hooks/useNavigationMessage'
import { AssigneeList } from '../tasks/AssigneeList'
import { Comments } from '../tasks/Comments'
import { SubtaskSection } from '../tasks/SubtaskSection'
import { TaskEditForm } from '../tasks/TaskEditForm'

/** The detail of a task or a subtask (F02, F03, F08, F17; wireframe 5): the data, the assignees, the subtasks and the comments. */
export function TaskPage() {
  const id = Number(useParams().taskId)
  const { state, retry } = useLoad((signal) => getTask(id, signal), [id])
  if (state.status === 'loading') {
    return <Spinner label="Loading the task..." />
  }
  if (state.status === 'error') {
    return <LoadError message={state.message} onRetry={retry} />
  }
  // A key by the task that was read (not by the address, which changes one render before the data): another task starts from a clean screen
  return <TaskScreen key={state.data.id} first={state.data} />
}

function TaskScreen({ first }: { first: Task }) {
  const { state: auth } = useAuth()
  const navigate = useNavigate()
  // The project page sends where the user came from (the Matrix or the List, with its filters): the way back goes there
  const from = (useLocation().state as { from?: unknown } | null)?.from
  const sent = useNavigationMessage() // for example a subtask that was deleted
  const [task, setTask] = useState(first)
  const project = useLoad((signal) => getProject(first.projectId, signal), [first.projectId])
  const people = useLoad((signal) => listAssignableUsers(first.projectId, signal), [first.projectId])
  const parent = useLoad((signal) => (first.parentTaskId === null ? Promise.resolve(null) : getTask(first.parentTaskId, signal)), [first.parentTaskId])
  // Text typed and not saved, in each of the three places that have a form: the page asks before leaving
  const [unsaved, setUnsaved] = useState({ task: false, subtask: false, comment: false })
  const dirty = unsaved.task || unsaved.subtask || unsaved.comment
  const setTaskDirty = useCallback((value: boolean) => setUnsaved((u) => ({ ...u, task: value })), [])
  const setSubtaskDirty = useCallback((value: boolean) => setUnsaved((u) => ({ ...u, subtask: value })), [])
  const setCommentDirty = useCallback((value: boolean) => setUnsaved((u) => ({ ...u, comment: value })), [])
  const leaving = useRef(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  // Leaving with changes that are not saved asks first: a link, the back button...
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) => dirty && !leaving.current && currentLocation.pathname !== nextLocation.pathname,
  )
  // ...and closing or reloading the tab
  useEffect(() => {
    if (!dirty) return
    const warn = (event: BeforeUnloadEvent) => event.preventDefault()
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [dirty])

  // Rejects when the task cannot be read: the section says so instead of showing a list that is not up to date
  const refreshSubtasks = useCallback(async () => {
    const fresh = await getTask(first.id)
    setTask((current) => ({ ...current, subtasks: fresh.subtasks }))
  }, [first.id])

  if (auth.status !== 'signedIn') {
    return null
  }
  if (project.state.status === 'loading' || people.state.status === 'loading') {
    return <Spinner label="Loading the task..." />
  }
  if (project.state.status === 'error') {
    return <LoadError message={project.state.message} onRetry={project.retry} />
  }
  if (people.state.status === 'error') {
    return <LoadError message={people.state.message} onRetry={people.retry} />
  }
  const me = auth.user
  const data = project.state.data
  const completed = data.status === 'Completed'
  const closed = FINAL_STATUSES.includes(task.status)
  const isSubtask = task.parentTaskId !== null
  const parentTask = parent.state.status === 'ready' ? parent.state.data : null
  const back = isSubtask ? `/tasks/${task.parentTaskId}` : `/projects/${task.projectId}`

  async function handleDelete() {
    setDeleting(true)
    try {
      await deleteTask(task.id)
      leaving.current = true
      navigate(back, { state: { message: `${isSubtask ? 'Subtask' : 'Task'} "${task.title}" is in the Recycle bin for 30 days.` } })
    } catch (cause) {
      setDeleteError(cause instanceof ApiError ? cause.message : 'Something went wrong. Try again in a moment.')
      setConfirmDelete(false)
      setDeleting(false)
    }
  }

  return (
    <>
      <p>
        <Link to="/projects">Projects</Link> / <Link to={typeof from === 'string' && from.split('?')[0] === `/projects/${task.projectId}` ? from : `/projects/${task.projectId}`}>{data.name}</Link>
        {isSubtask ? (
          <>
            {' '}
            / <Link to={`/tasks/${task.parentTaskId}`}>Task: {parentTask?.title ?? 'parent task'}</Link> / Subtask: {task.title}
          </>
        ) : (
          <> / Task: {task.title}</>
        )}
      </p>
      <h1>{task.title}</h1>
      {sent ? <Notice kind="success">{sent}</Notice> : null}
      {deleteError ? <Notice kind="error">{deleteError}</Notice> : null}
      <TaskEditForm
        task={first}
        timezone={data.timezone}
        isManager={data.manager.id === me.id}
        readOnly={completed}
        onTaskChange={setTask}
        onDirtyChange={setTaskDirty}
      />
      <AssigneeList
        taskId={task.id}
        assignees={task.assignees}
        people={people.state.data}
        canEdit={!completed}
        onChange={(assignees) => setTask((current) => ({ ...current, assignees }))}
      />
      {!isSubtask ? (
        <SubtaskSection
          task={task}
          timezone={data.timezone}
          people={people.state.data}
          canAdd={!closed && data.status !== 'Completed' && data.status !== 'Suspended'}
          onCreated={refreshSubtasks}
          onDirtyChange={setSubtaskDirty}
        />
      ) : null}
      {!completed ? (
        <p>
          <Button variant="secondary" onClick={() => setConfirmDelete(true)}>
            Delete {isSubtask ? 'subtask' : 'task'}
          </Button>
        </p>
      ) : null}
      <Comments taskId={task.id} me={me} writable={!completed && !closed} onDirtyChange={setCommentDirty} />

      {confirmDelete ? (
        <ConfirmDialog
          title={`Delete ${isSubtask ? 'subtask' : 'task'}?`}
          confirmLabel="Delete"
          busy={deleting}
          onConfirm={() => void handleDelete()}
          onCancel={() => setConfirmDelete(false)}
        >
          <p>
            "{task.title}"{isSubtask ? '' : ', its subtasks'} and its comments go to the Recycle bin for 30 days. A task with open subtasks cannot be deleted.
          </p>
        </ConfirmDialog>
      ) : null}
      {blocker.state === 'blocked' ? (
        <ConfirmDialog
          title="Discard your changes?"
          confirmLabel="Discard changes"
          onConfirm={() => blocker.proceed()}
          onCancel={() => blocker.reset()}
        >
          <p>There is text that is not saved. If you leave now it is lost.</p>
        </ConfirmDialog>
      ) : null}
    </>
  )
}
