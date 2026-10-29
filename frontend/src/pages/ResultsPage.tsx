import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiError, searchFlights } from '../api/client'
import type { Cabin, FlightSummaryResponse } from '../api/types'
import { FlightCard } from '../components/FlightCard'
import { dayLabel, groupByDay } from './search/searchCriteria'

export function ResultsPage() {
  const [params] = useSearchParams()
  const origin = params.get('origin') ?? ''
  const destination = params.get('destination') ?? ''
  const from = params.get('from') ?? params.get('date') ?? ''
  const to = params.get('to') || from
  const cabin = (params.get('cabin') ?? undefined) as Cabin | undefined

  const [flights, setFlights] = useState<FlightSummaryResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!origin || !destination || !from) {
      setError('Missing search criteria. Start again from the search page.')
      return
    }

    const controller = new AbortController()
    setFlights(null)
    setError(null)

    searchFlights({ origin, destination, from, to, cabin }, controller.signal)
      .then(setFlights)
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setError(cause instanceof ApiError ? cause.message : 'Could not load flights.')
      })

    return () => controller.abort()
  }, [origin, destination, from, to, cabin])

  const days = groupByDay(flights ?? [])

  return (
    <section>
      <div className="results-header">
        <h1>
          {origin} to {destination}
        </h1>
        <p className="muted">
          {from === to ? from : `${from} to ${to}`}
          {cabin ? ` · ${cabin.replace('_', ' ').toLowerCase()}` : ''}
        </p>
        <Link to="/search">Change search</Link>
      </div>

      {error && <p className="error">{error}</p>}
      {flights === null && !error && <p className="muted">Loading flights…</p>}
      {flights?.length === 0 && <p className="muted">No flights match this search.</p>}

      {days.map(([day, dayFlights]) => (
        <section key={day} className="flight-day">
          <h2 className="flight-day-heading">
            {dayLabel(`${day}T12:00:00Z`, 'UTC')}
            <span className="muted"> · {dayFlights.length} flight{dayFlights.length === 1 ? '' : 's'}</span>
          </h2>
          <ul className="flight-list">
            {dayFlights.map((flight) => (
              <li key={flight.flightId}>
                <FlightCard flight={flight} cabinFilter={cabin} />
              </li>
            ))}
          </ul>
        </section>
      ))}
    </section>
  )
}
