import type { ReactNode } from 'react'

/** What a list shows when there is nothing to list, with what the user can do next. */
export function EmptyState({ children }: { children: ReactNode }) {
  return <p className="empty">{children}</p>
}
