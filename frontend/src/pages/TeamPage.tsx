import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { messageOf } from '../api/errors'
import { addMember, listCandidateMembers, listMembers, type Member } from '../api/members'
import { getProject } from '../api/projects'
import { useAuth } from '../auth/useAuth'
import { Button } from '../components/Button'
import { EmptyState } from '../components/EmptyState'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { Select } from '../components/Select'
import { Spinner } from '../components/Spinner'
import { useLoad } from '../hooks/useLoad'
import { RemoveMemberDialog } from '../team/RemoveMemberDialog'

/** The team of a project (F01, US01; wireframe 6). Only the manager of the project gets here, and not in a completed project (R28). */
export function TeamPage() {
  const id = Number(useParams().projectId)
  // A key by project: going from one team to another starts from a clean screen
  return <Team key={id} id={id} />
}

function Team({ id }: { id: number }) {
  const { state: auth } = useAuth()
  const project = useLoad((signal) => getProject(id, signal), [id])
  if (auth.status !== 'signedIn') {
    return null
  }
  if (project.state.status === 'loading') {
    return <Spinner label="Loading the project..." />
  }
  if (project.state.status === 'error') {
    return <LoadError message={project.state.message} onRetry={project.retry} />
  }
  const data = project.state.data
  const back = (
    <p>
      <Link to="/projects">Projects</Link> / <Link to={`/projects/${id}`}>{data.name}</Link> / Team
    </p>
  )
  if (data.manager.id !== auth.user.id) {
    return (
      <>
        {back}
        <Notice kind="error">Only the manager of the project manages its team.</Notice>
      </>
    )
  }
  if (data.status === 'Completed') {
    return (
      <>
        {back}
        <Notice kind="info">The project is completed, so its team cannot be changed.</Notice>
      </>
    )
  }
  return (
    <>
      {back}
      <h1>Team of {data.name}</h1>
      <TeamManager projectId={id} projectName={data.name} />
    </>
  )
}

function TeamManager({ projectId, projectName }: { projectId: number; projectName: string }) {
  const members = useLoad((signal) => listMembers(projectId, signal), [projectId])
  const candidates = useLoad((signal) => listCandidateMembers(projectId, signal), [projectId])
  const [choice, setChoice] = useState('')
  const [pending, setPending] = useState(false)
  const [message, setMessage] = useState<{ kind: 'success' | 'error'; text: string } | null>(null)
  const [leaving, setLeaving] = useState<Member | null>(null)

  function refresh() {
    members.reload()
    candidates.reload()
  }

  if (members.state.status === 'loading' || candidates.state.status === 'loading') {
    return <Spinner label="Loading the team..." />
  }
  if (members.state.status === 'error') {
    return <LoadError message={members.state.message} onRetry={members.retry} />
  }
  if (candidates.state.status === 'error') {
    return <LoadError message={candidates.state.message} onRetry={candidates.retry} />
  }
  const team = members.state.data
  const people = candidates.state.data
  // The person chosen is only valid while they are still available: after a refresh they may not be
  const selected = people.some((candidate) => String(candidate.user.id) === choice && candidate.available) ? choice : ''
  const refreshError = members.state.refreshError ?? candidates.state.refreshError
  const shared = team.filter((member) => member.inOtherOpenProject)

  async function handleAdd() {
    if (selected === '') return
    setPending(true)
    setMessage(null)
    try {
      const added = await addMember(projectId, Number(selected))
      setChoice('')
      setMessage({ kind: 'success', text: `${added.name} joined "${projectName}".` })
    } catch (cause) {
      setMessage({ kind: 'error', text: messageOf(cause) })
    } finally {
      setPending(false)
      refresh() // who is available may have changed
    }
  }

  return (
    <>
      {message ? <Notice kind={message.kind}>{message.text}</Notice> : null}
      {refreshError ? <Notice kind="error">{refreshError}</Notice> : null}
      {shared.length > 0 ? (
        <Notice kind="error">
          The project was resumed and {shared.map((member) => member.name).join(', ')} {shared.length === 1 ? 'is' : 'are'} also in another
          open project.
        </Notice>
      ) : null}
      {team.length === 0 ? (
        <EmptyState>This project has no members yet.</EmptyState>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col">Role</th>
              <th scope="col">Open tasks</th>
              <th scope="col">Actions</th>
            </tr>
          </thead>
          <tbody>
            {team.map((member) => (
              <tr key={member.id}>
                <td data-label="Name">
                  {member.name}
                  {member.inOtherOpenProject ? <span className="badge badge--late">Also in another open project</span> : null}
                </td>
                <td data-label="Role">{member.role}</td>
                <td data-label="Open tasks">{member.openTaskCount}</td>
                <td data-label="Actions">
                  <Button variant="secondary" onClick={() => setLeaving(member)}>
                    Remove{' '}<span className="visually-hidden">{member.name}</span>
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <h2>Add member</h2>
      <div className="actions actions--end">
        <Select
          label="Person"
          value={selected}
          options={[
            { value: '', label: 'Choose...' },
            ...people.map((candidate) => ({
              value: String(candidate.user.id),
              label: candidate.available ? candidate.user.name : `${candidate.user.name} (in another open project)`,
              disabled: !candidate.available, // R11: shown, with the reason, but not offered
            })),
          ]}
          onChange={(event) => setChoice(event.target.value)}
        />
        <Button busy={pending} disabled={selected === ''} onClick={() => void handleAdd()}>
          Add member
        </Button>
      </div>
      {leaving ? (
        <RemoveMemberDialog
          projectId={projectId}
          member={leaving}
          members={team}
          candidates={people}
          onRemoved={() => {
            setMessage({ kind: 'success', text: `${leaving.name} was removed from "${projectName}".` })
            setLeaving(null)
            refresh()
          }}
          onCancel={() => setLeaving(null)}
        />
      ) : null}
    </>
  )
}
