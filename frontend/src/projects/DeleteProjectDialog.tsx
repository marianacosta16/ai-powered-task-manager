import { useState } from 'react'
import { ApiError } from '../api/errors'
import { deleteProject } from '../api/projects'
import { ConfirmDialog } from '../components/ConfirmDialog'

interface DeleteProjectDialogProps {
  project: { id: number; name: string }
  onDeleted: () => void
  /** The API refused or the server could not be reached: the message to show. */
  onFailed: (message: string) => void
  onCancel: () => void
}

/**
 * The confirmation to delete a project and the request itself (R9, R20), the same on the list and on the project.
 * While the request is under way both buttons wait.
 */
export function DeleteProjectDialog({ project, onDeleted, onFailed, onCancel }: DeleteProjectDialogProps) {
  const [busy, setBusy] = useState(false)

  async function confirm() {
    setBusy(true)
    try {
      await deleteProject(project.id)
      onDeleted()
    } catch (cause) {
      onFailed(cause instanceof ApiError ? cause.message : 'Something went wrong. Try again in a moment.')
    }
  }

  return (
    <ConfirmDialog title="Delete project?" confirmLabel="Delete" busy={busy} onConfirm={() => void confirm()} onCancel={onCancel}>
      <p>
        "{project.name}" and its tasks go to the Recycle bin for 30 days. Only the project manager can restore them.
      </p>
    </ConfirmDialog>
  )
}
