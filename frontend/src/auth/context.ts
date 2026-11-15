import { createContext } from 'react'
import type { User } from '../api/auth'

/** Where the session stands: being checked, signed in, or signed out (with the reason, when the API gave one). */
export type AuthState =
  | { status: 'loading' }
  | { status: 'signedIn'; user: User }
  | { status: 'signedOut'; message?: string }

export interface AuthContextValue {
  state: AuthState
  /** Signs in; throws the ApiError of the API when the credentials are not correct, so the form can show it. */
  signIn: (username: string, password: string) => Promise<User>
  signOut: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
