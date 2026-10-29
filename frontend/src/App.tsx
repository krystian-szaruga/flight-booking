import { BrowserRouter, Link, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { SessionBadge } from './components/SessionBadge'
import { ThemeToggle } from './components/ThemeToggle'
import { AuthPage } from './pages/AuthPage'
import { LandingPage } from './pages/LandingPage'
import { MyReservationsPage } from './pages/MyReservationsPage'
import { SearchPage } from './pages/SearchPage'
import { ResultsPage } from './pages/ResultsPage'
import { SeatMapPage } from './pages/SeatMapPage'

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
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
              <div className="header-actions">
                <SessionBadge />
                <ThemeToggle />
              </div>
            </div>
          </header>
          <main className="app-main">
            <Routes>
              <Route path="/" element={<LandingPage />} />
              <Route path="/search" element={<SearchPage />} />
              <Route path="/results" element={<ResultsPage />} />
              <Route path="/auth" element={<AuthPage />} />
              <Route
                path="/reservations"
                element={
                  <ProtectedRoute>
                    <MyReservationsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/flights/:flightId/seats"
                element={
                  <ProtectedRoute>
                    <SeatMapPage />
                  </ProtectedRoute>
                }
              />
              <Route path="*" element={<p>Page not found.</p>} />
            </Routes>
          </main>
        </div>
      </AuthProvider>
    </BrowserRouter>
  )
}
