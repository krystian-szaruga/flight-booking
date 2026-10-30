import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { reservation, payment, seatMap, VERIFIED_USER } from '../../test/fixtures'
import '../../test/isolate'
import { renderApp } from '../../test/renderApp'
import { problem, startBackend } from '../../test/server'

const SIGNED_IN = { path: '/api/auth/me', body: VERIFIED_USER }

function seatMapRoute(overrides: Parameters<typeof seatMap>[0] = {}) {
  return { path: '/api/v1/flights/LO26/seats', body: seatMap(overrides) }
}

async function namePassenger(user: ReturnType<typeof userEvent.setup>, seat: string) {
  await user.type(screen.getByLabelText(`First name for seat ${seat}`), 'Grace')
  await user.type(screen.getByLabelText(`Last name for seat ${seat}`), 'Hopper')
  await user.type(screen.getByLabelText(`Passport number for seat ${seat}`), 'X1234567')
}

describe('buying seats', () => {
  it('when a traveller picks a seat and names the passenger then the purchase opens the checkout with that passenger', async () => {
    const backend = startBackend([
      SIGNED_IN,
      seatMapRoute(),
      { method: 'POST', path: '/api/v1/reservations/flights/LO26', body: reservation(), once: true },
    ])
    const user = userEvent.setup()
    renderApp('/flights/LO26/seats')

    await user.click(await screen.findByRole('gridcell', { name: '3A, economy, available' }))
    await namePassenger(user, '3A')
    await user.click(screen.getByRole('button', { name: 'Buy' }))

    expect(backend.lastCall(/POST \/api\/v1\/reservations\/flights\/LO26/).body).toEqual({
      seats: [
        {
          seat: '3A',
          passengerType: 'OTHER',
          firstName: 'Grace',
          lastName: 'Hopper',
          passportNumber: 'X1234567',
        },
      ],
    })
    expect(await screen.findByRole('heading', { name: 'Complete your booking' })).toBeInTheDocument()
    expect(screen.getByText('Grace Hopper')).toBeInTheDocument()
    expect(screen.getByText(/LO 26 · WAW to LIS/)).toBeInTheDocument()
  })

  it('when a passenger enters a passport number then it is sent with the hold and checkout shows only the last four digits', async () => {
    const backend = startBackend([
      SIGNED_IN,
      seatMapRoute(),
      {
        method: 'POST',
        path: '/api/v1/reservations/flights/LO26',
        body: reservation({
          seats: [{ seat: '3A', firstName: 'Grace', lastName: 'Hopper', passportNumber: '••••4567' }],
        }),
        once: true,
      },
    ])
    const user = userEvent.setup()
    renderApp('/flights/LO26/seats')

    await user.click(await screen.findByRole('gridcell', { name: '3A, economy, available' }))
    await namePassenger(user, '3A')
    await user.click(screen.getByRole('button', { name: 'Buy' }))

    expect(backend.lastCall(/POST \/api\/v1\/reservations\/flights\/LO26/).body).toEqual({
      seats: [
        {
          seat: '3A',
          passengerType: 'OTHER',
          firstName: 'Grace',
          lastName: 'Hopper',
          passportNumber: 'X1234567',
        },
      ],
    })
    expect(await screen.findByRole('heading', { name: 'Complete your booking' })).toBeInTheDocument()
    expect(screen.getByDisplayValue('••••4567')).toBeInTheDocument()
    expect(screen.queryByDisplayValue('X1234567')).not.toBeInTheDocument()
  })

  it('when a traveller books a seat for themselves then the profile name is sent instead of typed names', async () => {
    const backend = startBackend([
      SIGNED_IN,
      seatMapRoute(),
      { method: 'POST', path: '/api/v1/reservations/flights/LO26', body: reservation(), once: true },
    ])
    const user = userEvent.setup()
    renderApp('/flights/LO26/seats')

    await user.click(await screen.findByRole('gridcell', { name: '3A, economy, available' }))
    await user.click(screen.getByRole('radio', { name: 'For me' }))
    expect(screen.getByLabelText('First name for seat 3A')).toHaveValue('Ada')
    expect(screen.getByLabelText('Last name for seat 3A')).toHaveValue('Lovelace')
    expect(screen.getByRole('button', { name: 'Buy' })).toBeDisabled()
    await user.type(screen.getByLabelText('Passport number for seat 3A'), 'X1234567')
    await user.click(screen.getByRole('button', { name: 'Buy' }))

    expect(backend.lastCall(/POST \/api\/v1\/reservations\/flights\/LO26/).body).toEqual({
      seats: [{ seat: '3A', passengerType: 'SELF', passportNumber: 'X1234567' }],
    })
    expect(await screen.findByRole('heading', { name: 'Complete your booking' })).toBeInTheDocument()
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

  it('when the session runs out while buying a seat then the traveller is sent back to sign in', async () => {
    startBackend([
      SIGNED_IN,
      seatMapRoute(),
      problem('/api/v1/reservations/flights/LO26', 'Unauthorised', 401),
    ])
    const user = userEvent.setup()
    renderApp('/flights/LO26/seats')

    await user.click(await screen.findByRole('gridcell', { name: '3A, economy, available' }))
    await namePassenger(user, '3A')
    await user.click(screen.getByRole('button', { name: 'Buy' }))

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })
})

describe('paying at checkout', () => {
  async function openCheckout() {
    const user = userEvent.setup()
    renderApp('/flights/LO26/seats')
    await user.click(await screen.findByRole('gridcell', { name: '3A, economy, available' }))
    await namePassenger(user, '3A')
    await user.click(screen.getByRole('button', { name: 'Buy' }))
    await screen.findByRole('heading', { name: 'Complete your booking' })
    return user
  }

  const CHECKOUT_SETUP = [
    SIGNED_IN,
    seatMapRoute(),
    { method: 'POST', path: '/api/v1/reservations/flights/LO26', body: reservation(), once: true },
  ]

  it('when the traveller pays with the approving card then the payment is sent and the booking is confirmed', async () => {
    const backend = startBackend([
      ...CHECKOUT_SETUP,
      { method: 'POST', path: '/api/v1/reservations/res-1/payment', body: payment(), once: true },
    ])
    const user = await openCheckout()

    expect(screen.getByText('120.00 EUR')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Pay 120.00 EUR' }))
    const dialog = await screen.findByRole('dialog')
    expect(dialog).toHaveTextContent('Choose a card')
    await user.click(within(dialog).getByRole('button', { name: 'Pay 120.00 EUR' }))

    expect(backend.lastCall(/POST \/api\/v1\/reservations\/res-1\/payment/).body).toEqual({
      idempotencyKey: expect.any(String),
      cardNumber: '4242424242424242',
      cardHolder: 'Ada Lovelace',
      currency: 'PLN',
    })
    expect(await screen.findByRole('heading', { name: 'Booking confirmed' })).toBeInTheDocument()
    expect(screen.getByText(/card •••• 4242/)).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('when the card is declined then the reason is shown in the card dialog and the booking stays payable', async () => {
    const backend = startBackend([
      ...CHECKOUT_SETUP,
      {
        method: 'POST',
        path: '/api/v1/reservations/res-1/payment',
        status: 402,
        body: payment({ status: 'DECLINED', cardLast4: '0002', declineReason: 'Card declined by issuer' }),
        once: true,
      },
    ])
    const user = await openCheckout()

    await user.click(screen.getByRole('button', { name: 'Pay 120.00 EUR' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('radio', { name: /declines/ }))
    await user.click(within(dialog).getByRole('button', { name: 'Pay 120.00 EUR' }))

    expect(backend.lastCall(/POST \/api\/v1\/reservations\/res-1\/payment/).body).toEqual({
      idempotencyKey: expect.any(String),
      cardNumber: '4000000000000002',
      cardHolder: 'Ada Lovelace',
      currency: 'PLN',
    })
    expect(await screen.findByText('Card declined by issuer')).toBeInTheDocument()
    expect(screen.getByRole('dialog')).toBeInTheDocument()
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
