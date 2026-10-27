import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Cabin } from '../api/types'

const IATA = /^[A-Za-z]{3}$/

const CABINS: { value: '' | Cabin; label: string }[] = [
  { value: '', label: 'Any cabin' },
  { value: 'ECONOMY', label: 'Economy' },
  { value: 'PREMIUM_ECONOMY', label: 'Premium economy' },
  { value: 'BUSINESS', label: 'Business' },
  { value: 'FIRST', label: 'First' },
]

function tomorrow(): string {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  return date.toISOString().slice(0, 10)
}

export function SearchPage() {
  const navigate = useNavigate()
  const [origin, setOrigin] = useState('')
  const [destination, setDestination] = useState('')
  const [date, setDate] = useState(tomorrow)
  const [cabin, setCabin] = useState<'' | Cabin>('')
  const [error, setError] = useState<string | null>(null)

  function submit(event: FormEvent) {
    event.preventDefault()
    const from = origin.trim().toUpperCase()
    const to = destination.trim().toUpperCase()

    if (!IATA.test(from) || !IATA.test(to)) {
      setError('Airport codes must be three letters, for example WAW.')
      return
    }
    if (from === to) {
      setError('Origin and destination must differ.')
      return
    }
    if (!date) {
      setError('Pick a departure date.')
      return
    }

    setError(null)
    const params = new URLSearchParams({ origin: from, destination: to, date })
    if (cabin) params.set('cabin', cabin)
    navigate(`/results?${params.toString()}`)
  }

  return (
    <section className="card">
      <h1>Find a flight</h1>
      <form className="search-form" onSubmit={submit}>
        <label>
          From
          <input
            value={origin}
            onChange={(event) => setOrigin(event.target.value.toUpperCase())}
            placeholder="WAW"
            maxLength={3}
            autoComplete="off"
            required
          />
        </label>
        <label>
          To
          <input
            value={destination}
            onChange={(event) => setDestination(event.target.value.toUpperCase())}
            placeholder="LIS"
            maxLength={3}
            autoComplete="off"
            required
          />
        </label>
        <label>
          Departure
          <input
            type="date"
            value={date}
            min={tomorrow()}
            onChange={(event) => setDate(event.target.value)}
            required
          />
        </label>
        <label>
          Cabin
          <select value={cabin} onChange={(event) => setCabin(event.target.value as '' | Cabin)}>
            {CABINS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <button type="submit">Search flights</button>
      </form>
      {error && <p className="error">{error}</p>}
    </section>
  )
}
