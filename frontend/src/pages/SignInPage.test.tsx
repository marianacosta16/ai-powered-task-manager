import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  ANA,
  BAD_CREDENTIALS,
  MARTA,
  SESSION_ENDED,
  SIGN_IN_NEEDED,
  deferredResponse,
  fullProject,
  jsonResponse,
  mockApi,
  mockFetch,
} from '../test/helpers'
import { renderApp } from '../test/renderApp'

/** The calls to the API that are not the check of the session at the start. */
function loginCalls(api: ReturnType<typeof mockApi>) {
  return api.callsTo('POST /auth/login')
}

async function openSignIn(...answers: Parameters<typeof mockFetch>) {
  const api = mockApi({
    'GET /auth/me': jsonResponse(401, SIGN_IN_NEEDED),
    'POST /auth/login': answers.length > 0 ? answers : jsonResponse(200, ANA),
    'GET /dashboard': jsonResponse(200, []),
  })

  renderApp('/sign-in')
  await screen.findByRole('heading', { name: 'Sign in to Matriz' })
  return api
}

describe('the sign in screen', () => {
  it('shows the form and the notice of the demonstration environment (wireframe 1)', async () => {
    await openSignIn()

    expect(screen.getByLabelText('Username')).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toHaveAttribute('type', 'password')
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeEnabled()
    expect(screen.getByText(/all the data is fictional/)).toBeInTheDocument()
  })

  it('asks for both fields when they are empty, without calling the API (W6)', async () => {
    const fetch = await openSignIn()

    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(screen.getByText('Enter your username.')).toBeInTheDocument()
    expect(screen.getByText('Enter your password.')).toBeInTheDocument()
    expect(screen.getByLabelText('Username')).toHaveFocus()
    expect(screen.getByLabelText('Username')).toBeInvalid()
    expect(loginCalls(fetch)).toHaveLength(0)
  })

  it('asks only for the field that is empty, and a username of only spaces is empty', async () => {
    const fetch = await openSignIn()
    await userEvent.type(screen.getByLabelText('Username'), '   ')
    await userEvent.type(screen.getByLabelText('Password'), 'secret')

    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(screen.getByText('Enter your username.')).toBeInTheDocument()
    expect(screen.queryByText('Enter your password.')).not.toBeInTheDocument()
    await userEvent.clear(screen.getByLabelText('Username'))
    await userEvent.type(screen.getByLabelText('Username'), 'ana')
    expect(screen.queryByText('Enter your username.')).not.toBeInTheDocument() // goes away when the user types
    await userEvent.clear(screen.getByLabelText('Password'))
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(screen.getByText('Enter your password.')).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toHaveFocus()
    expect(loginCalls(fetch)).toHaveLength(0)
  })

  it('signs in and shows the dashboard with the name and the role (scenario 10.1)', async () => {
    const fetch = await openSignIn(jsonResponse(200, ANA))

    await userEvent.type(screen.getByLabelText('Username'), '  ana ')
    await userEvent.type(screen.getByLabelText('Password'), 'secret')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByText('Team member', { selector: '.topbar__role' })).toBeInTheDocument()
    expect(loginCalls(fetch)[0].body).toEqual({ username: 'ana', password: 'secret' }) // the name is trimmed, the password is not
  })

  it('shows the message of the API when the credentials are not correct, and keeps what was typed (scenario 10.2)', async () => {
    await openSignIn(jsonResponse(401, BAD_CREDENTIALS))

    await userEvent.type(screen.getByLabelText('Username'), 'ana')
    await userEvent.type(screen.getByLabelText('Password'), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(BAD_CREDENTIALS.message)
    expect(screen.getByLabelText('Username')).toHaveValue('ana')
    expect(screen.getByLabelText('Password')).toHaveValue('wrong')
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeEnabled() // it can be tried again
  })

  it('shows that the server cannot be reached', async () => {
    await openSignIn(new TypeError('Failed to fetch'))

    await userEvent.type(screen.getByLabelText('Username'), 'ana')
    await userEvent.type(screen.getByLabelText('Password'), 'x')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('cannot be reached')
    expect(screen.getByLabelText('Username')).toHaveValue('ana')
  })

  it('does not send the same sign in twice while the request is under way', async () => {
    const pending = deferredResponse()
    const fetch = await openSignIn(pending.fetch)
    await userEvent.type(screen.getByLabelText('Username'), 'ana')
    await userEvent.type(screen.getByLabelText('Password'), 'secret')

    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    const button = screen.getByRole('button', { name: 'Signing in...' })
    expect(button).toBeDisabled()
    await userEvent.click(button)
    await userEvent.type(screen.getByLabelText('Password'), '{Enter}') // Enter in the form does not send it again either

    expect(loginCalls(fetch)).toHaveLength(1)
    pending.respond(jsonResponse(200, ANA))
    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
  })

  it('does not send the same sign in twice even if the form is submitted again by other means', async () => {
    const pending = deferredResponse()
    const fetch = await openSignIn(pending.fetch)
    await userEvent.type(screen.getByLabelText('Username'), 'ana')
    await userEvent.type(screen.getByLabelText('Password'), 'secret')
    const form = screen.getByRole('button', { name: 'Sign in' }).closest('form') as HTMLFormElement

    fireEvent.submit(form) // a submit event does not look at the disabled button
    fireEvent.submit(form)

    expect(loginCalls(fetch)).toHaveLength(1)
    pending.respond(jsonResponse(200, ANA))
    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
  })

  it('tells why the user is here when the session ended (scenario 10.6)', async () => {
    mockFetch(jsonResponse(401, SESSION_ENDED))

    renderApp('/')

    expect(await screen.findByRole('heading', { name: 'Sign in to Matriz' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Your session has ended. Sign in again to continue.')
  })

  it('asks to sign in, without showing the screen, for a direct link without a session (scenario 10.5)', async () => {
    mockFetch(jsonResponse(401, SIGN_IN_NEEDED))

    renderApp('/projects/12')

    expect(await screen.findByRole('heading', { name: 'Sign in to Matriz' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Sign in to continue.')
    expect(screen.queryByText('Page not found')).not.toBeInTheDocument()
  })

  it('goes back to the screen that was asked for after signing in', async () => {
    mockApi({
      'GET /auth/me': jsonResponse(401, SIGN_IN_NEEDED),
      'POST /auth/login': jsonResponse(200, MARTA),
      'GET /projects/12': jsonResponse(200, fullProject(12, 'Website Redesign')),
    })
    renderApp('/projects/12')
    await screen.findByRole('heading', { name: 'Sign in to Matriz' })

    await userEvent.type(screen.getByLabelText('Username'), 'marta')
    await userEvent.type(screen.getByLabelText('Password'), 'secret')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    // the user lands on the screen that was asked for, not on the dashboard
    expect(await screen.findByRole('heading', { name: 'Website Redesign' })).toBeInTheDocument()
  })

  it('sends someone who is already signed in away from the sign in screen', async () => {
    mockApi({ 'GET /auth/me': jsonResponse(200, ANA), 'GET /dashboard': jsonResponse(200, []) })

    renderApp('/sign-in')

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByLabelText('Username')).not.toBeInTheDocument())
  })

  it('shows a spinner while the session is checked', async () => {
    mockFetch(() => new Promise<Response>(() => undefined))

    renderApp('/sign-in')

    expect(screen.getByRole('status')).toHaveTextContent('Checking your session...')
  })
})
