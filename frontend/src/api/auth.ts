import { fail } from './client'
import type { UserResponse } from './types'

export { ApiError } from './client'

export type RegistrationStage = 'CODE_SENT' | 'EMAIL_VERIFIED'

export interface RegistrationResponse {
  stage: RegistrationStage
  retryAfterSeconds: number
}

export interface ResendResponse {
  retryAfterSeconds: number
}

async function send<T>(method: 'GET' | 'POST' | 'PUT' | 'DELETE', path: string, body?: unknown): Promise<T> {
  const response = await fetch(path, {
    method,
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (response.status === 204) return undefined as T
  if (!response.ok) await fail(response)
  return (await response.json()) as T
}

export function fetchCurrentUser(): Promise<UserResponse | null> {
  return send<UserResponse | null>('GET', '/api/auth/me')
}

export function login(email: string, password: string): Promise<UserResponse | null> {
  return send<UserResponse | null>('POST', '/api/auth/login', { email, password })
}

export function register(email: string, password: string): Promise<RegistrationResponse> {
  return send<RegistrationResponse>('POST', '/api/auth/register', { email, password })
}

export function verifyEmail(email: string, code: string): Promise<UserResponse | null> {
  return send<UserResponse | null>('POST', '/api/auth/verify', { email, code })
}

export function resendVerification(email: string): Promise<ResendResponse | null> {
  return send<ResendResponse | null>('POST', '/api/auth/verify/resend', { email })
}

export function logout(): Promise<void> {
  return send<void>('POST', '/api/auth/logout')
}

export function updateProfile(firstName: string, lastName: string): Promise<UserResponse> {
  return send<UserResponse>('PUT', '/api/auth/me', { firstName, lastName })
}
