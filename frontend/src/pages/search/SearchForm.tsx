import type { FormEvent } from 'react'
import type { AirportSummary, Cabin } from '../../api/types'
import { AirportCombobox } from '../../components/AirportCombobox'
import { CABINS, FLEXIBILITY, tomorrow } from './searchCriteria'

export function SearchForm({
  origin,
  destination,
  date,
  flexibility,
  cabin,
  range,
  error,
  onOrigin,
  onDestination,
  onDate,
  onFlexibility,
  onCabin,
  onSubmit,
}: {
  origin: AirportSummary | null
  destination: AirportSummary | null
  date: string
  flexibility: number
  cabin: '' | Cabin
  range: { from: string; to: string }
  error: string | null
  onOrigin: (airport: AirportSummary | null) => void
  onDestination: (airport: AirportSummary | null) => void
  onDate: (date: string) => void
  onFlexibility: (days: number) => void
  onCabin: (cabin: '' | Cabin) => void
  onSubmit: (event: FormEvent) => void
}) {
  return (
    <section className="card">
      <h1>Find a flight</h1>
      <form className="search-form" onSubmit={onSubmit}>
        <label>
          From
          <AirportCombobox value={origin} onChange={onOrigin} placeholder="Warsaw" />
        </label>
        <label>
          To
          <AirportCombobox value={destination} onChange={onDestination} placeholder="Lisbon" />
        </label>
        <label>
          Departure
          <input type="date" value={date} min={tomorrow()} onChange={(event) => onDate(event.target.value)} required />
        </label>
        <label>
          Flexibility
          <select value={flexibility} onChange={(event) => onFlexibility(Number(event.target.value))}>
            {FLEXIBILITY.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          Cabin
          <select value={cabin} onChange={(event) => onCabin(event.target.value as '' | Cabin)}>
            {CABINS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <button type="submit">Search flights</button>
      </form>
      <p className="muted">
        Looking at {range.from} to {range.to}
      </p>
      {error && <p className="error">{error}</p>}
    </section>
  )
}
