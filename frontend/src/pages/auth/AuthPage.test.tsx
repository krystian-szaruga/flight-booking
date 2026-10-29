import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { VERIFIED_USER } from '../../test/fixtures'
import '../../test/isolate'
import { renderApp } from '../../test/renderApp'
import { problem, startBackend } from '../../test/server'

const SIGNED_OUT = { path: '/api/auth/me', body: null }

async function fillCredentials(user: ReturnType<typeof userEvent.setup>, email: string) {
  await user.type(screen.getByLabelText('Email'), email)
  await user.type(screen.getByLabelText('Password'), 'correct-horse')
}

describe('signing in', () => {
  it('when a verified traveller signs in then they return to the page they were sent away from', async () => {
    const backend = startBackend([
      SIGNED_OUT,
      { method: 'POST', path: '/api/auth/login', body: VERIFIED_USER },
      { path: '/api/v1/reservations', body: [] },
    ])
    const user = userEvent.setup()
    renderApp('/auth?redirect=%2Freservations')

    await fillCredentials(user, 'ada@example.com')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(backend.lastCall(/POST \/api\/auth\/login/).body).toEqual({
      email: 'ada@example.com',
      password: 'correct-horse',
    })
    expect(await screen.findByRole('heading', { name: 'My reservations' })).toBeInTheDocument()
    expect(await screen.findByText(/You have no reservations yet/)).toBeInTheDocument()
  })

  it('when the password is wrong then the backend message is shown and the traveller stays on the form', async () => {
    const backend = startBackend([SIGNED_OUT, problem('/api/auth/login', 'Bad credentials', 401)])
    const user = userEvent.setup()
    renderApp('/auth')

    await fillCredentials(user, 'ada@example.com')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Bad credentials')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(backend.callsTo(/GET \/api\/v1\/reservations/)).toHaveLength(0)
  })
})

describe('signing up', () => {
  it('when a new traveller registers then the verification code screen appears and a valid code signs them in', async () => {
    const backend = startBackend([
      SIGNED_OUT,
      { method: 'POST', path: '/api/auth/register', body: { stage: 'CODE_SENT', retryAfterSeconds: 60 } },
      { method: 'POST', path: '/api/auth/verify', body: VERIFIED_USER },
      { path: '/api/v1/reservations', body: [] },
    ])
    const user = userEvent.setup()
    renderApp('/auth?redirect=%2Freservations')

    await user.click(screen.getByRole('button', { name: 'No account? Sign up' }))
    await fillCredentials(user, 'ada@example.com')
    await user.click(screen.getByRole('button', { name: 'Sign up' }))

    expect(backend.lastCall(/POST \/api\/auth\/register/).body).toEqual({
      email: 'ada@example.com',
      password: 'correct-horse',
    })
    expect(await screen.findByRole('heading', { name: 'Enter the code from your email' })).toBeInTheDocument()

    await user.type(screen.getByLabelText('Verification code'), '418906')
    await user.click(screen.getByRole('button', { name: 'Verify email' }))

    expect(backend.lastCall(/POST \/api\/auth\/verify/).body).toEqual({
      email: 'ada@example.com',
      code: '418906',
    })
    expect(await screen.findByRole('heading', { name: 'My reservations' })).toBeInTheDocument()
    expect(await screen.findByText(/You have no reservations yet/)).toBeInTheDocument()
  })

  it('when the emailed code is wrong then a readable error is shown and no session is opened', async () => {
    const backend = startBackend([
      SIGNED_OUT,
      { method: 'POST', path: '/api/auth/register', body: { stage: 'CODE_SENT', retryAfterSeconds: 60 } },
      problem('/api/auth/verify', 'invalid_or_expired_code', 400),
    ])
    const user = userEvent.setup()
    renderApp('/auth')

    await user.click(screen.getByRole('button', { name: 'No account? Sign up' }))
    await fillCredentials(user, 'ada@example.com')
    await user.click(screen.getByRole('button', { name: 'Sign up' }))
    await user.type(await screen.findByLabelText('Verification code'), '000000')
    await user.click(screen.getByRole('button', { name: 'Verify email' }))

    expect(await screen.findByText('That code is invalid or has expired.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Enter the code from your email' })).toBeInTheDocument()
    expect(backend.callsTo(/GET \/api\/v1\/reservations/)).toHaveLength(0)
  })
})
