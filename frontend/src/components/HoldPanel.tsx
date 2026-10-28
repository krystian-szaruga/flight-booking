import { useState } from 'react'
import type { GroundedSeat, ReservationResponse, SeatResponse } from '../api/types'
import { formatRemaining, useRemainingSeconds } from './useRemainingSeconds'

interface HoldPanelProps {
  selected: SeatResponse[]
  reservation: ReservationResponse | null
  busy: boolean
  error: string | null
  onHold: (seats: GroundedSeat[]) => void
  onCancel: () => void
  onReleaseSeat: (seat: string) => void
}

export function HoldPanel({
  selected,
  reservation,
  busy,
  error,
  onHold,
  onCancel,
  onReleaseSeat,
}: HoldPanelProps) {
  const [names, setNames] = useState<Record<string, string>>({})
  const seconds = useRemainingSeconds(reservation?.expiresAt ?? null)

  if (reservation) {
    const expired = seconds === 0
    return (
      <section className="hold-panel" aria-live="polite">
        <h2>Seats on hold</h2>
        <p className="muted">
          {expired
            ? 'This hold has expired — the seats will be released.'
            : `Pay within ${formatRemaining(seconds ?? 0)} or the seats are released.`}
        </p>
        <ul className="hold-seats">
          {reservation.seats.map((seat) => (
            <li key={seat.seat}>
              <strong>{seat.seat}</strong> {seat.passengerName}
            </li>
          ))}
        </ul>
        <button type="button" className="link-button" onClick={onCancel} disabled={busy || expired}>
          {busy ? 'Cancelling…' : 'Cancel hold'}
        </button>
        {error && <p className="error">{error}</p>}
      </section>
    )
  }

  if (selected.length === 0) {
    return <p className="muted">Select seats on the map to hold them.</p>
  }

  const ready = selected.every((seat) => (names[seat.seat] ?? '').trim().length > 0)

  return (
    <section className="hold-panel">
      <h2>{selected.length === 1 ? 'One seat selected' : `${selected.length} seats selected`}</h2>
      <ul className="hold-seats">
        {selected.map((seat) => (
          <li key={seat.seat}>
            <strong>{seat.seat}</strong>
            <input
              type="text"
              aria-label={`Passenger name for seat ${seat.seat}`}
              placeholder="Passenger name"
              value={names[seat.seat] ?? ''}
              onChange={(event) => setNames((current) => ({ ...current, [seat.seat]: event.target.value }))}
            />
            <button
              type="button"
              className="link-button"
              onClick={() => onReleaseSeat(seat.seat)}
              aria-label={`Remove seat ${seat.seat}`}
            >
              Remove
            </button>
          </li>
        ))}
      </ul>
      <button
        type="button"
        disabled={!ready || busy}
        onClick={() => onHold(selected.map((seat) => ({ seat: seat.seat, passengerName: (names[seat.seat] ?? '').trim() })))}
      >
        {busy ? 'Holding…' : 'Hold seats'}
      </button>
      {error && <p className="error">{error}</p>}
    </section>
  )
}
