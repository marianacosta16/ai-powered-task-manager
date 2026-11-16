import { useCallback, useEffect, useRef, useState, type DependencyList } from 'react'
import { ApiError } from '../api/errors'

export type LoadState<T> =
  | { status: 'loading' }
  | { status: 'ready'; data: T }
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
  loadRef.current = load

  useEffect(() => {
    const controller = new AbortController()
    if (!silent.current) {
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
        setState({
          status: 'error',
          message: cause instanceof ApiError ? cause.message : 'Something went wrong. Try again in a moment.',
        })
      })
    return () => controller.abort()
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
