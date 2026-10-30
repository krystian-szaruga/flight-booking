import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import '../test/isolate'
import { renderApp } from '../test/renderApp'
import { startBackend } from '../test/server'

describe('currency selection', () => {
  it('when the traveller picks a currency then it is stored and the flight search uses it', async () => {
    localStorage.clear()
    const backend = startBackend([
      { path: '/api/v1/currencies', body: { default: 'PLN', supported: ['PLN', 'EUR', 'USD'] } },
      { path: '/api/v1/flights', body: [] },
    ])
    const user = userEvent.setup()
    renderApp('/results?origin=WAW&destination=LIS&from=2026-11-10')

    await user.selectOptions(await screen.findByLabelText('Currency'), 'EUR')

    expect(localStorage.getItem('currency')).toBe('EUR')
    await waitFor(() => {
      const call = backend.callsTo(/GET \/api\/v1\/flights/).at(-1)
      expect(call?.search.get('currency')).toBe('EUR')
    })
  })
})
