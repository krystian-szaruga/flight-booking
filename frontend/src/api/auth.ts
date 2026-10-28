import type { UserResponse } from './types'

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function send(method: 'GET' | 'POST', path: string, body?: unknown): Promise<UserResponse | null> {
  const response = await fetch(path, {
    method,
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (response.status === 204) return null
  if (!response.ok) throw new ApiError(response.status, await detail(response))
  return (await response.json()) as UserResponse
}

async function detail(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { detail?: string; title?: string }
    return body.detail ?? body.title ?? `Request failed with status ${response.status}`
  } catch {
    return `Request failed with status ${response.status}`
  }
}

export function fetchCurrentUser(): Promise<UserResponse | null> {
  return send('GET', '/api/auth/me')
}

export function login(email: string, password: string): Promise<UserResponse | null> {
  return send('POST', '/api/auth/login', { email, password })
}

export function register(email: string, password: string): Promise<UserResponse | null> {
  return send('POST', '/api/auth/register', { email, password })
}

export function logout(): Promise<UserResponse | null> {
  return send('POST', '/api/auth/logout')
}
