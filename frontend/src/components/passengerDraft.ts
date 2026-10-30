import type { PassengerType } from '../api/types'

export interface PassengerDraft {
  passengerType: PassengerType
  firstName: string
  lastName: string
  passportNumber: string
  phoneNumber: string
  dateOfBirth: string
}

export const EMPTY_DRAFT: PassengerDraft = {
  passengerType: 'OTHER',
  firstName: '',
  lastName: '',
  passportNumber: '',
  phoneNumber: '',
  dateOfBirth: '',
}

export function isDraftReady(draft: PassengerDraft): boolean {
  if (draft.passportNumber.trim().length === 0) return false
  if (!PHONE_PATTERN.test(draft.phoneNumber.trim())) return false
  if (draft.dateOfBirth.trim().length === 0) return false
  if (isFutureDate(draft.dateOfBirth)) return false
  if (draft.passengerType === 'SELF') return true
  return draft.firstName.trim().length > 0 && draft.lastName.trim().length > 0
}

export const PHONE_PATTERN = /^\+[1-9][0-9]{6,14}$/

export function isFutureDate(value: string): boolean {
  return value > todayIso()
}

function todayIso(): string {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}
