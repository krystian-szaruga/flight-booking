import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { ApiError, getMyReservations, getSeatMap, payReservation } from '../api/client'
import type { ReservationResponse, SeatMapResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { useRequireAuth } from '../auth/useRequireAuth'
import { formatRemaining, useRemainingSeconds } from '../components/useRemainingSeconds'
import { useCurrency } from '../currency/CurrencyContext'

const TEST_CARDS = [
  { number: '4242424242424242', label: 'Test card · approves', detail: '•••• 4242' },
  { number: '4000000000000002', label: 'Test card · declines', detail: '•••• 0002' },
  { number: '4000000000009995', label: 'Test card · no funds', detail: '•••• 9995' },
]

const money = new Intl.NumberFormat(undefined, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

interface CheckoutState {
  reservation?: ReservationResponse
  seatMap?: SeatMapResponse
}

function paymentKey(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : Math.random().toString(36).slice(2)
}

export function CheckoutPage() {
  const { reservationId = '' } = useParams<{ reservationId: string }>()
  const location = useLocation()
  const state = (location.state ?? {}) as CheckoutState
  const { currency } = useCurrency()
  const { user } = useAuth()
  useRequireAuth()

  const [reservation, setReservation] = useState<ReservationResponse | null>(state.reservation ?? null)
  const [seatMap, setSeatMap] = useState<SeatMapResponse | null>(state.seatMap ?? null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(!state.reservation)
  const [cardOpen, setCardOpen] = useState(false)
  const [card, setCard] = useState(TEST_CARDS[0].number)
  const [paying, setPaying] = useState(false)
  const [declined, setDeclined] = useState<string | null>(null)

  useEffect(() => {
    if (state.reservation || !reservationId) return
    const controller = new AbortController()
    getMyReservations(currency, controller.signal)
      .then((items) => {
        const found = items.find((item) => item.reservationId === reservationId)
        if (!found) setError('We could not find that booking.')
        else setReservation(found)
      })
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setError(cause instanceof ApiError ? cause.message : 'Could not load that booking.')
      })
      .finally(() => setLoading(false))
    return () => controller.abort()
  }, [reservationId, currency, state.reservation])

  useEffect(() => {
    const flightId = reservation?.flightId
    if (!flightId || seatMap) return
    const controller = new AbortController()
    getSeatMap(flightId, currency, controller.signal)
      .then(setSeatMap)
      .catch(() => undefined)
    return () => controller.abort()
  }, [reservation, seatMap, currency])

  const seconds = useRemainingSeconds(reservation?.status === 'PENDING' ? reservation.expiresAt : null)
  const confirmed = reservation?.status === 'CONFIRMED'
  const expired = !confirmed && reservation?.status === 'PENDING' && seconds === 0
  const total = reservation?.price
    ? `${money.format(reservation.price.amount)} ${reservation.price.currency}`
    : 'the total'

  const pay = async () => {
    if (!reservation) return
    setPaying(true)
    setDeclined(null)
    try {
      const result = await payReservation(reservation.reservationId, {
        idempotencyKey: paymentKey(),
        cardNumber: card,
        cardHolder:
          [user?.firstName, user?.lastName].filter(Boolean).join(' ') || user?.email || 'Card holder',
        currency,
      })
      if (result.status === 'DECLINED') {
        setDeclined(result.declineReason ?? 'The card was declined.')
        return
      }
      setCardOpen(false)
      setReservation({
        ...reservation,
        status: 'CONFIRMED',
        expiresAt: null,
        paid: {
          amount: result.amount,
          currency: result.currency,
          cardLast4: result.cardLast4,
          paidAt: result.createdAt,
        },
      })
    } catch (cause) {
      setDeclined(cause instanceof ApiError ? cause.message : 'Could not take the payment.')
    } finally {
      setPaying(false)
    }
  }

  if (loading) return <p className="muted">Loading your booking…</p>

  if (error || !reservation) {
    return (
      <section>
        <p className="error">{error ?? 'We could not find that booking.'}</p>
        <Link to="/reservations">Back to my reservations</Link>
      </section>
    )
  }

  return (
    <section className="checkout">
      <div className="checkout-head">
        <p className="checkout-eyebrow">Checkout</p>
        <h1>{confirmed ? 'You are all set' : 'Complete your booking'}</h1>
        {seatMap && (
          <p className="muted">
            {seatMap.flightNumber} · {seatMap.origin} to {seatMap.destination} ·{' '}
            {new Date(seatMap.departureAt).toLocaleString()}
          </p>
        )}
      </div>

      {confirmed ? (
        <section className="checkout-success" aria-live="polite">
          <div className="checkout-success-mark" aria-hidden="true">
            ✓
          </div>
          <h2>Booking confirmed</h2>
          <p className="muted">
            Paid {money.format(reservation.paid?.amount ?? reservation.price?.amount ?? 0)}{' '}
            {reservation.paid?.currency ?? reservation.price?.currency ?? ''} · card ••••{' '}
            {reservation.paid?.cardLast4 ?? '••••'}
          </p>
          <ul className="checkout-seat-chips">
            {reservation.seats.map((seat) => (
              <li key={seat.seat}>{seat.seat}</li>
            ))}
          </ul>
          <Link to="/reservations" className="button">
            View my reservations
          </Link>
        </section>
      ) : (
        <div className="checkout-layout">
          <div className="checkout-main">
            <section className="checkout-card">
              <div className="checkout-card-head">
                <h2>Passenger details</h2>
                <span className="checkout-tag">Names from your seats</span>
              </div>
              <p className="muted">
                Fields marked “not supported” need a backend extension before they can be filled in.
              </p>
              <ul className="checkout-passengers">
                {reservation.seats.map((seat) => (
                  <li key={seat.seat} className="checkout-passenger">
                    <div className="checkout-passenger-head">
                      <span className="checkout-seat">{seat.seat}</span>
                      <strong>
                        {seat.firstName} {seat.lastName}
                      </strong>
                    </div>
                    <div className="checkout-grid">
                      <label>
                        <span className="muted">First name</span>
                        <input value={seat.firstName} readOnly />
                      </label>
                      <label>
                        <span className="muted">Last name</span>
                        <input value={seat.lastName} readOnly />
                      </label>
                      <label>
                        <span className="muted">Date of birth</span>
                        <input disabled placeholder="Not supported yet" />
                      </label>
                      <label>
                        <span className="muted">Nationality</span>
                        <input disabled placeholder="Not supported yet" />
                      </label>
                      <label>
                        <span className="muted">Passport number</span>
                        <input disabled placeholder="Not supported yet" />
                      </label>
                      <label>
                        <span className="muted">Phone number</span>
                        <input disabled placeholder="Not supported yet" />
                      </label>
                    </div>
                  </li>
                ))}
              </ul>
            </section>

            <section className="checkout-card">
              <h2>Contact</h2>
              <div className="checkout-grid">
                <label>
                  <span className="muted">Email</span>
                  <input value={user?.email ?? ''} disabled />
                </label>
                <label>
                  <span className="muted">Billing address</span>
                  <input disabled placeholder="Not supported yet" />
                </label>
              </div>
            </section>
          </div>

          <aside className="checkout-summary">
            <h2>Order summary</h2>
            <ul className="checkout-seat-chips">
              {reservation.seats.map((seat) => (
                <li key={seat.seat}>{seat.seat}</li>
              ))}
            </ul>
            <div className="checkout-summary-row">
              <span className="muted">Seats</span>
              <span>{reservation.seats.length}</span>
            </div>
            <div className="checkout-summary-row checkout-total">
              <span>Total</span>
              <strong>{total}</strong>
            </div>
            <p className={`checkout-countdown ${expired ? 'checkout-countdown-expired' : ''}`}>
              {expired
                ? 'This hold has expired. Pick your seats again.'
                : `Seats held for ${formatRemaining(seconds ?? 0)}`}
            </p>
            <button
              type="button"
              className="button checkout-pay"
              disabled={expired || paying}
              onClick={() => {
                setDeclined(null)
                setCardOpen(true)
              }}
            >
              Pay {total}
            </button>
            <Link to={`/flights/${encodeURIComponent(reservation.flightId)}/seats`} className="checkout-back">
              Back to seat map
            </Link>
          </aside>
        </div>
      )}

      {cardOpen && (
        <div
          className="dialog-backdrop"
          role="presentation"
          onClick={() => {
            if (!paying) setCardOpen(false)
          }}
        >
          <div
            className="dialog payment-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="payment-title"
            onClick={(event) => event.stopPropagation()}
          >
            <h2 id="payment-title">Choose a card</h2>
            <p className="muted">
              This is a demo payment. In production you would be redirected to a payment provider.
            </p>
            <div className="card-options">
              {TEST_CARDS.map((option) => (
                <label
                  key={option.number}
                  className={`card-option ${card === option.number ? 'card-option-active' : ''}`}
                >
                  <input
                    type="radio"
                    name="card"
                    value={option.number}
                    checked={card === option.number}
                    disabled={paying}
                    onChange={() => setCard(option.number)}
                  />
                  <span className="card-option-label">{option.label}</span>
                  <span className="card-option-number">{option.detail}</span>
                </label>
              ))}
            </div>
            {declined && <p className="error">{declined}</p>}
            <div className="dialog-actions">
              <button
                type="button"
                className="link-button"
                disabled={paying}
                onClick={() => setCardOpen(false)}
              >
                Cancel
              </button>
              <button type="button" className="button" disabled={paying} onClick={pay}>
                {paying ? 'Paying…' : `Pay ${total}`}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}
