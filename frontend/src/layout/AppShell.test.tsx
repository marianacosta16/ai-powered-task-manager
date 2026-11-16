import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ANA, MARTA, SESSION_ENDED, jsonResponse, mockApi } from '../test/helpers'
import { renderApp } from '../test/renderApp'

describe('the top bar', () => {
  it('shows who is signed in, with the name and the role (scenarios 10.1 and 10.3)', async () => {
    mockApi({ 'GET /auth/me': jsonResponse(200, MARTA), 'GET /dashboard': jsonResponse(200, []) })

    renderApp('/')

    expect(await screen.findByText('Marta')).toBeInTheDocument()
    expect(screen.getByText('Project manager', { selector: '.topbar__role' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign out' })).toBeEnabled()
  })

  it('signs out and goes back to the sign in screen, without a message (scenario 10.4)', async () => {
    const api = mockApi({ 'GET /auth/me': jsonResponse(200, ANA), 'GET /dashboard': jsonResponse(200, []), 'POST /auth/logout': jsonResponse(204) })
    renderApp('/')
    await screen.findByText('Ana')

    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('heading', { name: 'Sign in to Matriz' })).toBeInTheDocument()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    expect(api.callsTo('POST /auth/logout')).toHaveLength(1)
  })

  it('stays signed in and says so when the sign out fails', async () => {
    mockApi({ 'GET /auth/me': jsonResponse(200, ANA), 'GET /dashboard': jsonResponse(200, []), 'POST /auth/logout': new TypeError('Failed to fetch') })
    renderApp('/')
    await screen.findByText('Ana')

    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('cannot be reached')
    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign out' })).toBeEnabled()
  })

  it('goes to the sign in screen with the message when the session ended meanwhile', async () => {
    mockApi({ 'GET /auth/me': jsonResponse(200, ANA), 'GET /dashboard': jsonResponse(200, []), 'POST /auth/logout': jsonResponse(401, SESSION_ENDED) })
    renderApp('/')
    await screen.findByText('Ana')

    await userEvent.click(screen.getByRole('button', { name: 'Sign out' })) // the API answers 401: the session was gone

    expect(await screen.findByRole('heading', { name: 'Sign in to Matriz' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Your session has ended')
  })

  it('folds into a menu that opens and closes (narrow screens)', async () => {
    mockApi({ 'GET /auth/me': jsonResponse(200, ANA), 'GET /dashboard': jsonResponse(200, []) })
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

  it('marks the link of the screen that is open, and only that one', async () => {
    mockApi({ 'GET /auth/me': jsonResponse(200, ANA), 'GET /projects': jsonResponse(200, []) })

    renderApp('/projects')

    await screen.findByText('Ana')
    expect(screen.getByRole('link', { name: 'Projects' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('aria-current')
  })
})
