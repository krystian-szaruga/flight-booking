import { useState } from 'react'
import type { ReservationResponse } from '../api/types'
import { useConfirm } from './confirm/ConfirmProvider'
import { HoldSeatList } from './HoldSeatList'
import { formatRemaining, useRemainingSeconds } from './useRemainingSeconds'

const money = new Intl.NumberFormat(undefined, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const TEST_CARDS = [
  { number: '4242424242424242', label: 'Test card · approves' },
  { number: '4000000000000002', label: 'Test card · declines' },
  { number: '4000000000009995', label: 'Test card · no funds' },
]

export function SeatHold({
  hold,
  busy,
  onCancel,
  onCancelSeats,
  onPay,
}: {
  hold: ReservationResponse
  busy: boolean
  onCancel: (reservationId: string) => void
  onCancelSeats: (reservationId: string, seats: string[]) => void
  onPay: (reservationId: string, cardNumber: string) => void
}) {
  const seconds = useRemainingSeconds(hold.expiresAt ?? null)
  const confirmed = hold.status === 'CONFIRMED'
  const expired = !confirmed && seconds === 0
  const [picked, setPicked] = useState<string[]>([])
  const [card, setCard] = useState(TEST_CARDS[0].number)
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
          {confirmed
            ? 'Paid and confirmed.'
            : expired
              ? 'Expired'
              : `Pay within ${formatRemaining(seconds ?? 0)} or the seats are released.`}
        </span>
      </div>
      {confirmed && hold.paid && (
        <p className="muted">
          Paid {money.format(hold.paid.amount)} {hold.paid.currency} · card •••• {hold.paid.cardLast4}
        </p>
      )}
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
          {!confirmed && (
            <button type="button" className="link-button" disabled={busy} onClick={cancelAll}>
              Release all
            </button>
          )}
        </div>
      )}
      {hold.status === 'PENDING' && !expired && (
        <div className="payment-box">
          <label htmlFor={`card-${hold.reservationId}`}>Test card</label>
          <select
            id={`card-${hold.reservationId}`}
            value={card}
            disabled={busy}
            onChange={(event) => setCard(event.target.value)}
          >
            {TEST_CARDS.map((option) => (
              <option key={option.number} value={option.number}>
                {option.label}
              </option>
            ))}
          </select>
          {hold.price && (
            <strong className="payment-total">
              {money.format(hold.price.amount)} {hold.price.currency}
            </strong>
          )}
          <button
            type="button"
            className="button"
            disabled={busy}
            onClick={() => onPay(hold.reservationId, card)}
          >
            Pay now
          </button>
        </div>
      )}
    </li>
  )
}
