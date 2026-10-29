import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { reservation, seatMap, VERIFIED_USER } from '../../test/fixtures'
import '../../test/isolate'
import { renderApp } from '../../test/renderApp'
import { problem, startBackend } from '../../test/server'

const SIGNED_IN = { path: '/api/auth/me', body: VERIFIED_USER }

function seatMapRoute(overrides: Parameters<typeof seatMap>[0] = {}) {
  return { path: '/api/v1/flights/LO26/seats', body: seatMap(overrides) }
}

function heldSeatMapRoute() {
  return seatMapRoute({
    seats: seatMap().seats.map((s) => (s.seat === '3A' ? { ...s, status: 'HELD' as const } : s)),
  })
}

async function namePassenger(user: ReturnType<typeof userEvent.setup>, seat: string) {
  await user.type(screen.getByLabelText(`First name for seat ${seat}`), 'Grace')
  await user.type(screen.getByLabelText(`Last name for seat ${seat}`), 'Hopper')
}

describe('holding seats', () => {
  it('when a traveller picks a seat and names the passenger then the hold is sent for that seat and listed', async () => {
    const backend = startBackend([
      SIGNED_IN,
      seatMapRoute(),
      { method: 'POST', path: '/api/v1/reservations/flights/LO26', body: reservation(), once: true },
      heldSeatMapRoute(),
    ])
    const user = userEvent.setup()
    renderApp('/flights/LO26/seats')

    await user.click(await screen.findByRole('gridcell', { name: '3A, economy, available' }))
    await namePassenger(user, '3A')
    await user.click(screen.getByRole('button', { name: 'Hold seats' }))

    expect(backend.lastCall(/POST \/api\/v1\/reservations\/flights\/LO26/).body).toEqual({
      seats: [{ seat: '3A', passengerType: 'OTHER', firstName: 'Grace', lastName: 'Hopper' }],
    })
    expect(await screen.findByRole('heading', { name: 'Seat on hold' })).toBeInTheDocument()
    expect(screen.getByText(/Pay within/)).toBeInTheDocument()
    expect(await screen.findByRole('gridcell', { name: '3A, economy, held' })).toBeDisabled()
  })

  it('when a traveller books a seat for themselves then the profile name is sent instead of typed names', async () => {
    const backend = startBackend([
      SIGNED_IN,
      seatMapRoute(),
      { method: 'POST', path: '/api/v1/reservations/flights/LO26', body: reservation(), once: true },
      seatMapRoute(),
    ])
    const user = userEvent.setup()
    renderApp('/flights/LO26/seats')

    await user.click(await screen.findByRole('gridcell', { name: '3A, economy, available' }))
    await user.click(screen.getByRole('radio', { name: 'For me' }))
    expect(screen.getByLabelText('First name for seat 3A')).toHaveValue('Ada')
    expect(screen.getByLabelText('Last name for seat 3A')).toHaveValue('Lovelace')
    await user.click(screen.getByRole('button', { name: 'Hold seats' }))

    expect(backend.lastCall(/POST \/api\/v1\/reservations\/flights\/LO26/).body).toEqual({
      seats: [{ seat: '3A', passengerType: 'SELF' }],
    })
  })

  it('when a hold is released after confirming then the chosen seats are sent to the release endpoint and the hold shrinks', async () => {
    const twoSeats = [
      { seat: '3A', firstName: 'Grace', lastName: 'Hopper' },
      { seat: '3B', firstName: 'Grace', lastName: 'Hopper' },
    ]
    const backend = startBackend([
      SIGNED_IN,
      seatMapRoute(),
      { method: 'POST', path: '/api/v1/reservations/flights/LO26', body: reservation({ seats: twoSeats }), once: true },
      {
        method: 'DELETE',
        path: '/api/v1/reservations/res-1/seats',
        body: reservation({ seats: [twoSeats[1]] }),
      },
      seatMapRoute(),
    ])
    const user = userEvent.setup()
    renderApp('/flights/LO26/seats')

    await user.click(await screen.findByRole('gridcell', { name: '3A, economy, available' }))
    await namePassenger(user, '3A')
    await user.click(await screen.findByRole('gridcell', { name: '3B, economy, available' }))
    await namePassenger(user, '3B')
    await user.click(screen.getByRole('button', { name: 'Hold seats' }))

    const hold = (await screen.findByRole('heading', { name: 'Seat on hold' })).parentElement as HTMLElement
    await user.click(within(hold).getByRole('checkbox', { name: /3A/ }))
    await user.click(within(hold).getByRole('button', { name: 'Release selected (1)' }))

    const dialog = await screen.findByRole('dialog')
    expect(dialog).toHaveTextContent('Release 1 seat(s)?')
    await user.click(within(dialog).getByRole('button', { name: 'Release' }))

    expect(backend.lastCall(/DELETE \/api\/v1\/reservations\/res-1\/seats/).body).toEqual({ seats: ['3A'] })
    expect(await screen.findByRole('checkbox', { name: /3B/ })).toBeInTheDocument()
    expect(screen.queryByRole('checkbox', { name: /3A/ })).not.toBeInTheDocument()
  })

  it('when a cabin is chosen then seats outside it cannot be picked and only that cabin fare is shown', async () => {
    const backend = startBackend([SIGNED_IN, seatMapRoute()])
    const user = userEvent.setup()
    renderApp('/flights/LO26/seats?cabin=BUSINESS')

    expect(await screen.findByRole('gridcell', { name: '3A, economy, available' })).toBeDisabled()
    await user.click(screen.getByRole('gridcell', { name: '1A, business, available' }))

    expect(screen.getByRole('gridcell', { name: '1A, business, selected' })).toBeInTheDocument()
    expect(screen.getByText(/business seats are selectable/)).toBeInTheDocument()
    expect(backend.callsTo(/POST/)).toHaveLength(0)
  })

  it('when the session runs out while holding a seat then the traveller is sent back to sign in', async () => {
    startBackend([
      SIGNED_IN,
      seatMapRoute(),
      problem('/api/v1/reservations/flights/LO26', 'Unauthorised', 401),
    ])
    const user = userEvent.setup()
    renderApp('/flights/LO26/seats')

    await user.click(await screen.findByRole('gridcell', { name: '3A, economy, available' }))
    await namePassenger(user, '3A')
    await user.click(screen.getByRole('button', { name: 'Hold seats' }))

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })
})

describe('guarding the seat map', () => {
  it('when the traveller has not verified their email then the seat map is refused and they are sent to sign in', async () => {
    const backend = startBackend([
      { path: '/api/auth/me', body: { ...VERIFIED_USER, emailVerifiedAt: null } },
      seatMapRoute(),
    ])
    renderApp('/flights/LO26/seats')

    expect(await screen.findByRole('heading', { name: 'Enter the code from your email' })).toBeInTheDocument()
    expect(screen.queryByRole('gridcell', { name: '3A, economy, available' })).not.toBeInTheDocument()
    expect(backend.callsTo(/POST/)).toHaveLength(0)
  })
})
