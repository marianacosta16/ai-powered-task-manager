import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

/**
 * The message that the previous screen sent in the state of the navigation (for example "the project was deleted").
 * It is read on the first render and then taken out of the history, because a reload or going back would bring it
 * again: it is shown once.
 */
export function useNavigationMessage(): string | null {
  const location = useLocation()
  const navigate = useNavigate()
  const [text] = useState(() => {
    const message = (location.state as { message?: unknown } | null)?.message
    return typeof message === 'string' ? message : null
  })
  useEffect(() => {
    if ((location.state as { message?: unknown } | null)?.message !== undefined) {
      navigate(location.pathname + location.search, { replace: true, state: null })
    }
  }, [location, navigate])
  return text
}
