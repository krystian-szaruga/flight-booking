import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiError, searchFlights } from '../api/client'
import type { Cabin, FlightSummaryResponse } from '../api/types'
import { FlightCard } from '../components/FlightCard'

export function ResultsPage() {
  const [params] = useSearchParams()
  const origin = params.get('origin') ?? ''
  const destination = params.get('destination') ?? ''
  const date = params.get('date') ?? ''
  const cabin = (params.get('cabin') ?? undefined) as Cabin | undefined

  const [flights, setFlights] = useState<FlightSummaryResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!origin || !destination || !date) {
      setError('Missing search criteria. Start again from the search page.')
      return
    }

    const controller = new AbortController()
    setFlights(null)
    setError(null)

    searchFlights({ origin, destination, date, cabin }, controller.signal)
      .then(setFlights)
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setError(cause instanceof ApiError ? cause.message : 'Could not load flights.')
      })

    return () => controller.abort()
  }, [origin, destination, date, cabin])

  return (
    <section>
      <div className="results-header">
        <h1>
          {origin} to {destination}
        </h1>
        <p className="muted">
          {date}
          {cabin ? ` · ${cabin.replace('_', ' ').toLowerCase()}` : ''}
        </p>
        <Link to="/">Change search</Link>
      </div>

      {error && <p className="error">{error}</p>}
      {flights === null && !error && <p className="muted">Loading flights…</p>}
      {flights?.length === 0 && <p className="muted">No flights match this search.</p>}

      <ul className="flight-list">
        {flights?.map((flight) => (
          <li key={flight.flightId}>
            <FlightCard flight={flight} cabinFilter={cabin} />
          </li>
        ))}
      </ul>
    </section>
  )
}
