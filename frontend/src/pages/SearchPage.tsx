import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import type { AirportSummary, Cabin } from '../api/types'
import { AirportCombobox } from '../components/AirportCombobox'

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
  const [origin, setOrigin] = useState<AirportSummary | null>(null)
  const [destination, setDestination] = useState<AirportSummary | null>(null)
  const [date, setDate] = useState(tomorrow)
  const [cabin, setCabin] = useState<'' | Cabin>('')
  const [error, setError] = useState<string | null>(null)

  function submit(event: FormEvent) {
    event.preventDefault()

    if (!origin || !destination) {
      setError('Pick both airports from the suggestions.')
      return
    }
    if (origin.iataCode === destination.iataCode) {
      setError('Origin and destination must differ.')
      return
    }
    if (!date) {
      setError('Pick a departure date.')
      return
    }

    setError(null)
    const params = new URLSearchParams({
      origin: origin.iataCode,
      destination: destination.iataCode,
      date,
    })
    if (cabin) params.set('cabin', cabin)
    navigate(`/results?${params.toString()}`)
  }

  return (
    <section className="card">
      <h1>Find a flight</h1>
      <form className="search-form" onSubmit={submit}>
        <label>
          From
          <AirportCombobox value={origin} onChange={setOrigin} placeholder="Warsaw" />
        </label>
        <label>
          To
          <AirportCombobox value={destination} onChange={setDestination} placeholder="Lisbon" />
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
