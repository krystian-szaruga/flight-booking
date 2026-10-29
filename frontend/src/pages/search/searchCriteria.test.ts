import { describe, expect, it } from 'vitest'
import { flight } from '../../test/fixtures'
import {
  APP_TIME_ZONE,
  dayKey,
  dayLabel,
  groupByDay,
  shift,
  tomorrow,
  withRange,
} from './searchCriteria'

describe('day keys', () => {
  it('when the same instant is seen from two time zones then the day keys differ', () => {
    const lateEvening = '2026-11-10T23:30:00Z'

    expect(dayKey(lateEvening, 'Europe/Warsaw')).toBe('2026-11-11')
    expect(dayKey(lateEvening, 'America/Los_Angeles')).toBe('2026-11-10')
  })

  it('when a flight carries its own zone then the resolver decides which day it lands in', () => {
    const flights = [
      flight({ flightId: 'waw', origin: 'WAW', departureAt: '2026-11-10T23:30:00Z' }),
      flight({ flightId: 'lis', origin: 'LIS', departureAt: '2026-11-10T23:30:00Z' }),
    ]

    const groups = groupByDay(flights, (item) => (item.origin === 'LIS' ? 'Europe/Lisbon' : 'Europe/Warsaw'))

    expect(groups.map(([day, items]) => [day, items.map((item) => item.flightId)])).toEqual([
      ['2026-11-11', ['waw']],
      ['2026-11-10', ['lis']],
    ])
  })

  it('when the app zone is used then flights on either side of midnight land in the app zone day', () => {
    const flights = [
      flight({ flightId: 'late', departureAt: '2026-11-10T23:30:00Z' }),
      flight({ flightId: 'early', departureAt: '2026-11-11T00:30:00Z' }),
    ]

    const groups = groupByDay(flights, () => APP_TIME_ZONE)

    expect(groups.length).toBe(1)
    expect(groups[0][0]).toBe('2026-11-11')
    expect(groups[0][1].map((item) => item.flightId)).toEqual(['late', 'early'])
  })

  it('when the clock moves to summer time then shifting still counts single calendar days', () => {
    expect(shift('2026-03-28', 1)).toBe('2026-03-29')
    expect(shift('2026-03-29', 1)).toBe('2026-03-30')
    expect(shift('2026-10-24', 1)).toBe('2026-10-25')
    expect(shift('2026-10-25', 1)).toBe('2026-10-26')
    expect(shift('2026-10-26', -1)).toBe('2026-10-25')
  })

  it('when a range is built around a future date then both bounds are plain calendar dates', () => {
    const date = shift(tomorrow(), 10)

    expect(withRange(date, 3)).toEqual({ from: shift(date, -3), to: shift(date, 3) })
    expect(tomorrow()).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })

  it('when a date in the past is used then the range never starts before tomorrow', () => {
    const past = shift(tomorrow(), -20)

    expect(withRange(past, 3).from).toBe(tomorrow())
  })

  it('when a day label is formatted then it names the day in the requested zone', () => {
    expect(dayLabel('2026-11-10T23:30:00Z', 'Europe/Warsaw')).toContain('Nov 11')
    expect(dayLabel('2026-11-10T23:30:00Z', 'America/Los_Angeles')).toContain('Nov 10')
  })
})
