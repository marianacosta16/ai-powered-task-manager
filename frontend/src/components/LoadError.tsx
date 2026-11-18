import { Link } from 'react-router-dom'
import { Button } from './Button'
import { Notice } from './Notice'

interface LoadErrorProps {
  message: string
  onRetry: () => void
  /** Whether trying again can help; a refusal (403, 404...) gets the same answer, so there it offers the way out instead. */
  retryable?: boolean
}

/** The error of a screen that could not load, with the way to try again, or the way out when trying again is no use. */
export function LoadError({ message, onRetry, retryable = true }: LoadErrorProps) {
  return (
    <>
      <Notice kind="error">{message}</Notice>
      {retryable ? (
        <Button variant="secondary" onClick={onRetry}>
          Try again
        </Button>
      ) : (
        <Link className="button button--secondary" to="/">
          Go to the dashboard
        </Link>
      )}
    </>
  )
}
