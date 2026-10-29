import { Link } from 'react-router-dom'
import type { Cabin, FlightSummaryResponse } from '../../api/types'
import { FlightCard } from '../../components/FlightCard'
import { dayLabel, groupByDay } from './searchCriteria'

export function SearchResults({
  origin,
  destination,
  from,
  to,
  cabin,
  flights,
  error,
}: {
  origin: string
  destination: string
  from: string
  to: string
  cabin: '' | Cabin
  flights: FlightSummaryResponse[] | null
  error: string | null
}) {
  return (
    <section className="search-results">
      <div className="results-header">
        <h2>
          {origin} to {destination}
        </h2>
        <p className="muted">
          {from === to ? from : `${from} to ${to}`}
          {cabin ? ` · ${cabin.replace('_', ' ').toLowerCase()}` : ''} · <Link to="/search">Clear results</Link>
        </p>
      </div>

      {error && <p className="error">{error}</p>}
      {flights === null && !error && <p className="muted">Loading flights…</p>}
      {flights?.length === 0 && <p className="muted">No flights match this search.</p>}

      {flights !== null &&
        groupByDay(flights).map(([day, dayFlights]) => (
          <section key={day} className="flight-day">
            <h3 className="flight-day-heading">
              {dayLabel(`${day}T12:00:00Z`, 'UTC')}
              <span className="muted">
                {' '}
                · {dayFlights.length} flight{dayFlights.length === 1 ? '' : 's'}
              </span>
            </h3>
            <ul className="flight-grid">
              {dayFlights.map((flight) => (
                <li key={flight.flightId}>
                  <FlightCard flight={flight} cabinFilter={cabin || undefined} />
                </li>
              ))}
            </ul>
          </section>
        ))}
    </section>
  )
}
