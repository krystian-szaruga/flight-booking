import { BrowserRouter, Link, Route, Routes } from 'react-router-dom'
import { SearchPage } from './pages/SearchPage'
import { ResultsPage } from './pages/ResultsPage'
import { SeatMapPage } from './pages/SeatMapPage'

export function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <header className="app-header">
          <Link to="/" className="brand">
            Flight booking
          </Link>
        </header>
        <main className="app-main">
          <Routes>
            <Route path="/" element={<SearchPage />} />
            <Route path="/results" element={<ResultsPage />} />
            <Route path="/flights/:flightId/seats" element={<SeatMapPage />} />
            <Route path="*" element={<p>Page not found.</p>} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}
