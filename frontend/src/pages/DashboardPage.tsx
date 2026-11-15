import { useAuth } from '../auth/useAuth'
import { Notice } from '../components/Notice'

/** The home screen. It only greets for now: the dashboard with the projects arrives with the next increment. */
export function DashboardPage() {
  const { state } = useAuth()
  if (state.status !== 'signedIn') {
    return null
  }
  return (
    <>
      <h1>Home</h1>
      <p>
        Welcome, {state.user.name}. You are signed in as {state.user.role}.
      </p>
      <Notice kind="info">The dashboard with your projects comes with the next increment.</Notice>
    </>
  )
}
