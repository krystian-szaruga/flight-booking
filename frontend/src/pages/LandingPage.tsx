import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, getDeals } from '../api/client'
import type { DealResponse } from '../api/types'
import { DealCard } from '../components/DealCard'

export function LandingPage() {
  const [deals, setDeals] = useState<DealResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()

    getDeals(6, controller.signal)
      .then(setDeals)
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setError(cause instanceof ApiError ? cause.message : 'Could not load deals.')
      })

    return () => controller.abort()
  }, [])

  return (
    <section>
      <div className="hero">
        <p className="hero-eyebrow">Cheapest upcoming</p>
        <h1>Where can you fly for the least?</h1>
        <p className="muted">
          The six best prices across the whole catalogue from now on, wherever they depart.
        </p>
        <Link to="/search" className="button">
          Search flights
        </Link>
      </div>

      {error && <p className="error">{error}</p>}
      {deals === null && !error && <p className="muted">Loading deals…</p>}
      {deals?.length === 0 && (
        <p className="muted">
          No upcoming flights with free seats in the catalogue. Load some data and try again.
        </p>
      )}

      <ul className="deal-grid">
        {deals?.map((deal) => (
          <li key={`${deal.flightId}-${deal.cabin}`}>
            <DealCard deal={deal} />
          </li>
        ))}
      </ul>
    </section>
  )
}
