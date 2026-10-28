import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, cancelReservation, getMyReservations } from '../api/client'
import type { ReservationResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { isUnauthorized, useRequireAuth } from '../auth/useRequireAuth'
import { formatRemaining, useRemainingSeconds } from '../components/useRemainingSeconds'

export function MyReservationsPage() {
  const { user } = useAuth()
  useRequireAuth()
  const navigate = useNavigate()

  const [reservations, setReservations] = useState<ReservationResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)

  const sendToAuth = () => navigate('/auth?redirect=%2Freservations', { replace: true })

  const load = useCallback(
    (signal?: AbortSignal) => {
      if (!user) return
      getMyReservations(signal)
        .then(setReservations)
        .catch((cause: unknown) => {
          if (cause instanceof DOMException && cause.name === 'AbortError') return
          if (isUnauthorized(cause)) {
            sendToAuth()
            return
          }
          setError(cause instanceof ApiError ? cause.message : 'Could not load your reservations.')
        })
    },
    [user],
  )

  useEffect(() => {
    if (!user) return
    const controller = new AbortController()
    load(controller.signal)
    return () => controller.abort()
  }, [load, user])

  const onCancel = async (reservationId: string) => {
    setBusyId(reservationId)
    setError(null)
    try {
      await cancelReservation(reservationId)
      load()
    } catch (cause) {
      if (isUnauthorized(cause)) {
        sendToAuth()
        return
      }
      setError(cause instanceof ApiError ? cause.message : 'Could not cancel the reservation.')
    } finally {
      setBusyId(null)
    }
  }

  if (error) {
    return (
      <section>
        <p className="error">{error}</p>
        <Link to="/">Back to search</Link>
      </section>
    )
  }

  if (!reservations) return <p className="muted">Loading your reservations…</p>

  return (
    <section>
      <div className="results-header">
        <h1>My reservations</h1>
        <p className="muted">{user?.email}</p>
      </div>

      {reservations.length === 0 ? (
        <p className="muted">
          You have no reservations yet. <Link to="/search">Find a flight</Link>.
        </p>
      ) : (
        <ul className="reservation-list">
          {reservations.map((reservation) => (
            <ReservationRow
              key={reservation.reservationId}
              reservation={reservation}
              busy={busyId === reservation.reservationId}
              onCancel={onCancel}
            />
          ))}
        </ul>
      )}
    </section>
  )
}

function ReservationRow({
  reservation,
  busy,
  onCancel,
}: {
  reservation: ReservationResponse
  busy: boolean
  onCancel: (reservationId: string) => void
}) {
  const remaining = useRemainingSeconds(reservation.status === 'PENDING' ? reservation.expiresAt : null)

  return (
    <li className="card reservation-item">
      <div className="reservation-head">
        <strong>{reservation.status.toLowerCase()}</strong>
        <span className="muted">
          {reservation.seats.length} seat{reservation.seats.length === 1 ? '' : 's'} ·{' '}
          {reservation.seats.map((seat) => seat.seat).join(', ')}
        </span>
      </div>
      {remaining !== null && <p className="muted">Held for {formatRemaining(remaining)}</p>}
      <div className="reservation-actions">
        <Link to={`/flights/${encodeURIComponent(reservation.flightId)}/seats`}>Open flight</Link>
        {reservation.status === 'PENDING' && (
          <button
            type="button"
            className="link-button"
            disabled={busy}
            onClick={() => onCancel(reservation.reservationId)}
          >
            Cancel
          </button>
        )}
      </div>
    </li>
  )
}
