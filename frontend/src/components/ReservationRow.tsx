import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { ReservationResponse } from '../api/types'
import { useConfirm } from './confirm/ConfirmProvider'
import { HoldSeatList } from './HoldSeatList'
import { formatRemaining, useRemainingSeconds } from './useRemainingSeconds'

const money = new Intl.NumberFormat(undefined, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

export function ReservationRow({
  reservation,
  busy,
  onCancel,
}: {
  reservation: ReservationResponse
  busy: boolean
  onCancel: (reservationId: string, seats?: string[]) => void
}) {
  const confirm = useConfirm()
  const remaining = useRemainingSeconds(reservation.status === 'PENDING' ? reservation.expiresAt : null)
  const [picked, setPicked] = useState<string[]>([])
  const pending = reservation.status === 'PENDING'

  const toggle = (seat: string) =>
    setPicked((current) =>
      current.includes(seat) ? current.filter((item) => item !== seat) : [...current, seat],
    )

  const releaseSelected = async () => {
    const seats = picked
    const answer = await confirm({
      title: `Release ${seats.length === 1 ? 'this seat' : `these ${seats.length} seats`}?`,
      body: `${seats.join(', ')} ${seats.length === 1 ? 'is' : 'are'} released and can be booked by anyone.`,
      confirmLabel: 'Release',
      danger: true,
    })
    if (!answer) return
    onCancel(reservation.reservationId, seats)
    setPicked([])
  }

  const cancelAll = async () => {
    const answer = await confirm({
      title: 'Cancel this reservation?',
      body: `All ${reservation.seats.length} seat(s) in this reservation are released.`,
      confirmLabel: 'Cancel reservation',
      cancelLabel: 'Keep it',
      danger: true,
    })
    if (answer) onCancel(reservation.reservationId)
  }

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
      {reservation.paid ? (
        <p>
          Paid <strong>{money.format(reservation.paid.amount)} {reservation.paid.currency}</strong> by card ••••{' '}
          {reservation.paid.cardLast4}
        </p>
      ) : (
        reservation.price && (
          <p className="muted">
            Total {money.format(reservation.price.amount)} {reservation.price.currency}
          </p>
        )
      )}
      <HoldSeatList
        seats={reservation.seats}
        picked={picked}
        disabled={busy || !pending || reservation.seats.length < 2}
        onToggle={toggle}
      />
      <div className="reservation-actions">
        {pending && (
          <Link to={`/checkout/${encodeURIComponent(reservation.reservationId)}`}>Complete payment</Link>
        )}
        <Link to={`/flights/${encodeURIComponent(reservation.flightId)}/seats`}>Open flight</Link>
        {pending && (
          <>
            <button
              type="button"
              className="link-button"
              disabled={busy || picked.length === 0}
              onClick={releaseSelected}
            >
              Release selected{picked.length > 0 ? ` (${picked.length})` : ''}
            </button>
            <button type="button" className="link-button" disabled={busy} onClick={cancelAll}>
              Cancel all
            </button>
          </>
        )}
      </div>
    </li>
  )
}
