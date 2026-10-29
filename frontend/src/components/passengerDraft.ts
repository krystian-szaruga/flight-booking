import type { PassengerType } from '../api/types'

export interface PassengerDraft {
  passengerType: PassengerType
  firstName: string
  lastName: string
}

export const EMPTY_DRAFT: PassengerDraft = { passengerType: 'OTHER', firstName: '', lastName: '' }

export function isDraftReady(draft: PassengerDraft): boolean {
  if (draft.passengerType === 'SELF') return true
  return draft.firstName.trim().length > 0 && draft.lastName.trim().length > 0
}
