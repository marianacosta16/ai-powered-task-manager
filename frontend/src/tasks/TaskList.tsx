import { Link } from 'react-router-dom'
import type { Task } from '../api/tasks'
import { StatusBadge } from '../components/StatusBadge'
import { levelLabel } from './taskView'

/** The table of the main tasks (F05, wireframe 4). The text of a task is shown as it is, never as HTML (V6, NF09). */
export function TaskList({ tasks }: { tasks: Task[] }) {
  return (
    <table className="table">
      <thead>
        <tr>
          <th scope="col">Title</th>
          <th scope="col">Subtasks</th>
          <th scope="col">Status</th>
          <th scope="col">Deadline</th>
          <th scope="col">Urgency</th>
          <th scope="col">Importance</th>
          <th scope="col">Assignees</th>
        </tr>
      </thead>
      <tbody>
        {tasks.map((task) => (
          <tr key={task.id}>
            <td data-label="Title">
              <Link to={`/tasks/${task.id}`}>{task.title}</Link>
            </td>
            <td data-label="Subtasks">{task.subtasks?.length ?? 0}</td>
            <td data-label="Status">
              <StatusBadge value={task.status} />
            </td>
            <td data-label="Deadline">{task.deadline}</td>
            <td data-label="Urgency">{levelLabel(task.urgency)}</td>
            <td data-label="Importance">{levelLabel(task.importance)}</td>
            <td data-label="Assignees">{task.assignees.map((person) => person.name).join(', ')}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
