import { request } from './client'

/** The role of a user, as the contract says it. */
export type Role = 'Team member' | 'Project manager'

/** User of the contract: who is signed in. The password never comes from the API. */
export interface User {
  id: number
  name: string
  role: Role
}

/** signIn: starts the session; the API puts the session cookie in the answer. */
export function signIn(username: string, password: string): Promise<User> {
  return request<User>('/auth/login', { method: 'POST', body: { username, password } })
}

/** signOut: ends the session. */
export function signOut(): Promise<void> {
  return request<void>('/auth/logout', { method: 'POST' })
}

/** getAuthenticatedUser: the user of the session, or a 401 if there is none. */
export function getAuthenticatedUser(): Promise<User> {
  return request<User>('/auth/me')
}
