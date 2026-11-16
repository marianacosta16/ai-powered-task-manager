import { useState, type FormEvent } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { listProjects, type ProjectSummary } from '../api/projects'
import { useAuth } from '../auth/useAuth'
import { Button } from '../components/Button'
import { EmptyState } from '../components/EmptyState'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { Spinner } from '../components/Spinner'
import { StatusBadge } from '../components/StatusBadge'
import { useLoad } from '../hooks/useLoad'
import { DeleteProjectDialog } from '../projects/DeleteProjectDialog'
import { deletedMessage } from '../projects/messages'
import { ProjectEditor } from '../projects/ProjectEditor'
import { ProjectForm } from '../projects/ProjectForm'
import { PROJECT_NAME_MAX } from '../limits'

type Panel = { kind: 'closed' } | { kind: 'create' } | { kind: 'edit'; projectId: number }

/**
 * The projects screen (F01, F22, US01, US12; wireframe 3): the list with the search, and for a project manager the
 * form to create, edit and the confirmation to delete. Edit and Delete only appear on the projects the user manages
 * that are not completed (R9, R28). After each change the list is fetched again, so it shows what the server has.
 */
export function ProjectsPage() {
  const { state: auth } = useAuth()
  const [typed, setTyped] = useState('')
  const [search, setSearch] = useState('')
  const [panel, setPanel] = useState<Panel>({ kind: 'closed' })
  const [toDelete, setToDelete] = useState<ProjectSummary | null>(null)
  const location = useLocation()
  // The project page sends the message of a project it deleted
  const [message, setMessage] = useState<{ kind: 'success' | 'error'; text: string } | null>(() => {
    const text = (location.state as { message?: unknown } | null)?.message
    return typeof text === 'string' ? { kind: 'success', text } : null
  })
  const { state, reload, retry } = useLoad((signal) => listProjects(search, signal), [search])

  if (auth.status !== 'signedIn') {
    return null
  }
  const { user } = auth
  const isManager = user.role === 'Project manager'

  function handleSearch(event: FormEvent) {
    event.preventDefault()
    setSearch(typed.trim())
  }

  function saved(text: string) {
    setPanel({ kind: 'closed' })
    setMessage({ kind: 'success', text })
    reload()
  }

  return (
    <>
      <h1>Projects</h1>
      {message ? <Notice kind={message.kind}>{message.text}</Notice> : null}
      <div className="toolbar">
        <form className="toolbar__search" role="search" onSubmit={handleSearch}>
          <label className="visually-hidden" htmlFor="project-search">
            Search projects by name
          </label>
          <input
            id="project-search"
            className="field__input"
            type="search"
            value={typed}
            maxLength={PROJECT_NAME_MAX}
            placeholder="Search by name"
            onChange={(event) => setTyped(event.target.value)}
          />
          <Button type="submit" variant="secondary">
            Search
          </Button>
        </form>
        {isManager ? (
          <Button
            onClick={() => {
              setMessage(null)
              setPanel({ kind: 'create' })
            }}
          >
            + New project
          </Button>
        ) : null}
      </div>
      <div className="split">
        <div className="split__list">
          {state.status === 'loading' ? <Spinner label="Loading the projects..." /> : null}
          {state.status === 'error' ? <LoadError message={state.message} onRetry={retry} /> : null}
          {state.status === 'ready' && state.refreshError ? <Notice kind="error">{state.refreshError}</Notice> : null}
          {state.status === 'ready' ? (
            state.data.length === 0 ? (
              <EmptyState>{search === '' ? 'You have no projects yet.' : 'No projects found.'}</EmptyState>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th scope="col">Name</th>
                    <th scope="col">Phase</th>
                    <th scope="col">Status</th>
                    <th scope="col">Manager</th>
                    <th scope="col">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {state.data.map((project) => {
                    const manages = project.manager.id === user.id && project.status !== 'Completed'
                    return (
                      <tr key={project.id}>
                        <td data-label="Name">
                          <Link to={`/projects/${project.id}`}>{project.name}</Link>
                        </td>
                        <td data-label="Phase">{project.phase}</td>
                        <td data-label="Status">
                          <StatusBadge value={project.status} />
                        </td>
                        <td data-label="Manager">{project.manager.name}</td>
                        <td data-label="Actions" className="table__actions">
                          <Link className="button button--secondary" to={`/projects/${project.id}`}>
                            View{' '}<span className="visually-hidden">{project.name}</span>
                          </Link>
                          {manages ? (
                            <>
                              <Button
                                variant="secondary"
                                onClick={() => {
                                  setMessage(null)
                                  setPanel({ kind: 'edit', projectId: project.id })
                                }}
                              >
                                Edit{' '}<span className="visually-hidden">{project.name}</span>
                              </Button>
                              <Button
                                variant="secondary"
                                onClick={() => {
                                  setMessage(null)
                                  setToDelete(project)
                                }}
                              >
                                Delete{' '}<span className="visually-hidden">{project.name}</span>
                              </Button>
                            </>
                          ) : null}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )
          ) : null}
        </div>
        {isManager && panel.kind === 'create' ? (
          <ProjectForm
            project={null}
            managers={[]}
            onSaved={(project) => saved(`Project "${project.name}" created.`)}
            onCancel={() => setPanel({ kind: 'closed' })}
          />
        ) : null}
        {isManager && panel.kind === 'edit' ? (
          <ProjectEditor
            projectId={panel.projectId}
            onSaved={(project) => saved(`Project "${project.name}" saved.`)}
            onCancel={() => setPanel({ kind: 'closed' })}
          />
        ) : null}
      </div>
      {toDelete ? (
        <DeleteProjectDialog
          project={toDelete}
          onDeleted={() => {
            setMessage({ kind: 'success', text: deletedMessage(toDelete.name) })
            setPanel((current) => (current.kind === 'edit' && current.projectId === toDelete.id ? { kind: 'closed' } : current))
            setToDelete(null)
            reload()
          }}
          onFailed={(text) => {
            setMessage({ kind: 'error', text })
            setToDelete(null)
            reload() // the project may have changed meanwhile: show what the server has
          }}
          onCancel={() => setToDelete(null)}
        />
      ) : null}
    </>
  )
}
