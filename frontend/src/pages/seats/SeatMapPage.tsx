import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  ApiError,
  getSeatMap,
  holdSeats,
  updateReservationPassengers,
} from '../../api/client'
import type { Cabin, HoldSeat, ReservationResponse, SeatMapResponse, SeatResponse } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { isUnauthorized } from '../../auth/useRequireAuth'
import { SignInToContinue } from '../../components/AfterAuthLink'
import { HoldPanel } from '../../components/HoldPanel'
import { SeatGrid } from '../../components/SeatGrid'
import { useCurrency } from '../../currency/CurrencyContext'
import { FareSummary, FlightHeader, SeatLegend } from './FlightHeader'

interface SeatMapState {
  edit?: { reservation: ReservationResponse; passengers: HoldSeat[] }
}

export function SeatMapPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { flightId } = useParams<{ flightId: string }>()
  const [params] = useSearchParams()
  const cabin = params.get('cabin') as Cabin | null
  const edit = (location.state as SeatMapState | null)?.edit

  const { user, loading, expireSession } = useAuth()
  const { currency } = useCurrency()

  const sendToAuth = () => {
    expireSession()
    navigate(`/auth?redirect=${encodeURIComponent(`${location.pathname}${location.search}`)}`, { replace: true })
  }

  const [seatMap, setSeatMap] = useState<SeatMapResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [selected, setSelected] = useState<ReadonlySet<string>>(
    () => new Set(edit?.reservation.seats.map((seat) => seat.seat) ?? []),
  )
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
      getSeatMap(flightId, currency, signal)
        .then(setSeatMap)
        .catch((cause: unknown) => {
          if (cause instanceof DOMException && cause.name === 'AbortError') return
          setError(cause instanceof ApiError ? cause.message : 'Could not load the seat map.')
        })
    },
    [flightId, currency],
  )

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
    return () => controller.abort()
  }, [load])

  const toggle = (seat: SeatResponse) => {
    if (!seat.seat || seat.status !== 'AVAILABLE') return
    if (cabin && seat.cabin !== cabin) return
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(seat.seat)) next.delete(seat.seat)
      else next.add(seat.seat)
      return next
    })
  }

  const guarded = async (action: () => Promise<void>, fallback: string) => {
    setBusy(true)
    setHoldError(null)
    try {
      await action()
    } catch (cause) {
      if (isUnauthorized(cause)) {
        sendToAuth()
        return
      }
      setHoldError(cause instanceof ApiError ? cause.message : fallback)
    } finally {
      setBusy(false)
    }
  }

  const onHold = (seats: HoldSeat[]) => {
    if (!flightId) return
    void guarded(async () => {
      if (edit) {
        const updated = await updateReservationPassengers(edit.reservation.reservationId, seats, currency)
        navigate(`/checkout/${encodeURIComponent(updated.reservationId)}`, {
          state: { reservation: updated, seatMap, passengers: seats },
        })
        return
      }
      const created = await holdSeats(flightId, seats, currency)
      setSelected(new Set())
      navigate(`/checkout/${encodeURIComponent(created.reservationId)}`, {
        state: { reservation: created, seatMap, passengers: seats },
      })
    }, edit ? 'Could not update the passengers.' : 'Could not start the purchase.')
  }

  const onReleaseSeat = (seat: string) =>
    setSelected((current) => {
      const next = new Set(current)
      next.delete(seat)
      return next
    })

  if (error) {
    return (
      <section>
        <p className="error">{error}</p>
        <Link to="/search">Back to search</Link>
      </section>
    )
  }

  if (loading) return <p className="muted">Checking your session…</p>

  const currentTarget = `${location.pathname}${location.search}`
  if (!user?.emailVerifiedAt) {
    return (
      <>
        {seatMap && <FlightHeader seatMap={seatMap} />}
        <SignInToContinue target={currentTarget} />
      </>
    )
  }

  if (!seatMap) return <p className="muted">Loading seat map…</p>

  const visibleFares = cabin ? seatMap.fares.filter((fare) => fare.cabin === cabin) : seatMap.fares
  const selectedSeats = seatMap.seats.filter((seat) => selected.has(seat.seat))

  return (
    <section>
      <FlightHeader seatMap={seatMap} />
      <FareSummary fares={visibleFares} />
      <SeatGrid seats={seatMap.seats} selected={selected} onToggle={toggle} allowedCabin={cabin ?? undefined} />
      <SeatLegend />
      {cabin && (
        <p className="muted">
          Showing the whole aircraft; {cabin.replace('_', ' ').toLowerCase()} seats are selectable and the other
          cabins are greyed out. <Link to={`/flights/${encodeURIComponent(seatMap.flightId)}/seats`}>Show all cabins</Link>
        </p>
      )}
      <HoldPanel
        selected={selectedSeats}
        initialPassengers={edit?.passengers}
        busy={busy}
        error={holdError}
        onHold={onHold}
        onReleaseSeat={onReleaseSeat}
      />
    </section>
  )
}
