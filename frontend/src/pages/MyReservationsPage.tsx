import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, cancelReservation, cancelReservationSeats, getMyReservations } from '../api/client'
import type { ReservationResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { isUnauthorized, useRequireAuth } from '../auth/useRequireAuth'
import { ReservationRow } from '../components/ReservationRow'

export function MyReservationsPage() {
  const { user, expireSession } = useAuth()
  useRequireAuth()
  const navigate = useNavigate()

  const [reservations, setReservations] = useState<ReservationResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)

  const sendToAuth = () => {
    expireSession()
    navigate('/auth?redirect=%2Freservations', { replace: true })
  }

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

  const onCancel = async (reservationId: string, seats?: string[]) => {
    setBusyId(reservationId)
    setError(null)
    try {
      const cancelled = seats?.length
        ? await cancelReservationSeats(reservationId, seats)
        : await cancelReservation(reservationId)
      setReservations((current) =>
        current
          ? current.map((reservation) =>
              reservation.reservationId === cancelled.reservationId ? cancelled : reservation,
            )
          : current,
      )
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
