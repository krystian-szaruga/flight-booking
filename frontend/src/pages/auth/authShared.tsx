import { ApiError } from '../../api/client'

export function EyeIcon() {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false">
      <path
        d="M12 5c-5 0-9 4.5-10 7 1 2.5 5 7 10 7s9-4.5 10-7c-1-2.5-5-7-10-7z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
      <circle cx="12" cy="12" r="3" fill="none" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  )
}

export function EyeOffIcon() {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false">
      <path d="M3 3l18 18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      <path
        d="M10.6 6.2A10.9 10.9 0 0 1 12 6c5 0 9 4.5 10 7a16 16 0 0 1-3.6 4.2M6.4 7.6A16.4 16.4 0 0 0 2 13c1 2.5 5 7 10 7a10.6 10.6 0 0 0 4.4-1"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

export function safeRedirect(raw: string | null): string {
  if (!raw) return '/'
  if (!raw.startsWith('/') || raw.startsWith('//')) return '/'
  return raw
}

export function describeAuthError(error: ApiError): string {
  if (error.message === 'invalid_or_expired_code') return 'That code is invalid or has expired.'
  if (error.status === 429) return 'Too many attempts. Wait a moment and try again.'
  return error.message
}
