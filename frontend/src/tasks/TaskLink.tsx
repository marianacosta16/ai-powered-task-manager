import { Link, useLocation } from 'react-router-dom'

/**
 * The link to a task. It tells the task page where the user came from (the Matrix or the List, with its filters), so
 * the way back goes there.
 */
export function TaskLink({ id, title }: { id: number; title: string }) {
  const here = useLocation()
  return (
    <Link to={`/tasks/${id}`} state={{ from: here.pathname + here.search }}>
      {title}
    </Link>
  )
}
