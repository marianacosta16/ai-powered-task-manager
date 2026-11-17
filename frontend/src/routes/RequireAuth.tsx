import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { Spinner } from '../components/Spinner'

/**
 * The screens inside need a session. Without one the user goes to the sign in screen, and after signing in comes
 * back to the screen that was asked for (scenario 10.5).
 */
export function RequireAuth() {
  const { state } = useAuth()
  const location = useLocation()
  if (state.status === 'loading') {
    return <Spinner label="Checking your session..." />
  }
  if (state.status === 'signedOut') {
    // A session that ended (or was never there) brings the user back to this screen after signing in. A sign out on purpose
    // has no message and does not: whoever signs in next, maybe someone else, starts at the dashboard.
    return <Navigate to="/sign-in" replace state={state.message ? { from: location } : null} />
  }
  return <Outlet />
}
