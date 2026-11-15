import type { ReactNode } from 'react'

interface NoticeProps {
  kind?: 'info' | 'success' | 'error'
  children: ReactNode
}

/** A message for the whole screen: an error of the API, a session that ended, something that worked. */
export function Notice({ kind = 'info', children }: NoticeProps) {
  return (
    <div className={`notice notice--${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}
