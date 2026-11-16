import { Link } from 'react-router-dom'

/** A path that does not exist. */
export function NotFoundPage() {
  return (
    <>
      <h1>Page not found</h1>
      <p>
        This page does not exist. <Link to="/">Go to the home screen</Link>.
      </p>
    </>
  )
}
