import { useState } from 'react'
import type { ReservationResponse } from '../api/types'
import { useConfirm } from './confirm/ConfirmProvider'
import { HoldSeatList } from './HoldSeatList'
import { formatRemaining, useRemainingSeconds } from './useRemainingSeconds'

export function SeatHold({
  hold,
  busy,
  onCancel,
  onCancelSeats,
}: {
  hold: ReservationResponse
  busy: boolean
  onCancel: (reservationId: string) => void
  onCancelSeats: (reservationId: string, seats: string[]) => void
}) {
  const seconds = useRemainingSeconds(hold.expiresAt ?? null)
  const expired = seconds === 0
  const [picked, setPicked] = useState<string[]>([])
  const confirm = useConfirm()

  const toggle = (seat: string) =>
    setPicked((current) => (current.includes(seat) ? current.filter((item) => item !== seat) : [...current, seat]))

  const cancelAll = async () => {
    const answer = await confirm({
      title: 'Release every seat?',
      body: `All ${hold.seats.length} seat(s) in this hold are released and anyone can book them.`,
      confirmLabel: 'Release all',
      danger: true,
    })
    if (answer) onCancel(hold.reservationId)
  }

  const cancelPicked = async () => {
    const seats = picked.length > 0 ? picked : hold.seats.map((seat) => seat.seat)
    const all = seats.length === hold.seats.length
    const answer = await confirm({
      title: all ? 'Release every seat?' : `Release ${seats.length} seat(s)?`,
      body: `${seats.join(', ')} ${all ? 'are' : 'is'} released and can be booked by anyone.`,
      confirmLabel: 'Release',
      danger: true,
    })
    if (answer) onCancelSeats(hold.reservationId, seats)
  }

  return (
    <li className="passenger-row">
      <div className="passenger-row-head">
        <strong>{hold.seats.map((seat) => seat.seat).join(', ')}</strong>
        <span className="muted">
          {expired
            ? 'Expired'
            : `Pay within ${formatRemaining(seconds ?? 0)} or the seats are released.`}
        </span>
      </div>
      <HoldSeatList
        seats={hold.seats}
        picked={picked}
        disabled={busy || expired}
        onToggle={toggle}
      />
      {!expired && (
        <div className="reservation-actions">
          <button
            type="button"
            className="link-button"
            disabled={busy || picked.length === 0}
            onClick={cancelPicked}
          >
            {picked.length === 0
              ? 'Release selected'
              : `Release selected (${picked.length})`}
          </button>
          <button type="button" className="link-button" disabled={busy} onClick={cancelAll}>
            Release all
          </button>
        </div>
      )}
    </li>
  )
}
