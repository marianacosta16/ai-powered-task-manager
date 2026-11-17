import { describe, expect, it } from 'vitest'
import type { Task } from '../api/tasks'
import { task } from '../test/helpers'
import { changes, isDirty, mergeAfterConflict, statusChoices, valuesOf } from './taskEdit'

const make = (overrides: Record<string, unknown> = {}) => task(10, 'A task', overrides) as unknown as Task

describe('what changed in a task (PATCH)', () => {
  it('is nothing when nothing was changed, also for a description that is null or empty', () => {
    const base = make({ description: null })
    expect(changes(valuesOf(base), base)).toEqual({})
    expect(isDirty({ ...valuesOf(base), description: '   ' }, base)).toBe(false)
  })

  it('clears the description with null and trims the texts', () => {
    const base = make({ description: 'Old' })
    expect(changes({ ...valuesOf(base), description: '  ' }, base)).toEqual({ description: null })
    expect(changes({ ...valuesOf(base), title: '  New title ', description: ' New ' }, base)).toEqual({ title: 'New title', description: 'New' })
  })

  it('sends the levels as numbers', () => {
    const base = make()
    expect(changes({ ...valuesOf(base), urgency: '1', importance: '2' }, base)).toEqual({ urgency: 1, importance: 2 })
  })

  it('sends the block reason with the status Blocked, also when only the reason changes, and never otherwise (A17)', () => {
    const blocked = make({ status: 'Blocked', blockReason: 'Waiting' })
    expect(changes({ ...valuesOf(blocked), blockReason: ' Waiting more ' }, blocked)).toEqual({ blockReason: 'Waiting more' })
    expect(changes(valuesOf(blocked), blocked)).toEqual({})
    const open = make()
    expect(changes({ ...valuesOf(open), blockReason: 'ignored' }, open)).toEqual({})
  })

  it('sends the original task with the status Duplicate, also when only the original changes, and never otherwise (A17)', () => {
    const dup = make({ status: 'Duplicate', originalTaskId: 5 })
    expect(changes({ ...valuesOf(dup), originalTaskId: '6' }, dup)).toEqual({ originalTaskId: 6 })
    expect(changes(valuesOf(dup), dup)).toEqual({})
    const open = make()
    expect(changes({ ...valuesOf(open), originalTaskId: '7' }, open)).toEqual({})
  })
})

describe('the statuses a person can choose (P8)', () => {
  it('limits a member to the final ones on a closed task, and gives all to the manager and on an open task', () => {
    expect(statusChoices('Done', false)).toEqual(['Done', 'Cancelled', 'Duplicate'])
    expect(statusChoices('Done', true)).toHaveLength(7)
    expect(statusChoices('Blocked', false)).toHaveLength(7)
  })
})

describe('going on after a conflict (R30)', () => {
  const base = make({ title: 'Base', description: 'Base text', urgency: 3 })

  it('keeps the fields the user changed and takes the others from the server', () => {
    const fresh = make({ title: 'Theirs', description: 'Base text', urgency: 1, version: 2 })
    const mine = { ...valuesOf(base), description: 'Mine' }

    const merged = mergeAfterConflict(mine, base, fresh)

    expect(merged.values).toEqual({ ...valuesOf(fresh), description: 'Mine' })
    expect(merged.alsoChanged).toEqual([])
  })

  it('lists the fields both changed, in the order of the form, and not the ones that ended up equal', () => {
    const fresh = make({ title: 'Theirs', description: 'Both', urgency: 1, version: 2 })
    const mine = { ...valuesOf(base), title: 'Mine', description: 'Both', urgency: '2' }

    const merged = mergeAfterConflict(mine, base, fresh)

    expect(merged.alsoChanged).toEqual(['Task title', 'Urgency']) // the description is the same on both sides
    expect(merged.values.title).toBe('Mine')
    expect(merged.values.urgency).toBe('2')
  })
})
