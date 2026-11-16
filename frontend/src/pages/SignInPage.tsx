import { useRef, useState, type FormEvent } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { ApiError } from '../api/errors'
import { useAuth } from '../auth/useAuth'
import { Button } from '../components/Button'
import { Notice } from '../components/Notice'
import { Spinner } from '../components/Spinner'
import { TextField } from '../components/TextField'

/** The messages of the empty fields (W6). */
export const EMPTY_USERNAME = 'Enter your username.'
export const EMPTY_PASSWORD = 'Enter your password.'

interface FieldErrors {
  username?: string
  password?: string
}

/**
 * The sign in screen (F10, US10; wireframe 1): the form with its validation, the message of the API when the
 * credentials are not correct, and the reason when the user comes here because the session ended. If the sign in
 * fails what was typed stays in the form, and while the request is under way the button cannot be pressed again.
 */
export function SignInPage() {
  const { state, signIn } = useAuth()
  const location = useLocation()
  const usernameRef = useRef<HTMLInputElement>(null)
  const passwordRef = useRef<HTMLInputElement>(null)
  const submitting = useRef(false)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  if (state.status === 'loading') {
    return <Spinner label="Checking your session..." />
  }
  if (state.status === 'signedIn') {
    const from = (location.state as { from?: { pathname: string; search: string } } | null)?.from
    return <Navigate to={from ? `${from.pathname}${from.search}` : '/'} replace />
  }
  const sessionMessage = state.message

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (submitting.current) {
      return
    }
    const errors: FieldErrors = {}
    if (username.trim() === '') {
      errors.username = EMPTY_USERNAME
    }
    if (password === '') {
      errors.password = EMPTY_PASSWORD
    }
    setFieldErrors(errors)
    setServerError(null)
    if (errors.username || errors.password) {
      ;(errors.username ? usernameRef : passwordRef).current?.focus()
      return
    }
    submitting.current = true
    setPending(true)
    try {
      await signIn(username.trim(), password) // on success the state changes and this screen redirects
    } catch (cause) {
      setServerError(cause instanceof ApiError ? cause.message : 'Something went wrong. Try again in a moment.')
    } finally {
      submitting.current = false
      setPending(false)
    }
  }

  return (
    <main className="auth">
      <section className="auth__card">
        <h1>Sign in to Matriz</h1>
        {sessionMessage ? <Notice kind="info">{sessionMessage}</Notice> : null}
        {serverError ? <Notice kind="error">{serverError}</Notice> : null}
        <form onSubmit={handleSubmit} noValidate>
          <TextField
            ref={usernameRef}
            label="Username"
            name="username"
            autoComplete="username"
            value={username}
            error={fieldErrors.username}
            onChange={(event) => {
              setUsername(event.target.value)
              setFieldErrors((errors) => ({ ...errors, username: undefined }))
            }}
          />
          <TextField
            ref={passwordRef}
            label="Password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            error={fieldErrors.password}
            onChange={(event) => {
              setPassword(event.target.value)
              setFieldErrors((errors) => ({ ...errors, password: undefined }))
            }}
          />
          <Button type="submit" busy={pending}>
            {pending ? 'Signing in...' : 'Sign in'}
          </Button>
        </form>
        <p className="auth__demo">Demonstration environment: all the data is fictional.</p>
      </section>
    </main>
  )
}
