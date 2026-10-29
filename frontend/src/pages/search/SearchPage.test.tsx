import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { flight, LISBON, WARSAW } from '../../test/fixtures'
import '../../test/isolate'
import { renderApp } from '../../test/renderApp'
import { startBackend } from '../../test/server'
import { shift, tomorrow } from './searchCriteria'

function airportsRoute() {
  return { path: '/api/v1/airports', body: [WARSAW, LISBON] }
}

function localToday(): string {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

async function pickBothCities(user: ReturnType<typeof userEvent.setup>) {
  const [from, to] = screen.getAllByRole('combobox')
  await user.type(from, 'Warsaw')
  await user.click(await screen.findByRole('option', { name: /Warsaw/ }))
  await user.type(to, 'Lisbon')
  await user.click(await screen.findByRole('option', { name: /Lisbon/ }))
}

describe('flight search', () => {
  it('when a traveller searches from a date in the future then the request carries their criteria and the flights are listed', async () => {
    const departureDate = shift(tomorrow(), 7)
    const backend = startBackend([
      airportsRoute(),
      {
        path: '/api/v1/flights',
        body: [
          flight({ departureAt: `${departureDate}T08:00:00Z`, arrivalAt: `${departureDate}T12:30:00Z` }),
          flight({
            flightId: 'TP1234',
            flightNumber: 'TP 1234',
            departureAt: `${shift(departureDate, 1)}T09:00:00Z`,
            arrivalAt: `${shift(departureDate, 1)}T13:30:00Z`,
          }),
        ],
      },
    ])
    const user = userEvent.setup()
    renderApp(`/search?from=${departureDate}`)

    expect(await screen.findByLabelText('Departure')).toHaveValue(departureDate)
    await pickBothCities(user)
    await user.selectOptions(screen.getByLabelText('Cabin'), 'BUSINESS')
    await user.click(screen.getByRole('button', { name: 'Search flights' }))

    const query = backend.lastCall(/GET \/api\/v1\/flights/).search
    expect(query.get('origin')).toBe('WAW')
    expect(query.get('destination')).toBe('LIS')
    expect(query.get('cabin')).toBe('BUSINESS')
    expect(query.get('from')).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    expect(query.get('from')).toBe(departureDate)
    expect(query.get('to')).toBeNull()

    expect(await screen.findByText('LO 26')).toBeInTheDocument()
    expect(await screen.findByText('TP 1234')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /WAW to LIS/ })).toBeInTheDocument()
    expect(screen.getAllByRole('heading', { level: 3 })).toHaveLength(2)
  })

  it('when the traveller searches with a date in the past then nothing is searched until a valid date is chosen', async () => {
    const backend = startBackend([airportsRoute(), { path: '/api/v1/flights', body: [flight()] }])
    const user = userEvent.setup()
    renderApp(`/search?from=${shift(localToday(), -3)}`)

    await pickBothCities(user)
    await user.click(screen.getByRole('button', { name: 'Search flights' }))

    expect(backend.callsTo(/GET \/api\/v1\/flights/)).toHaveLength(0)
  })

  it('when the traveller types an unknown city then no search is sent and the airport must be picked', async () => {
    const backend = startBackend([airportsRoute()])
    const user = userEvent.setup()
    renderApp('/search')

    const [from, to] = screen.getAllByRole('combobox')
    await user.type(from, 'Atlantis')
    await user.tab()
    await user.type(to, 'Lisbon')
    await user.click(await screen.findByRole('option', { name: /Lisbon/ }))
    await user.click(screen.getByRole('button', { name: 'Search flights' }))

    expect(await screen.findByText('Pick both airports from the suggestions.')).toBeInTheDocument()
    expect(backend.callsTo(/GET \/api\/v1\/flights/)).toHaveLength(0)
  })

  it('when the departure city equals the arrival city then the search is rejected before it is sent', async () => {
    const backend = startBackend([airportsRoute()])
    const user = userEvent.setup()
    renderApp('/search')

    const [from, to] = screen.getAllByRole('combobox')
    await user.type(from, 'Warsaw')
    await user.click(await screen.findByRole('option', { name: /Warsaw/ }))
    await user.type(to, 'Warsaw')
    await user.click(await screen.findByRole('option', { name: /Warsaw/ }))
    await user.click(screen.getByRole('button', { name: 'Search flights' }))

    expect(await screen.findByText('Origin and destination must differ.')).toBeInTheDocument()
    expect(backend.callsTo(/GET \/api\/v1\/flights/)).toHaveLength(0)
  })
})
