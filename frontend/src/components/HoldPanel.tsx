import { useState } from 'react'
import type { HoldSeat, PassengerType, ReservationResponse, SeatResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { SeatEditor } from './SeatEditor'
import { SeatHold } from './SeatHold'
import type { PassengerDraft } from './passengerDraft'

interface HoldPanelProps {
  selected: SeatResponse[]
  reservations: ReservationResponse[]
  busy: boolean
  error: string | null
  onHold: (seats: HoldSeat[]) => void
  onCancel: (reservationId: string) => void
  onCancelSeats: (reservationId: string, seats: string[]) => void
  onReleaseSeat: (seat: string) => void
  onPay: (reservationId: string, cardNumber: string) => void
}

export function HoldPanel({
  selected,
  reservations,
  busy,
  error,
  onHold,
  onCancel,
  onCancelSeats,
  onReleaseSeat,
  onPay,
}: HoldPanelProps) {
  const { user } = useAuth()
  const [drafts, setDrafts] = useState<Record<string, PassengerDraft>>({})

  const activeHolds = reservations.filter(
    (item) => item.status === 'PENDING' || item.status === 'CONFIRMED',
  )

  const choose = (seat: string, passengerType: PassengerType) =>
    setDrafts((current) => ({
      ...current,
      [seat]: {
        passengerType,
        firstName: current[seat]?.firstName ?? '',
        lastName: current[seat]?.lastName ?? '',
      },
    }))

  const updateName = (seat: string, field: 'firstName' | 'lastName', value: string) =>
    setDrafts((current) => ({
      ...current,
      [seat]: {
        passengerType: current[seat]?.passengerType ?? 'OTHER',
        firstName: field === 'firstName' ? value : (current[seat]?.firstName ?? ''),
        lastName: field === 'lastName' ? value : (current[seat]?.lastName ?? ''),
      },
    }))

  return (
    <>
      {selected.length > 0 && (
        <SeatEditor
          selected={selected}
          drafts={drafts}
          profileFirst={user?.firstName ?? ''}
          profileLast={user?.lastName ?? ''}
          hasProfileName={Boolean(user?.firstName && user?.lastName)}
          busy={busy}
          onChoose={choose}
          onName={updateName}
          onRemove={onReleaseSeat}
          onHold={onHold}
        />
      )}

      {activeHolds.length > 0 && (
        <section className="hold-panel" aria-live="polite">
          <h2>{activeHolds.length === 1 ? 'Seat on hold' : 'Seats on hold'}</h2>
          <ul className="hold-seats">
            {activeHolds.map((hold) => (
              <SeatHold
                key={hold.reservationId}
                hold={hold}
                busy={busy}
                onCancel={onCancel}
                onCancelSeats={onCancelSeats}
                onPay={onPay}
              />
            ))}
          </ul>
          {error && <p className="error">{error}</p>}
        </section>
      )}

      {selected.length === 0 ? (
        <>
          <p className="muted">
            Select {activeHolds.length > 0 ? 'more ' : ''}seats on the map to hold them.
          </p>
          {activeHolds.length === 0 && error && <p className="error">{error}</p>}
        </>
      ) : (
        <p className="muted">
          Selected: <strong>{selected.map((seat) => seat.seat).join(', ')}</strong>
        </p>
      )}
    </>
  )
}
