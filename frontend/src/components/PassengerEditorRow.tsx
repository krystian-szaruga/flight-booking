import { Link } from 'react-router-dom'
import type { SeatResponse } from '../api/types'
import type { PassengerDraft } from './passengerDraft'

export function PassengerEditorRow({
  seat,
  draft,
  profileFirst,
  profileLast,
  hasProfileName,
  onChoose,
  onName,
  onRemove,
}: {
  seat: SeatResponse
  draft: PassengerDraft
  profileFirst: string
  profileLast: string
  hasProfileName: boolean
  onChoose: (passengerType: 'SELF' | 'OTHER') => void
  onName: (field: 'firstName' | 'lastName', value: string) => void
  onRemove: () => void
}) {
  const forSelf = draft.passengerType === 'SELF'

  return (
    <li className="passenger-row">
      <div className="passenger-row-head">
        <strong>{seat.seat}</strong>
        <button
          type="button"
          className="link-button"
          onClick={onRemove}
          aria-label={`Remove seat ${seat.seat}`}
        >
          Remove
        </button>
      </div>
      {hasProfileName && (
        <div className="passenger-types" role="group" aria-label={`Who is flying in seat ${seat.seat}`}>
          <label className={`passenger-type ${forSelf ? '' : 'passenger-type-muted'}`}>
            <input type="radio" name={`who-${seat.seat}`} checked={forSelf} onChange={() => onChoose('SELF')} />
            For me
          </label>
          <label className={`passenger-type ${forSelf ? 'passenger-type-muted' : ''}`}>
            <input type="radio" name={`who-${seat.seat}`} checked={!forSelf} onChange={() => onChoose('OTHER')} />
            For someone else
          </label>
        </div>
      )}
      <div className={`passenger-name-fields ${forSelf ? 'passenger-name-fields-readonly' : ''}`}>
        <label>
          <span className="muted">First name</span>
          <input
            type="text"
            aria-label={`First name for seat ${seat.seat}`}
            placeholder="First name"
            maxLength={100}
            value={forSelf ? profileFirst : draft.firstName}
            readOnly={forSelf}
            onChange={(event) => onName('firstName', event.target.value)}
          />
        </label>
        <label>
          <span className="muted">Last name</span>
          <input
            type="text"
            aria-label={`Last name for seat ${seat.seat}`}
            placeholder="Last name"
            maxLength={100}
            value={forSelf ? profileLast : draft.lastName}
            readOnly={forSelf}
            onChange={(event) => onName('lastName', event.target.value)}
          />
        </label>
      </div>
      {forSelf && !hasProfileName && (
        <p className="muted departure-passenger">
          Your profile has no name yet. <Link to="/profile">Add your details</Link>.
        </p>
      )}
    </li>
  )
}
