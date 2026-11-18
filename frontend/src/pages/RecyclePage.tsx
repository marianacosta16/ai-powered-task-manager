import { useState } from 'react'
import { ApiError, messageOf } from '../api/errors'
import {
  ASSIGNEE_REQUIRED,
  listRecycleBin,
  restoreProject,
  restoreTask,
  type NotRestorableReason,
  type RecycleItem,
} from '../api/recycle'
import { Button } from '../components/Button'
import { EmptyState } from '../components/EmptyState'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { Spinner } from '../components/Spinner'
import { useLoad } from '../hooks/useLoad'
import { ReplacementDialog } from '../recycle/ReplacementDialog'

const WHY_NOT: Record<NotRestorableReason, string> = {
  PROJECT_IN_RECYCLE_BIN: 'Restore the project first.',
  PARENT_IN_RECYCLE_BIN: 'Restore the parent task first.',
  PROJECT_COMPLETED: 'The project is completed.',
}

const DAY = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium' })

/**
 * The recycle bin (F09, US07; wireframe 7): what was deleted in the projects the user can reach, with the days left before
 * it is removed for good (R20). A manager also sees and restores projects (R9). An item that cannot come back says why.
 */
export function RecyclePage() {
  const { state, reload, retry } = useLoad((signal) => listRecycleBin(signal), [])
  const [message, setMessage] = useState<{ kind: 'success' | 'error'; text: string } | null>(null)
  const [busyKey, setBusyKey] = useState<string | null>(null)
  const [needsReplacement, setNeedsReplacement] = useState<{ item: RecycleItem; reason: string } | null>(null)

  async function restore(item: RecycleItem) {
    if (busyKey !== null) return
    setBusyKey(`${item.itemType}-${item.id}`)
    setMessage(null)
    try {
      if (item.itemType === 'Project') {
        await restoreProject(item.id)
      } else {
        await restoreTask(item.id)
      }
      setMessage({ kind: 'success', text: `"${item.name}" was restored.` })
    } catch (cause) {
      if (cause instanceof ApiError && cause.code === ASSIGNEE_REQUIRED) {
        setNeedsReplacement({ item, reason: cause.message })
      } else {
        setMessage({ kind: 'error', text: messageOf(cause) })
      }
    } finally {
      setBusyKey(null)
      reload() // what the server has now: the item is gone, or it could not come back
    }
  }

  return (
    <>
      <h1>Recycle bin</h1>
      <p className="hint">Deleted items are removed for good 30 days after they were deleted. You only see the items of the projects you can access.</p>
      {message ? <Notice kind={message.kind}>{message.text}</Notice> : null}
      {state.status === 'loading' ? <Spinner label="Loading the recycle bin..." /> : null}
      {state.status === 'error' ? <LoadError message={state.message} retryable={state.retryable} onRetry={retry} /> : null}
      {state.status === 'ready' && state.refreshError ? <Notice kind="error">{state.refreshError}</Notice> : null}
      {state.status === 'ready' ? (
        state.data.length === 0 ? (
          <EmptyState>The recycle bin is empty.</EmptyState>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th scope="col">Type</th>
                <th scope="col">Name</th>
                <th scope="col">Project</th>
                <th scope="col">Deleted</th>
                <th scope="col">Days left</th>
                <th scope="col">Actions</th>
              </tr>
            </thead>
            <tbody>
              {state.data.map((item) => (
                <tr key={`${item.itemType}-${item.id}`}>
                  <td data-label="Type">{item.itemType}</td>
                  <td data-label="Name">
                    {item.name}
                    {item.itemType === 'Project' && item.includedTasks != null ? (
                      <span className="hint"> · {item.includedTasks} {item.includedTasks === 1 ? 'task' : 'tasks'} deleted with it</span>
                    ) : null}
                  </td>
                  <td data-label="Project">{item.itemType === 'Project' ? '' : item.projectName}</td>
                  <td data-label="Deleted">{DAY.format(new Date(item.deletedAt))}</td>
                  <td data-label="Days left">{item.daysLeft}</td>
                  <td data-label="Actions">
                    {item.restorable ? (
                      <Button variant="secondary" busy={busyKey === `${item.itemType}-${item.id}`} onClick={() => void restore(item)}>
                        Restore{' '}<span className="visually-hidden">{item.name}</span>
                      </Button>
                    ) : (
                      <>
                        <Button variant="secondary" disabled>
                          Restore{' '}<span className="visually-hidden">{item.name}</span>
                        </Button>{' '}
                        <span className="hint">{item.notRestorableReason ? WHY_NOT[item.notRestorableReason] : 'It cannot be restored now.'}</span>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )
      ) : null}
      {needsReplacement ? (
        <ReplacementDialog
          item={needsReplacement.item}
          reason={needsReplacement.reason}
          onRestored={() => {
            setMessage({ kind: 'success', text: `"${needsReplacement.item.name}" was restored.` })
            setNeedsReplacement(null)
            reload()
          }}
          onCancel={() => setNeedsReplacement(null)}
        />
      ) : null}
    </>
  )
}
