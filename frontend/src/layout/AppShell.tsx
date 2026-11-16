import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { ApiError } from '../api/errors'
import { useAuth } from '../auth/useAuth'
import { Button } from '../components/Button'
import { Notice } from '../components/Notice'

/**
 * The frame of every screen after signing in: the top bar with who is signed in and the way to sign out (scenarios
 * 10.1, 10.3 and 10.4). On narrow screens the user and the sign out button fold into a menu.
 */
export function AppShell() {
  const { state, signOut } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (state.status !== 'signedIn') {
    return null // RequireAuth sends the user to the sign in screen
  }
  const { user } = state

  async function handleSignOut() {
    setPending(true)
    setError(null)
    try {
      await signOut()
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Something went wrong. Try again in a moment.')
      setPending(false)
    }
  }

  return (
    <div className="shell">
      <header className="topbar">
        <Link to="/" className="topbar__brand">
          Matriz
        </Link>
        <Button
          variant="secondary"
          className="topbar__toggle"
          aria-expanded={menuOpen}
          aria-controls="topbar-menu"
          onClick={() => setMenuOpen((open) => !open)}
        >
          Menu
        </Button>
        <div id="topbar-menu" className={`topbar__menu${menuOpen ? ' topbar__menu--open' : ''}`}>
          <nav className="topbar__nav" aria-label="Main">
            <NavLink to="/">
              Dashboard
            </NavLink>
            <NavLink to="/projects">Projects</NavLink>
          </nav>
          <span className="topbar__user">
            <strong>{user.name}</strong>
            <span className="topbar__role">{user.role}</span>
          </span>
          <Button variant="secondary" busy={pending} onClick={handleSignOut}>
            Sign out
          </Button>
        </div>
      </header>
      <main className="page">
        {error ? <Notice kind="error">{error}</Notice> : null}
        <Outlet />
      </main>
    </div>
  )
}
