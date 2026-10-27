import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ApiError, getSeatMap } from '../api/client'
import type { Cabin, SeatMapResponse } from '../api/types'
import { SeatGrid } from '../components/SeatGrid'

const formatter = new Intl.NumberFormat(undefined, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

export function SeatMapPage() {
  const navigate = useNavigate()
  const { flightId } = useParams<{ flightId: string }>()
  const [params] = useSearchParams()
  const cabin = params.get('cabin') as Cabin | null

  const [seatMap, setSeatMap] = useState<SeatMapResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!flightId) {
      setError('Missing flight.')
      return
    }

    const controller = new AbortController()
    setSeatMap(null)
    setError(null)

    getSeatMap(flightId, controller.signal)
      .then(setSeatMap)
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setError(cause instanceof ApiError ? cause.message : 'Could not load the seat map.')
      })

    return () => controller.abort()
  }, [flightId])

  if (error) {
    return (
      <section>
        <p className="error">{error}</p>
        <Link to="/">Back to search</Link>
      </section>
    )
  }

  if (!seatMap) return <p className="muted">Loading seat map…</p>

  const visibleFares = cabin ? seatMap.fares.filter((fare) => fare.cabin === cabin) : seatMap.fares
  const visibleSeats = cabin
    ? seatMap.seats.filter((seat) => seat.cabin === cabin)
    : seatMap.seats

  return (
    <section>
      <div className="results-header">
        <h1>
          {seatMap.flightNumber} · {seatMap.origin} to {seatMap.destination}
        </h1>
        <p className="muted">
          {new Date(seatMap.departureAt).toLocaleString()} · {seatMap.aircraft} ·{' '}
          {seatMap.seatRows} rows
        </p>
        <button type="button" className="link-button" onClick={() => navigate(-1)}>
          Back to results
        </button>
      </div>

      <ul className="fare-summary">
        {visibleFares.map((fare) => (
          <li key={fare.cabin}>
            <strong>{fare.cabin.replace('_', ' ').toLowerCase()}</strong>{' '}
            <span className="muted">
              from {formatter.format(fare.price)} {fare.currency}
            </span>
          </li>
        ))}
      </ul>

      <div className="legend">
        <span className="seat seat-available legend-swatch" aria-hidden="true" /> available
        <span className="seat seat-held legend-swatch" aria-hidden="true" /> on hold
        <span className="seat seat-sold legend-swatch" aria-hidden="true" /> sold
      </div>

      <SeatGrid seats={visibleSeats} />

      <p className="muted">Seat selection and holds arrive with the reservation API.</p>
    </section>
  )
}
