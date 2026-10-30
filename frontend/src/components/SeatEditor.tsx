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
  onRemove: (seat: string) => void
  onHold: (seats: { seat: string; passengerType: 'SELF' | 'OTHER'; firstName?: string; lastName?: string }[]) => void
}) {
  const draftFor = (seat: string) => drafts[seat] ?? EMPTY_DRAFT
  const ready = selected.every((seat) => isDraftReady(draftFor(seat.seat)))

  const submit = () =>
    onHold(
      selected.map((seat) => {
        const draft = draftFor(seat.seat)
        if (draft.passengerType === 'SELF') {
          return { seat: seat.seat, passengerType: 'SELF' as const }
        }
        return {
          seat: seat.seat,
          passengerType: 'OTHER' as const,
          firstName: draft.firstName.trim(),
          lastName: draft.lastName.trim(),
        }
      }),
    )

  return (
    <section className="hold-panel">
      <h2>{selected.length === 1 ? 'One seat selected' : `${selected.length} seats selected`}</h2>
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
            onChoose={(passengerType) => onChoose(seat.seat, passengerType)}
            onName={(field, value) => onName(seat.seat, field, value)}
            onRemove={() => onRemove(seat.seat)}
          />
        ))}
      </ul>
      <button type="button" className="button seat-buy" disabled={!ready || busy} onClick={submit}>
        {busy ? 'Starting checkout…' : 'Buy'}
      </button>
    </section>
  )
}
