import type { Cabin, FlightSummaryResponse } from '../../api/types'

export const CABINS: { value: '' | Cabin; label: string }[] = [
  { value: '', label: 'Any cabin' },
  { value: 'ECONOMY', label: 'Economy' },
  { value: 'PREMIUM_ECONOMY', label: 'Premium economy' },
  { value: 'BUSINESS', label: 'Business' },
  { value: 'FIRST', label: 'First' },
]

export const FLEXIBILITY = [
  { value: 0, label: 'Exact date' },
  { value: 1, label: '± 1 day' },
  { value: 3, label: '± 3 days' },
  { value: 7, label: '± 1 week' },
  { value: 14, label: '± 2 weeks' },
]

export const APP_TIME_ZONE = 'Europe/Warsaw'

export type TzResolver = (flight: FlightSummaryResponse) => string

export const flightTz: TzResolver = (flight) => flight.departureTimeZone ?? APP_TIME_ZONE

const dayFormats = new Map<string, Intl.DateTimeFormat>()

function dayFormat(timeZone: string): Intl.DateTimeFormat {
  let format = dayFormats.get(timeZone)
  if (!format) {
    format = new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit' })
    dayFormats.set(timeZone, format)
  }
  return format
}

export function dayKey(instant: Date | string, timeZone: string = APP_TIME_ZONE): string {
  const date = typeof instant === 'string' ? new Date(instant) : instant
  return dayFormat(timeZone).format(date)
}

export function tomorrow(): string {
  return shift(dayKey(new Date()), 1)
}

export function shift(date: string, days: number): string {
  const shifted = new Date(`${date}T12:00:00Z`)
  shifted.setUTCDate(shifted.getUTCDate() + days)
  return dayKey(shifted, 'UTC')
}

export function withRange(date: string, flexibility: number): { from: string; to: string } {
  const earliest = tomorrow()
  const from = shift(date, -flexibility)
  return { from: from < earliest ? earliest : from, to: shift(date, flexibility) }
}

export function flexibilityOf(from: string, to: string): number {
  if (!from) return 0
  const days = Math.round(
    (new Date(`${to}T12:00:00Z`).getTime() - new Date(`${from}T12:00:00Z`).getTime()) / 86400000,
  )
  return days >= 0 ? days : 0
}

export function groupByDay(
  flights: FlightSummaryResponse[],
  timeZoneOf: TzResolver = flightTz,
): [string, FlightSummaryResponse[]][] {
  const days = new Map<string, FlightSummaryResponse[]>()
  for (const flight of flights) {
    const key = dayKey(flight.departureAt, timeZoneOf(flight))
    const bucket = days.get(key) ?? []
    bucket.push(flight)
    days.set(key, bucket)
  }
  return [...days.entries()]
}

export function dayLabel(instant: Date | string, timeZone: string = APP_TIME_ZONE): string {
  const date = typeof instant === 'string' ? new Date(instant) : instant
  return new Intl.DateTimeFormat(undefined, {
    timeZone,
    weekday: 'long',
    day: 'numeric',
    month: 'short',
  }).format(date)
}
