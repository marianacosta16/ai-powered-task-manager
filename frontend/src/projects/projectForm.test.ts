import { afterEach, describe, expect, it, vi } from 'vitest'
import { EMPTY_VALUES, today, validate } from './projectForm'

afterEach(() => vi.useRealTimers())

describe('today (R13, R29, P26)', () => {
  it('writes yyyy-mm-dd with the zeros, in the zone asked', () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-01-05T23:30:00Z'))

    expect(today('UTC')).toBe('2026-01-05')
    expect(today('Pacific/Kiritimati')).toBe('2026-01-06')
  })

  it('is the browser day without a time zone, and also for one the browser does not know', () => {
    expect(today()).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    expect(today('Not/AZone')).toBe(today())
  })

  it('is the day in the time zone asked, which can be another day than the browser one', () => {
    expect(today('Pacific/Kiritimati')).not.toBe(today('Pacific/Pago_Pago')) // UTC+14 and UTC-11
    expect(today('UTC')).toBe(new Date().toISOString().slice(0, 10))
  })

  it('checks the start date of a new project against today in the time zone of the project (R29)', () => {
    const base = { ...EMPTY_VALUES, name: 'Intranet', endDate: '2099-01-01' }
    const ahead = { ...base, timezone: 'Pacific/Kiritimati', startDate: today('Pacific/Pago_Pago') } // past there
    const behind = { ...base, timezone: 'Pacific/Pago_Pago', startDate: today('Pacific/Pago_Pago') } // today there

    expect(validate(ahead, null).startDate).toMatch(/past/)
    expect(validate(behind, null).startDate).toBeUndefined()
  })
})
