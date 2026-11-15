import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import { setUnauthorizedHandler } from '../api/client'
import { ApiError } from '../api/errors'
import { AuthContext, type AuthContextValue, type AuthState } from './context'

/**
 * Keeps who is signed in. At the start it asks the API (getAuthenticatedUser), because the session is a cookie that
 * the page cannot read. Any 401 of the API, in any screen, ends here: the user is signed out and the sign in screen
 * shows the message of the API (scenarios 10.5 and 10.6, P33).
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ status: 'loading' })

  useEffect(() => {
    let cancelled = false
    setUnauthorizedHandler((error) => setState({ status: 'signedOut', message: error.message }))
    authApi
      .getAuthenticatedUser()
      .then((user) => {
        if (!cancelled) {
          setState({ status: 'signedIn', user })
        }
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setState({ status: 'signedOut', message: error instanceof ApiError ? error.message : undefined })
        }
      })
    return () => {
      cancelled = true
      setUnauthorizedHandler(null)
    }
  }, [])

  const signIn = useCallback(async (username: string, password: string) => {
    const user = await authApi.signIn(username, password)
    setState({ status: 'signedIn', user })
    return user
  }, [])

  const signOut = useCallback(async () => {
    await authApi.signOut()
    setState({ status: 'signedOut' })
  }, [])

  const value = useMemo<AuthContextValue>(() => ({ state, signIn, signOut }), [state, signIn, signOut])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
