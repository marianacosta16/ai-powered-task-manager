import { useContext } from 'react'
import { AuthContext, type AuthContextValue } from './context'

/** The session of the user; it must be used inside the AuthProvider. */
export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (value === null) {
    throw new Error('useAuth must be used inside an AuthProvider')
  }
  return value
}
