import { Button } from './Button'
import { Notice } from './Notice'

/** The error of a screen that could not load, with the way to try again. */
export function LoadError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <>
      <Notice kind="error">{message}</Notice>
      <Button variant="secondary" onClick={onRetry}>
        Try again
      </Button>
    </>
  )
}
