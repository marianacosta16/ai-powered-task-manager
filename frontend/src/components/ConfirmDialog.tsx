import { useEffect, useRef, type ReactNode } from 'react'
import { Button } from './Button'

interface ConfirmDialogProps {
  title: string
  children: ReactNode
  confirmLabel: string
  variant?: 'primary' | 'danger'
  /** The request of the confirmed action is under way: both buttons wait. */
  busy?: boolean
  onConfirm: () => void
  onCancel: () => void
}

/**
 * The confirmation before an action that cannot be undone by the user (delete, complete). Focus starts on Cancel, so
 * pressing Enter by accident does not confirm, and Escape cancels.
 */
export function ConfirmDialog({
  title,
  children,
  confirmLabel,
  variant = 'danger',
  busy = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const cancelRef = useRef<HTMLButtonElement>(null)
  useEffect(() => {
    cancelRef.current?.focus()
  }, [])
  return (
    <div className="overlay" onKeyDown={(event) => event.key === 'Escape' && !busy && onCancel()}>
      <div className="dialog" role="dialog" aria-modal="true" aria-labelledby="dialog-title">
        <h2 id="dialog-title">{title}</h2>
        <div className="dialog__body">{children}</div>
        <div className="dialog__actions">
          <Button ref={cancelRef} variant="secondary" disabled={busy} onClick={onCancel}>
            Cancel
          </Button>
          <Button variant={variant} busy={busy} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  )
}
