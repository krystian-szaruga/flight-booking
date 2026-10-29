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

export function tomorrow(): string {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  return date.toISOString().slice(0, 10)
}

export function shift(date: string, days: number): string {
  const shifted = new Date(`${date}T00:00:00`)
  shifted.setDate(shifted.getDate() + days)
  return shifted.toISOString().slice(0, 10)
}

export function withRange(date: string, flexibility: number): { from: string; to: string } {
  const earliest = tomorrow()
  const from = shift(date, -flexibility)
  return { from: from < earliest ? earliest : from, to: shift(date, flexibility) }
}

export function flexibilityOf(from: string, to: string): number {
  if (!from) return 0
  const days = Math.round(
    (new Date(`${to}T00:00:00`).getTime() - new Date(`${from}T00:00:00`).getTime()) / 86400000,
  )
  return days >= 0 ? days : 0
}

export function groupByDay(flights: FlightSummaryResponse[]): [string, FlightSummaryResponse[]][] {
  const days = new Map<string, FlightSummaryResponse[]>()
  for (const flight of flights) {
    const key = new Date(flight.departureAt).toISOString().slice(0, 10)
    const bucket = days.get(key) ?? []
    bucket.push(flight)
    days.set(key, bucket)
  }
  return [...days.entries()]
}

export function dayLabel(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'short' })
}
