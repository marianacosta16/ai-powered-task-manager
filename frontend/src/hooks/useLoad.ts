import { useCallback, useEffect, useRef, useState, type DependencyList } from 'react'
import { ApiError } from '../api/errors'

export type LoadState<T> =
  | { status: 'loading' }
  | { status: 'ready'; data: T; refreshError?: string }
  | { status: 'error'; message: string }

/**
 * Loads data from the API for a screen, with the four states every list has (loading, error, and the data, which the
 * screen shows as empty or not). When the inputs change it goes back to loading; `reload` fetches again without the
 * spinner, so the screen keeps showing what it has until the new answer arrives (the lists update without reloading
 * the page). A request that is no longer wanted is aborted.
 */
export function useLoad<T>(load: (signal: AbortSignal) => Promise<T>, inputs: DependencyList) {
  const [state, setState] = useState<LoadState<T>>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const silent = useRef(false)
  const loadRef = useRef(load)

  // The latest `load`, kept in an effect (not in render). It runs before the effect below, which is declared after it.
  useEffect(() => {
    loadRef.current = load
  })

  useEffect(() => {
    const controller = new AbortController()
    const wasSilent = silent.current
    if (!wasSilent) {
      setState({ status: 'loading' })
    }
    silent.current = false
    loadRef
      .current(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setState({ status: 'ready', data })
        }
      })
      .catch((cause: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        const message = cause instanceof ApiError ? cause.message : 'Something went wrong. Try again in a moment.'
        // A refresh that fails keeps what is on screen and says so; only a first load becomes an error screen
        setState((current) => (wasSilent && current.status === 'ready' ? { ...current, refreshError: message } : { status: 'error', message }))
      })
    return () => controller.abort()
    // `inputs` is the dependency list the caller chose (a spread, which the rule cannot check); `load` is read through loadRef
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...inputs, attempt])

  const reload = useCallback(() => {
    silent.current = true
    setAttempt((n) => n + 1)
  }, [])
  const retry = useCallback(() => {
    silent.current = false
    setAttempt((n) => n + 1)
  }, [])
  return { state, reload, retry }
}
