import { BrowserRouter, Link, Route, Routes } from 'react-router-dom'
import { ThemeToggle } from './components/ThemeToggle'
import { LandingPage } from './pages/LandingPage'
import { SearchPage } from './pages/SearchPage'
import { ResultsPage } from './pages/ResultsPage'
import { SeatMapPage } from './pages/SeatMapPage'

export function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <header className="app-header">
          <div className="app-header-inner">
            <Link to="/" className="brand">
              <span className="brand-mark" aria-hidden="true">
                ✈
              </span>
              <span className="brand-text">
                Flight booking
                <small>Search · Seats</small>
              </span>
            </Link>
            <ThemeToggle />
          </div>
        </header>
        <main className="app-main">
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/results" element={<ResultsPage />} />
            <Route path="/flights/:flightId/seats" element={<SeatMapPage />} />
            <Route path="*" element={<p>Page not found.</p>} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}
