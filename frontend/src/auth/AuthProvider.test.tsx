import { act, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { request } from '../api/client'
import { ANA, SESSION_ENDED, SIGN_IN_NEEDED, jsonResponse, mockFetch } from '../test/helpers'
import { AuthProvider } from './AuthProvider'
import { useAuth } from './useAuth'

function Probe() {
  const { state, signOut } = useAuth()
  return (
    <div>
      <p data-testid="status">{state.status}</p>
      <p data-testid="user">{state.status === 'signedIn' ? state.user.name : ''}</p>
      <p data-testid="message">{state.status === 'signedOut' ? (state.message ?? '') : ''}</p>
      <button onClick={() => void signOut()}>out</button>
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
