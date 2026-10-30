import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { SeatResponse } from '../api/types'
import { PassengerEditorRow } from './PassengerEditorRow'
import { EMPTY_DRAFT, isDraftReady, type PassengerDraft } from './passengerDraft'

export function SeatEditor({
  selected,
  drafts,
  profileFirst,
  profileLast,
  hasProfileName,
  busy,
  onChoose,
  onName,
  onPassport,
  onPhone,
  onBirth,
  onRemove,
  onHold,
}: {
  selected: SeatResponse[]
  drafts: Record<string, PassengerDraft>
  profileFirst: string
  profileLast: string
  hasProfileName: boolean
  busy: boolean
  onChoose: (seat: string, passengerType: 'SELF' | 'OTHER') => void
  onName: (seat: string, field: 'firstName' | 'lastName', value: string) => void
  onPassport: (seat: string, value: string) => void
  onPhone: (seat: string, value: string) => void
  onBirth: (seat: string, value: string) => void
  onRemove: (seat: string) => void
  onHold: (seats: { seat: string; passengerType: 'SELF' | 'OTHER'; firstName?: string; lastName?: string; passportNumber?: string; phoneNumber?: string; dateOfBirth?: string }[]) => void
}) {
  const draftFor = (seat: string) => drafts[seat] ?? EMPTY_DRAFT
  const ready = selected.every((seat) => isDraftReady(draftFor(seat.seat)))
  const [showErrors, setShowErrors] = useState(false)
  const seatKey = selected.map((seat) => seat.seat).join(',')

  useEffect(() => {
    setShowErrors(false)
  }, [seatKey])

  const submit = () => {
    if (!ready) {
      setShowErrors(true)
      return
    }
    onHold(
      selected.map((seat) => {
        const draft = draftFor(seat.seat)
        const passportNumber = draft.passportNumber.trim() || undefined
        const phoneNumber = draft.phoneNumber.trim() || undefined
        const dateOfBirth = draft.dateOfBirth.trim() || undefined
        if (draft.passengerType === 'SELF') {
          return {
            seat: seat.seat,
            passengerType: 'SELF' as const,
            ...(passportNumber ? { passportNumber } : {}),
            ...(phoneNumber ? { phoneNumber } : {}),
            ...(dateOfBirth ? { dateOfBirth } : {}),
          }
        }
        return {
          seat: seat.seat,
          passengerType: 'OTHER' as const,
          firstName: draft.firstName.trim(),
          lastName: draft.lastName.trim(),
          ...(passportNumber ? { passportNumber } : {}),
          ...(phoneNumber ? { phoneNumber } : {}),
          ...(dateOfBirth ? { dateOfBirth } : {}),
        }
      }),
    )
  }

  return (
    <section className="hold-panel">
      <h2>{selected.length === 1 ? 'One seat selected' : `${selected.length} seats selected`}</h2>
      <p className="muted">A passport number, phone number and date of birth are required for every passenger.</p>
      {!hasProfileName && (
        <p className="muted">
          These seats are booked with the names you enter below. You can save your own details later in{' '}
          <Link to="/profile">your details</Link>.
        </p>
      )}
      <ul className="hold-seats">
        {selected.map((seat) => (
          <PassengerEditorRow
            key={seat.seat}
            seat={seat}
            draft={draftFor(seat.seat)}
            profileFirst={profileFirst}
            profileLast={profileLast}
            hasProfileName={hasProfileName}
            showErrors={showErrors}
            onChoose={(passengerType) => onChoose(seat.seat, passengerType)}
            onName={(field, value) => onName(seat.seat, field, value)}
            onPassport={(value) => onPassport(seat.seat, value)}
            onPhone={(value) => onPhone(seat.seat, value)}
            onBirth={(value) => onBirth(seat.seat, value)}
            onRemove={() => onRemove(seat.seat)}
          />
        ))}
      </ul>
      <button type="button" className="button seat-buy" disabled={busy} onClick={submit}>
        {busy ? 'Starting checkout…' : 'Buy'}
      </button>
    </section>
  )
}
