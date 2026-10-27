import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ApiError, cancelReservation, getSeatMap, holdSeats } from '../api/client'
import type { Cabin, GroundedSeat, ReservationResponse, SeatMapResponse, SeatResponse } from '../api/types'
import { HoldPanel } from '../components/HoldPanel'
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
  const [selected, setSelected] = useState<ReadonlySet<string>>(new Set())
  const [reservation, setReservation] = useState<ReservationResponse | null>(null)
  const [holdError, setHoldError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const load = useCallback(
    (signal?: AbortSignal) => {
      if (!flightId) {
        setError('Missing flight.')
        return
      }
      setSeatMap(null)
      setError(null)
      getSeatMap(flightId, signal)
        .then(setSeatMap)
        .catch((cause: unknown) => {
          if (cause instanceof DOMException && cause.name === 'AbortError') return
          setError(cause instanceof ApiError ? cause.message : 'Could not load the seat map.')
        })
    },
    [flightId],
  )

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
    return () => controller.abort()
  }, [load])

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
  const visibleSeats = cabin ? seatMap.seats.filter((seat) => seat.cabin === cabin) : seatMap.seats
  const selectedSeats = visibleSeats.filter((seat) => selected.has(seat.seat))

  const toggle = (seat: SeatResponse) => {
    if (!seat.seat || reservation) return
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(seat.seat)) next.delete(seat.seat)
      else next.add(seat.seat)
      return next
    })
  }

  const onHold = async (seats: GroundedSeat[]) => {
    if (!flightId) return
    setBusy(true)
    setHoldError(null)
    try {
      const created = await holdSeats(flightId, seats)
      setReservation(created)
      setSelected(new Set())
      load()
    } catch (cause) {
      setHoldError(cause instanceof ApiError ? cause.message : 'Could not hold those seats.')
    } finally {
      setBusy(false)
    }
  }

  const onCancel = async () => {
    if (!reservation) return
    setBusy(true)
    setHoldError(null)
    try {
      await cancelReservation(reservation.reservationId)
      setReservation(null)
      load()
    } catch (cause) {
      setHoldError(cause instanceof ApiError ? cause.message : 'Could not cancel the hold.')
    } finally {
      setBusy(false)
    }
  }

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
        <span className="seat seat-selected legend-swatch" aria-hidden="true" /> selected
      </div>

      <SeatGrid seats={visibleSeats} selected={selected} onToggle={toggle} />

      <HoldPanel
        selected={selectedSeats}
        reservation={reservation}
        busy={busy}
        error={holdError}
        onHold={onHold}
        onCancel={onCancel}
        onReleaseSeat={(seat) =>
          setSelected((current) => {
            const next = new Set(current)
            next.delete(seat)
            return next
          })
        }
      />
    </section>
  )
}
