import { TaskLink } from './TaskLink'
import { QUADRANTS, type Quadrant, type Task } from '../api/tasks'
import { StatusBadge } from '../components/StatusBadge'
import { isFinal, quadrantOf } from './taskView'

const DESCRIPTIONS: Record<Quadrant, string> = {
  'Do first': 'urgent and important',
  Schedule: 'important, not urgent',
  Delegate: 'urgent, not important',
  Later: 'not urgent, not important',
}

/**
 * The Matrix (F04; matrix wireframe): the open main tasks by urgency (columns) and importance (rows). The tasks in a
 * final status are hidden, because the API returns them in the list (P10). The API gives them by deadline, so each
 * quadrant keeps that order.
 */
export function TaskMatrix({ tasks }: { tasks: Task[] }) {
  const open = tasks.filter((task) => !isFinal(task))
  return (
    <div className="matrix">
      <p className="matrix__axis matrix__axis--urgency">
        <span>Urgency: High</span>
        <span>Urgency: Low</span>
      </p>
      <div className="matrix__grid">
        {QUADRANTS.map((quadrant) => {
          const inQuadrant = open.filter((task) => quadrantOf(task) === quadrant)
          return (
            <section key={quadrant} className="quadrant" aria-label={quadrant}>
              <h3 className="quadrant__title">
                {quadrant} <span className="quadrant__hint">{DESCRIPTIONS[quadrant]}</span>
              </h3>
              {inQuadrant.length === 0 ? (
                <p className="empty">No tasks</p>
              ) : (
                <ul className="quadrant__cards">
                  {inQuadrant.map((task) => (
                    <li key={task.id} className="task-card">
                      <TaskLink id={task.id} title={task.title} />
                      <div className="task-card__meta">
                        <StatusBadge value={task.status} /> {task.deadline}
                      </div>
                      <div className="task-card__meta">
                        {task.assignees.map((person) => person.name).join(', ')}
                        {task.subtasks && task.subtasks.length > 0 ? ` · ${task.subtasks.length} subtasks` : ''}
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          )
        })}
      </div>
      <p className="matrix__axis matrix__axis--importance">Rows: importance, high above, low below</p>
    </div>
  )
}
