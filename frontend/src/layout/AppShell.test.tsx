import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ANA, MARTA, SESSION_ENDED, jsonResponse, mockFetch } from '../test/helpers'
import { renderApp } from '../test/renderApp'

describe('the top bar', () => {
  it('shows who is signed in, with the name and the role (scenarios 10.1 and 10.3)', async () => {
    mockFetch(jsonResponse(200, MARTA))

    renderApp('/')

    expect(await screen.findByText('Marta')).toBeInTheDocument()
    expect(screen.getByText('Project manager', { selector: '.topbar__role' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign out' })).toBeEnabled()
  })

  it('signs out and goes back to the sign in screen, without a message (scenario 10.4)', async () => {
    const fetch = mockFetch(jsonResponse(200, ANA), jsonResponse(204))
    renderApp('/')
    await screen.findByText('Ana')

    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('heading', { name: 'Sign in to Matriz' })).toBeInTheDocument()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    expect(fetch).toHaveBeenCalledTimes(2)
    expect(String((fetch.mock.calls[1] as unknown[])[0])).toMatch(/\/auth\/logout$/)
  })

  it('stays signed in and says so when the sign out fails', async () => {
    mockFetch(jsonResponse(200, ANA), new TypeError('Failed to fetch'))
    renderApp('/')
    await screen.findByText('Ana')

    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('cannot be reached')
    expect(screen.getByRole('heading', { name: 'Home' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign out' })).toBeEnabled()
  })

  it('goes to the sign in screen with the message when the session ended meanwhile', async () => {
    mockFetch(jsonResponse(200, ANA), jsonResponse(401, SESSION_ENDED))
    renderApp('/')
    await screen.findByText('Ana')

    await userEvent.click(screen.getByRole('button', { name: 'Sign out' })) // the API answers 401: the session was gone

    expect(await screen.findByRole('heading', { name: 'Sign in to Matriz' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Your session has ended')
  })

  it('folds into a menu that opens and closes (narrow screens)', async () => {
    mockFetch(jsonResponse(200, ANA))
    renderApp('/')
    await screen.findByText('Ana')
    const toggle = screen.getByRole('button', { name: 'Menu' })

    expect(toggle).toHaveAttribute('aria-expanded', 'false')
    await userEvent.click(toggle)
    expect(toggle).toHaveAttribute('aria-expanded', 'true')
    expect(document.getElementById('topbar-menu')).toHaveClass('topbar__menu--open')
    await userEvent.click(toggle)
    await waitFor(() => expect(toggle).toHaveAttribute('aria-expanded', 'false'))
    expect(document.getElementById('topbar-menu')).not.toHaveClass('topbar__menu--open')
  })
})
