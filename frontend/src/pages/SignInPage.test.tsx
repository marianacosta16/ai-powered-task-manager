import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ANA, BAD_CREDENTIALS, MARTA, SESSION_ENDED, SIGN_IN_NEEDED, jsonResponse, mockFetch } from '../test/helpers'
import { renderApp } from '../test/renderApp'

/** The calls to the API that are not the check of the session at the start. */
function loginCalls(fetch: ReturnType<typeof mockFetch>) {
  return fetch.mock.calls.filter((call) => String((call as unknown[])[0]).endsWith('/auth/login'))
}

async function openSignIn(...answers: Parameters<typeof mockFetch>) {
  const fetch = mockFetch(jsonResponse(401, SIGN_IN_NEEDED), ...answers)
  renderApp('/sign-in')
  await screen.findByRole('heading', { name: 'Sign in to Matriz' })
  return fetch
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

  it('signs in and shows the home screen with the name and the role (scenario 10.1)', async () => {
    const fetch = await openSignIn(jsonResponse(200, ANA))

    await userEvent.type(screen.getByLabelText('Username'), '  ana ')
    await userEvent.type(screen.getByLabelText('Password'), 'secret')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'Home' })).toBeInTheDocument()
    expect(screen.getByText('Welcome, Ana. You are signed in as Team member.')).toBeInTheDocument()
    expect(screen.getByText('Team member', { selector: '.topbar__role' })).toBeInTheDocument()
    const [url, init] = loginCalls(fetch)[0] as unknown as [string, RequestInit]
    expect(url).toMatch(/\/auth\/login$/)
    expect(init.body).toBe('{"username":"ana","password":"secret"}') // the name is trimmed, the password is not
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
    let finish: (response: Response) => void = () => undefined
    const pending = new Promise<Response>((resolve) => {
      finish = resolve
    })
    const fetch = await openSignIn(() => pending)
    await userEvent.type(screen.getByLabelText('Username'), 'ana')
    await userEvent.type(screen.getByLabelText('Password'), 'secret')

    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    const button = screen.getByRole('button', { name: 'Signing in...' })
    expect(button).toBeDisabled()
    await userEvent.click(button)
    await userEvent.type(screen.getByLabelText('Password'), '{Enter}') // Enter in the form does not send it again either

    expect(loginCalls(fetch)).toHaveLength(1)
    finish(jsonResponse(200, ANA))
    expect(await screen.findByRole('heading', { name: 'Home' })).toBeInTheDocument()
  })

  it('does not send the same sign in twice even if the form is submitted again by other means', async () => {
    let finish: (response: Response) => void = () => undefined
    const pending = new Promise<Response>((resolve) => {
      finish = resolve
    })
    const fetch = await openSignIn(() => pending)
    await userEvent.type(screen.getByLabelText('Username'), 'ana')
    await userEvent.type(screen.getByLabelText('Password'), 'secret')
    const form = screen.getByRole('button', { name: 'Sign in' }).closest('form') as HTMLFormElement

    fireEvent.submit(form) // a submit event does not look at the disabled button
    fireEvent.submit(form)

    expect(loginCalls(fetch)).toHaveLength(1)
    finish(jsonResponse(200, ANA))
    expect(await screen.findByRole('heading', { name: 'Home' })).toBeInTheDocument()
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
    mockFetch(jsonResponse(401, SIGN_IN_NEEDED), jsonResponse(200, MARTA))
    renderApp('/projects/12')
    await screen.findByRole('heading', { name: 'Sign in to Matriz' })

    await userEvent.type(screen.getByLabelText('Username'), 'marta')
    await userEvent.type(screen.getByLabelText('Password'), 'secret')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    // the path /projects/12 has no screen yet: the user lands on it, not on the home screen
    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  })

  it('sends someone who is already signed in away from the sign in screen', async () => {
    mockFetch(jsonResponse(200, ANA))

    renderApp('/sign-in')

    expect(await screen.findByRole('heading', { name: 'Home' })).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByLabelText('Username')).not.toBeInTheDocument())
  })

  it('shows a spinner while the session is checked', async () => {
    mockFetch(() => new Promise<Response>(() => undefined))

    renderApp('/sign-in')

    expect(screen.getByRole('status')).toHaveTextContent('Checking your session...')
  })
})
