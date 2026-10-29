import type { ReactNode } from 'react'
import { render } from '@testing-library/react'
import { vi } from 'vitest'
import { App } from '../App'

const mockInitialEntry = vi.hoisted(() => ({ current: '/' }))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    BrowserRouter: ({ children }: { children: ReactNode }) => (
      <actual.MemoryRouter initialEntries={[mockInitialEntry.current]}>{children}</actual.MemoryRouter>
    ),
  }
})

export function renderApp(url: string) {
  mockInitialEntry.current = url
  return render(<App />)
}
