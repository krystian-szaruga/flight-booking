import { useState } from 'react'
import type { HoldSeat, PassengerType, SeatResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { SeatEditor } from './SeatEditor'
import type { PassengerDraft } from './passengerDraft'

interface HoldPanelProps {
  selected: SeatResponse[]
  busy: boolean
  error: string | null
  onHold: (seats: HoldSeat[]) => void
  onReleaseSeat: (seat: string) => void
}

export function HoldPanel({
  selected,
  busy,
  error,
  onHold,
  onReleaseSeat,
}: HoldPanelProps) {
  const { user } = useAuth()
  const [drafts, setDrafts] = useState<Record<string, PassengerDraft>>({})

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

      {selected.length === 0 ? (
        <p className="muted">Select seats on the map to buy them.</p>
      ) : (
        <p className="muted">
          Selected: <strong>{selected.map((seat) => seat.seat).join(', ')}</strong>
        </p>
      )}
      {error && <p className="error">{error}</p>}
    </>
  )
}
