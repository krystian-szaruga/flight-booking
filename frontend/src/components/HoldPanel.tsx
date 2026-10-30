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
  initialPassengers,
  busy,
  error,
  onHold,
  onReleaseSeat,
}: HoldPanelProps & { initialPassengers?: HoldSeat[] }) {
  const { user } = useAuth()
  const [drafts, setDrafts] = useState<Record<string, PassengerDraft>>(() => {
    const initial: Record<string, PassengerDraft> = {}
    for (const passenger of initialPassengers ?? []) {
      initial[passenger.seat] = {
        passengerType: passenger.passengerType,
        firstName: passenger.firstName ?? '',
        lastName: passenger.lastName ?? '',
        passportNumber: passenger.passportNumber ?? '',
        phoneNumber: passenger.phoneNumber ?? '',
        dateOfBirth: passenger.dateOfBirth ?? '',
      }
    }
    return initial
  })

  const choose = (seat: string, passengerType: PassengerType) =>
    setDrafts((current) => ({
      ...current,
      [seat]: {
        passengerType,
        firstName: current[seat]?.firstName ?? '',
        lastName: current[seat]?.lastName ?? '',
        passportNumber: current[seat]?.passportNumber ?? '',
        phoneNumber: current[seat]?.phoneNumber ?? '',
        dateOfBirth: current[seat]?.dateOfBirth ?? '',
      },
    }))

  const updateName = (seat: string, field: 'firstName' | 'lastName', value: string) =>
    setDrafts((current) => ({
      ...current,
      [seat]: {
        passengerType: current[seat]?.passengerType ?? 'OTHER',
        firstName: field === 'firstName' ? value : (current[seat]?.firstName ?? ''),
        lastName: field === 'lastName' ? value : (current[seat]?.lastName ?? ''),
        passportNumber: current[seat]?.passportNumber ?? '',
        phoneNumber: current[seat]?.phoneNumber ?? '',
        dateOfBirth: current[seat]?.dateOfBirth ?? '',
      },
    }))

  const updatePassport = (seat: string, value: string) =>
    setDrafts((current) => ({
      ...current,
      [seat]: {
        passengerType: current[seat]?.passengerType ?? 'OTHER',
        firstName: current[seat]?.firstName ?? '',
        lastName: current[seat]?.lastName ?? '',
        passportNumber: value,
        phoneNumber: current[seat]?.phoneNumber ?? '',
        dateOfBirth: current[seat]?.dateOfBirth ?? '',
      },
    }))

  const updatePhone = (seat: string, value: string) =>
    setDrafts((current) => ({
      ...current,
      [seat]: {
        passengerType: current[seat]?.passengerType ?? 'OTHER',
        firstName: current[seat]?.firstName ?? '',
        lastName: current[seat]?.lastName ?? '',
        passportNumber: current[seat]?.passportNumber ?? '',
        phoneNumber: value,
        dateOfBirth: current[seat]?.dateOfBirth ?? '',
      },
    }))

  const updateBirth = (seat: string, value: string) =>
    setDrafts((current) => ({
      ...current,
      [seat]: {
        passengerType: current[seat]?.passengerType ?? 'OTHER',
        firstName: current[seat]?.firstName ?? '',
        lastName: current[seat]?.lastName ?? '',
        passportNumber: current[seat]?.passportNumber ?? '',
        phoneNumber: current[seat]?.phoneNumber ?? '',
        dateOfBirth: value,
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
          onPassport={updatePassport}
          onPhone={updatePhone}
          onBirth={updateBirth}
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
