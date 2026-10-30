import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { reservation, VERIFIED_USER } from '../test/fixtures'
import '../test/isolate'
import { renderApp } from '../test/renderApp'
import { startBackend } from '../test/server'

const SIGNED_IN = { path: '/api/auth/me', body: VERIFIED_USER }

describe('my reservations', () => {
  it('when a traveller opens their reservations then the holds held on the server are listed', async () => {
    startBackend([
      SIGNED_IN,
      {
        path: '/api/v1/reservations',
        body: [
          reservation(),
          reservation({
            reservationId: 'res-2',
            status: 'CANCELLED',
            expiresAt: null,
            seats: [{ seat: '1A', firstName: 'Ada', lastName: 'Lovelace' }],
          }),
        ],
      },
    ])
    renderApp('/reservations')

    expect(await screen.findByRole('heading', { name: 'My reservations' })).toBeInTheDocument()
    expect(await screen.findByText('pending')).toBeInTheDocument()
    expect(screen.getByText('cancelled')).toBeInTheDocument()
    expect(screen.getByText(/Held for/)).toBeInTheDocument()
  })

  it('when a traveller cancels a reservation and confirms then the whole reservation is cancelled on the server and the row updates', async () => {
    const backend = startBackend([
      SIGNED_IN,
      { path: '/api/v1/reservations', body: [reservation()] },
      {
        method: 'DELETE',
        path: '/api/v1/reservations/res-1',
        body: reservation({ status: 'CANCELLED', expiresAt: null, seats: [] }),
      },
    ])
    const user = userEvent.setup()
    renderApp('/reservations')

    const row = (await screen.findByText('pending')).closest('li') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Cancel all' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Cancel reservation' }))

    expect(backend.lastCall(/DELETE \/api\/v1\/reservations\/res-1/).path).toBe('/api/v1/reservations/res-1')
    expect(await screen.findByText('cancelled')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancel all' })).not.toBeInTheDocument()
  })

  it('when the traveller declines the confirmation then nothing is cancelled', async () => {
    const backend = startBackend([SIGNED_IN, { path: '/api/v1/reservations', body: [reservation()] }])
    const user = userEvent.setup()
    renderApp('/reservations')

    const row = (await screen.findByText('pending')).closest('li') as HTMLElement
    await user.click(within(row).getByRole('button', { name: 'Cancel all' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Keep it' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByText('pending')).toBeInTheDocument()
    expect(backend.callsTo(/DELETE/)).toHaveLength(0)
  })

  it('when a paid reservation is listed then its row shows the amount that was paid', async () => {
    startBackend([
      SIGNED_IN,
      {
        path: '/api/v1/reservations',
        body: [
          reservation({
            status: 'CONFIRMED',
            expiresAt: null,
            paid: {
              amount: 120,
              currency: 'EUR',
              cardLast4: '4242',
              paidAt: '2026-01-01T10:00:00Z',
            },
          }),
        ],
      },
    ])
    renderApp('/reservations')

    expect(await screen.findByText('120.00 EUR')).toBeInTheDocument()
    expect(screen.getByText(/by card •••• 4242/)).toBeInTheDocument()
  })

  it('when a guest opens the reservations page then they are sent to sign in', async () => {
    startBackend([{ path: '/api/auth/me', body: null }])
    renderApp('/reservations')

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument()
  })
})
