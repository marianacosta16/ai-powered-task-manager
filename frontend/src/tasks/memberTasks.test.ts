import { describe, expect, it } from 'vitest'
import type { Task } from '../api/tasks'
import { ANA, MARTA, task } from '../test/helpers'
import { openTasksOf, substitutesFor, type Substitute } from './memberTasks'

const RUI = { id: 4, name: 'Rui', role: 'Team member' }
const make = (id: number, overrides: Record<string, unknown> = {}) => task(id, `T${id}`, overrides) as unknown as Task

describe('the open tasks of a member (R17)', () => {
  it('has the open tasks and subtasks they are assigned to, shared ones included', () => {
    const tasks = [
      make(1, { assignees: [ANA, RUI] }),
      make(2, { assignees: [RUI] }),
      make(3, { assignees: [MARTA], subtasks: [make(4, { parentTaskId: 3, assignees: [ANA] })] }),
    ]

    expect(openTasksOf(ANA.id, tasks)).toEqual([
      { id: 1, title: 'T1', assigneeIds: [ANA.id, RUI.id] },
      { id: 4, title: 'T4', parentTitle: 'T3', assigneeIds: [ANA.id] },
    ])
  })

  it('leaves out the tasks and subtasks in a final status', () => {
    const tasks = [
      make(1, { status: 'Done' }),
      make(2, { status: 'Cancelled', subtasks: [make(3, { parentTaskId: 2, status: 'Duplicate' })] }),
      make(5, { status: 'Blocked', subtasks: [make(6, { parentTaskId: 5, status: 'Done' }), make(7, { parentTaskId: 5, status: 'In review' })] }),
    ]

    expect(openTasksOf(ANA.id, tasks).map((t) => t.id)).toEqual([5, 7])
  })
})

describe('who can take a task over (R17, P24)', () => {
  const everyone: Substitute[] = [
    { id: 4, name: 'Rui', openTasks: 4, isNew: false },
    { id: 5, name: 'Luis', openTasks: 0, isNew: false },
    { id: 6, name: 'Zé', openTasks: 0, isNew: true },
  ]
  const open = { id: 10, title: 'T', assigneeIds: [5] }

  it('leaves out who is already assigned to that task', () => {
    expect(substitutesFor(open, everyone, {}, 5).map((s) => s.id)).toEqual([4, 6])
  })

  it('leaves out who would pass the limit, counting what was chosen for other tasks but not for this one', () => {
    expect(substitutesFor(open, everyone, { 11: 4 }, 5).map((s) => s.id)).toEqual([6]) // Rui: 4 + 1 chosen + 1 is 6
    expect(substitutesFor(open, everyone, { 10: 4 }, 5).map((s) => s.id)).toEqual([4, 6]) // the choice for this task does not count
    expect(substitutesFor(open, everyone, { 11: 4 }, 6).map((s) => s.id)).toEqual([4, 6]) // exactly at the limit is fine
  })
})
