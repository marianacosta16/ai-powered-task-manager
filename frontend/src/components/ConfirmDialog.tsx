import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react'
import { Button } from './Button'

interface ConfirmDialogProps {
  title: string
  children: ReactNode
  confirmLabel: string
  variant?: 'primary' | 'danger'
  /** The request of the confirmed action is under way: both buttons wait. */
  busy?: boolean
  /** The action cannot be confirmed yet (a choice is missing). */
  confirmDisabled?: boolean
  onConfirm: () => void
  onCancel: () => void
}

/**
 * The confirmation before an action that cannot be undone by the user (delete, complete). Focus starts on Cancel, so
 * pressing Enter by accident does not confirm; Tab stays inside the dialog; Escape cancels from anywhere (unless the
 * request is under way); and when the dialog closes the focus goes back to where it was.
 */
export function ConfirmDialog({
  title,
  children,
  confirmLabel,
  variant = 'danger',
  busy = false,
  confirmDisabled = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const titleId = useId() // one per dialog: two can be open at once (a confirmation and the one of the router)
  const cancelRef = useRef<HTMLButtonElement>(null)
  const dialogRef = useRef<HTMLDivElement>(null)
  const latest = useRef({ busy, onCancel })
  useEffect(() => {
    latest.current = { busy, onCancel }
  })

  useEffect(() => {
    const before = document.activeElement as HTMLElement | null
    cancelRef.current?.focus()
    function onEscape(event: globalThis.KeyboardEvent) {
      if (event.key === 'Escape' && !latest.current.busy) {
        latest.current.onCancel()
      }
    }
    document.addEventListener('keydown', onEscape)
    return () => {
      document.removeEventListener('keydown', onEscape)
      if (before?.isConnected) {
        before.focus()
      }
    }
  }, [])

  function keepFocusInside(event: KeyboardEvent) {
    if (event.key !== 'Tab') {
      return
    }
    const buttons = Array.from(dialogRef.current?.querySelectorAll<HTMLButtonElement>('button:not(:disabled)') ?? [])
    if (buttons.length === 0) {
      event.preventDefault() // everything is waiting: the focus stays where it is
      return
    }
    const first = buttons[0]
    const last = buttons[buttons.length - 1]
    const active = document.activeElement
    if (!dialogRef.current?.contains(active)) {
      event.preventDefault()
      first.focus()
    } else if (event.shiftKey && active === first) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && active === last) {
      event.preventDefault()
      first.focus()
    }
  }

  return (
    <div className="overlay" onKeyDown={keepFocusInside}>
      <div ref={dialogRef} className="dialog" role="dialog" aria-modal="true" aria-labelledby={titleId}>
        <h2 id={titleId}>{title}</h2>
        <div className="dialog__body">{children}</div>
        <div className="dialog__actions">
          <Button ref={cancelRef} variant="secondary" disabled={busy} onClick={onCancel}>
            Cancel
          </Button>
          <Button variant={variant} busy={busy} disabled={confirmDisabled} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  )
}
