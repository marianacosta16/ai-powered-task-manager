import { Outlet } from 'react-router-dom'
import { AuthProvider } from '../auth/AuthProvider'

/** The session is provided above every screen. */
export function Root() {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  )
}
