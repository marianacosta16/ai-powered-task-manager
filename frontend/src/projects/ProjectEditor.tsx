import { getProject, type Project } from '../api/projects'
import { listCandidateMembers, listMembers } from '../api/members'
import type { User } from '../api/auth'
import { LoadError } from '../components/LoadError'
import { Spinner } from '../components/Spinner'
import { useLoad } from '../hooks/useLoad'
import { ProjectForm } from './ProjectForm'

interface EditorData {
  project: Project
  managers: User[]
}

/**
 * Loads what the edit form needs: the project and who can take it over. The managers come from the members and the
 * candidates of the project (no endpoint of its own), plus the current manager. A candidate who is not available (R11) is left out: the API would refuse them.
 */
async function loadEditor(id: number, signal: AbortSignal): Promise<EditorData> {
  const [project, members, candidates] = await Promise.all([
    getProject(id, signal),
    listMembers(id, signal),
    listCandidateMembers(id, signal),
  ])
  const people: User[] = [project.manager, ...members, ...candidates.filter((candidate) => candidate.available).map((candidate) => candidate.user)]
  const managers = people
    .filter((person) => person.role === 'Project manager')
    .filter((person, index, all) => all.findIndex((other) => other.id === person.id) === index)
    .map(({ id: userId, name, role }) => ({ id: userId, name, role }))
  return { project, managers }
}

interface ProjectEditorProps {
  projectId: number
  onSaved: (project: Project) => void
  onCancel: () => void
}

export function ProjectEditor({ projectId, onSaved, onCancel }: ProjectEditorProps) {
  const { state, retry } = useLoad((signal) => loadEditor(projectId, signal), [projectId])
  if (state.status === 'loading') {
    return <Spinner label="Loading the project..." />
  }
  if (state.status === 'error') {
    return <LoadError message={state.message} onRetry={retry} />
  }
  return (
    <ProjectForm
      key={projectId}
      project={state.data.project}
      managers={state.data.managers}
      onSaved={onSaved}
      onCancel={onCancel}
    />
  )
}
