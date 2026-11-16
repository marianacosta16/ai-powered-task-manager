import { describe, expect, it } from 'vitest'
import { task } from '../test/helpers'
import type { Task } from '../api/tasks'
import { quadrantOf } from './taskView'

const make = (overrides: Record<string, unknown>) => task(1, 'x', overrides) as unknown as Task

describe('the quadrant of a task (F04, R25)', () => {
  it('uses the one the API gives', () => {
    expect(quadrantOf(make({ quadrant: 'Later', urgency: 4, importance: 4 }))).toBe('Later')
  })

  it('calculates it when it is missing: levels 3 and 4 are high, 1 and 2 are low', () => {
    expect(quadrantOf(make({ quadrant: undefined, urgency: 3, importance: 3 }))).toBe('Do first')
    expect(quadrantOf(make({ quadrant: undefined, urgency: 2, importance: 4 }))).toBe('Schedule')
    expect(quadrantOf(make({ quadrant: undefined, urgency: 4, importance: 2 }))).toBe('Delegate')
    expect(quadrantOf(make({ quadrant: undefined, urgency: 2, importance: 2 }))).toBe('Later')
  })
})
