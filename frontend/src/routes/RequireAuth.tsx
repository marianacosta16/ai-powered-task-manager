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
    return <Navigate to="/sign-in" replace state={{ from: location }} />
  }
  return <Outlet />
}
