import { act, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { request } from '../api/client'
import { ANA, SESSION_ENDED, SIGN_IN_NEEDED, deferredResponse, jsonResponse, mockFetch } from '../test/helpers'
import { AuthProvider } from './AuthProvider'
import { useAuth } from './useAuth'

function Probe() {
  const { state, signIn, signOut } = useAuth()
  return (
    <div>
      <p data-testid="status">{state.status}</p>
      <p data-testid="user">{state.status === 'signedIn' ? state.user.name : ''}</p>
      <p data-testid="message">{state.status === 'signedOut' ? (state.message ?? '') : ''}</p>
      <button onClick={() => signOut().catch(() => undefined)}>out</button>
      <button onClick={() => void signIn('ana', 'secret')}>in</button>
    </div>
  )
}

function renderProbe() {
  return render(
    <AuthProvider>
      <Probe />
    </AuthProvider>,
  )
}

describe('the session of the user', () => {
  it('is being checked at the start and then the user is signed in', async () => {
    mockFetch(jsonResponse(200, ANA))

    renderProbe()

    expect(screen.getByTestId('status')).toHaveTextContent('loading')
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('signedIn'))
    expect(screen.getByTestId('user')).toHaveTextContent('Ana')
  })

  it('is signed out, with the message of the API, when the API says the session ended', async () => {
    mockFetch(jsonResponse(401, SESSION_ENDED))

    renderProbe()

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('signedOut'))
    expect(screen.getByTestId('message')).toHaveTextContent('Your session has ended. Sign in again to continue.')
  })

  it('is signed out, with the message of the API, when there was no session', async () => {
    mockFetch(jsonResponse(401, SIGN_IN_NEEDED))

    renderProbe()

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('signedOut'))
    expect(screen.getByTestId('message')).toHaveTextContent('Sign in to continue.')
  })

  it('is signed out, saying that the server cannot be reached, when there is no network', async () => {
    mockFetch(new TypeError('Failed to fetch'))

    renderProbe()

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('signedOut'))
    expect(screen.getByTestId('message')).toHaveTextContent('cannot be reached')
  })

  it('is signed out by a 401 of any later request', async () => {
    mockFetch(jsonResponse(200, ANA), jsonResponse(401, SESSION_ENDED))
    renderProbe()
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('signedIn'))

    await act(async () => {
      await request('/projects').catch(() => undefined)
    })

    expect(screen.getByTestId('status')).toHaveTextContent('signedOut')
    expect(screen.getByTestId('message')).toHaveTextContent('Your session has ended')
  })

  it('is not signed out by a late 401 of a request that started before the user signed in', async () => {
    const late = deferredResponse()
    mockFetch(jsonResponse(401, SIGN_IN_NEEDED), late.fetch, jsonResponse(200, ANA))
    renderProbe()
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('signedOut'))
    const slow = request('/projects').catch(() => undefined) // started with no session

    await act(async () => {
      screen.getByRole('button', { name: 'in' }).click()
    })
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('signedIn'))
    late.respond(jsonResponse(401, SIGN_IN_NEEDED))
    await act(async () => {
      await slow
    })

    expect(screen.getByTestId('status')).toHaveTextContent('signedIn') // the new session was not thrown out
  })

  it('does not show "session ended" for a late 401 after the user signed out on purpose', async () => {
    const late = deferredResponse()
    mockFetch(jsonResponse(200, ANA), late.fetch, jsonResponse(204))
    renderProbe()
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('signedIn'))
    const slow = request('/projects').catch(() => undefined) // under way

    await act(async () => {
      screen.getByRole('button', { name: 'out' }).click()
    })
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('signedOut'))
    late.respond(jsonResponse(401, SESSION_ENDED))
    await act(async () => {
      await slow
    })

    expect(screen.getByTestId('status')).toHaveTextContent('signedOut')
    expect(screen.getByTestId('message')).toHaveTextContent('') // no "Your session has ended" for someone who signed out
  })

  it('still takes a 401 of a request under way when the sign out failed and the session is the same', async () => {
    const slow = deferredResponse()
    mockFetch(jsonResponse(200, ANA), slow.fetch, new TypeError('Failed to fetch'))
    renderProbe()
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('signedIn'))
    const pending = request('/projects').catch(() => undefined) // started before the sign out

    await act(async () => {
      screen.getByRole('button', { name: 'out' }).click() // fails: no network, the user stays signed in
    })
    expect(screen.getByTestId('status')).toHaveTextContent('signedIn')
    slow.respond(jsonResponse(401, SESSION_ENDED)) // and the session really had ended
    await act(async () => {
      await pending
    })

    expect(screen.getByTestId('status')).toHaveTextContent('signedOut')
    expect(screen.getByTestId('message')).toHaveTextContent('Your session has ended')
  })

  it('is signed out, without a message, when the user signs out', async () => {
    const fetch = mockFetch(jsonResponse(200, ANA), jsonResponse(204))
    renderProbe()
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('signedIn'))

    await act(async () => {
      screen.getByRole('button', { name: 'out' }).click()
    })

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('signedOut'))
    expect(screen.getByTestId('message')).toHaveTextContent('')
    expect((fetch.mock.calls[1] as unknown as [string, RequestInit])[0]).toContain('/auth/logout')
  })
})
