import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { SeatResponse } from '../api/types'
import { PHONE_PATTERN, isFutureDate, type PassengerDraft } from './passengerDraft'

type Field = 'firstName' | 'lastName' | 'passport' | 'phone' | 'birth'

export function PassengerEditorRow({
  seat,
  draft,
  profileFirst,
  profileLast,
  hasProfileName,
  showErrors,
  onChoose,
  onName,
  onPassport,
  onPhone,
  onBirth,
  onRemove,
}: {
  seat: SeatResponse
  draft: PassengerDraft
  profileFirst: string
  profileLast: string
  hasProfileName: boolean
  showErrors: boolean
  onChoose: (passengerType: 'SELF' | 'OTHER') => void
  onName: (field: 'firstName' | 'lastName', value: string) => void
  onPassport: (value: string) => void
  onPhone: (value: string) => void
  onBirth: (value: string) => void
  onRemove: () => void
}) {
  const [touched, setTouched] = useState<Record<Field, boolean>>({
    firstName: false,
    lastName: false,
    passport: false,
    phone: false,
    birth: false,
  })
  const markTouched = (field: Field) => setTouched((current) => ({ ...current, [field]: true }))
  const visible = (field: Field) => showErrors || touched[field]

  const forSelf = draft.passengerType === 'SELF'
  const firstName = forSelf ? profileFirst : draft.firstName
  const lastName = forSelf ? profileLast : draft.lastName
  const errors: Record<Field, string | null> = {
    firstName: firstName.trim() ? null : 'First name is required',
    lastName: lastName.trim() ? null : 'Last name is required',
    passport: draft.passportNumber.trim() ? null : 'Passport number is required',
    phone: !draft.phoneNumber.trim()
      ? 'Phone number is required'
      : PHONE_PATTERN.test(draft.phoneNumber.trim())
        ? null
        : 'Use + and digits only, e.g. +48600123456',
    birth: !draft.dateOfBirth.trim()
      ? 'Date of birth is required'
      : isFutureDate(draft.dateOfBirth)
        ? 'Date of birth cannot be in the future'
        : null,
  }

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
            aria-invalid={visible('firstName') && errors.firstName !== null}
            aria-describedby={visible('firstName') && errors.firstName ? `first-name-error-${seat.seat}` : undefined}
            placeholder="First name"
            maxLength={100}
            value={firstName}
            readOnly={forSelf}
            onChange={(event) => {
              markTouched('firstName')
              onName('firstName', event.target.value)
            }}
          />
          {visible('firstName') && errors.firstName && (
            <span className="field-error" id={`first-name-error-${seat.seat}`} role="alert">
              {errors.firstName}
            </span>
          )}
        </label>
        <label>
          <span className="muted">Last name</span>
          <input
            type="text"
            aria-label={`Last name for seat ${seat.seat}`}
            aria-invalid={visible('lastName') && errors.lastName !== null}
            aria-describedby={visible('lastName') && errors.lastName ? `last-name-error-${seat.seat}` : undefined}
            placeholder="Last name"
            maxLength={100}
            value={lastName}
            readOnly={forSelf}
            onChange={(event) => {
              markTouched('lastName')
              onName('lastName', event.target.value)
            }}
          />
          {visible('lastName') && errors.lastName && (
            <span className="field-error" id={`last-name-error-${seat.seat}`} role="alert">
              {errors.lastName}
            </span>
          )}
        </label>
      </div>
      <label className="passenger-detail-field">
        <span className="muted">Passport number (required)</span>
        <input
          type="text"
          aria-label={`Passport number for seat ${seat.seat}`}
          aria-invalid={visible('passport') && errors.passport !== null}
          aria-describedby={visible('passport') && errors.passport ? `passport-error-${seat.seat}` : undefined}
          placeholder="Required"
          maxLength={20}
          required
          value={draft.passportNumber}
          onChange={(event) => {
            markTouched('passport')
            onPassport(event.target.value)
          }}
        />
        {visible('passport') && errors.passport && (
          <span className="field-error" id={`passport-error-${seat.seat}`} role="alert">
            {errors.passport}
          </span>
        )}
      </label>
      <label className="passenger-detail-field">
        <span className="muted">Phone number (required)</span>
        <input
          type="tel"
          aria-label={`Phone number for seat ${seat.seat}`}
          aria-invalid={visible('phone') && errors.phone !== null}
          aria-describedby={visible('phone') && errors.phone ? `phone-error-${seat.seat}` : undefined}
          placeholder="+48600123456"
          pattern="[+][1-9][0-9]{6,14}"
          title="Start with + and use only digits, e.g. +48600123456"
          maxLength={16}
          required
          value={draft.phoneNumber}
          onChange={(event) => {
            markTouched('phone')
            onPhone(event.target.value)
          }}
        />
        {visible('phone') && errors.phone && (
          <span className="field-error" id={`phone-error-${seat.seat}`} role="alert">
            {errors.phone}
          </span>
        )}
      </label>
      <label className="passenger-detail-field">
        <span className="muted">Date of birth (required)</span>
        <input
          type="date"
          aria-label={`Date of birth for seat ${seat.seat}`}
          aria-invalid={visible('birth') && errors.birth !== null}
          aria-describedby={visible('birth') && errors.birth ? `birth-error-${seat.seat}` : undefined}
          required
          value={draft.dateOfBirth}
          onChange={(event) => {
            markTouched('birth')
            onBirth(event.target.value)
          }}
        />
        {visible('birth') && errors.birth && (
          <span className="field-error" id={`birth-error-${seat.seat}`} role="alert">
            {errors.birth}
          </span>
        )}
      </label>
      {forSelf && !hasProfileName && (
        <p className="muted departure-passenger">
          Your profile has no name yet. <Link to="/profile">Add your details</Link>.
        </p>
      )}
    </li>
  )
}
