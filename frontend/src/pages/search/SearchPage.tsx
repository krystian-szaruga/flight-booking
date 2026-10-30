import { useEffect, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ApiError, searchAirports, searchFlights } from '../../api/client'
import type { AirportSummary, Cabin, FlightSummaryResponse } from '../../api/types'
import { useCurrency } from '../../currency/CurrencyContext'
import { flexibilityOf, tomorrow, withRange } from './searchCriteria'
import { SearchForm } from './SearchForm'
import { SearchResults } from './SearchResults'

function useAirportByCode(code: string): [AirportSummary | null, (airport: AirportSummary | null) => void] {
  const [airport, setAirport] = useState<AirportSummary | null>(null)

  useEffect(() => {
    if (!code || airport?.iataCode === code) return
    let active = true
    searchAirports(code)
      .then((found) => {
        const match = found.find((item) => item.iataCode === code)
        if (active && match) setAirport(match)
      })
      .catch(() => undefined)
    return () => {
      active = false
    }
  }, [code, airport])

  return [airport, setAirport]
}

export function SearchPage() {
  const [params, setParams] = useSearchParams()
  const { currency } = useCurrency()

  const originCode = params.get('origin') ?? ''
  const destinationCode = params.get('destination') ?? ''
  const from = params.get('from') ?? params.get('date') ?? ''
  const to = params.get('to') || from
  const cabin = (params.get('cabin') ?? '') as '' | Cabin
  const searched = Boolean(originCode && destinationCode && from)

  const [origin, setOrigin] = useAirportByCode(originCode)
  const [destination, setDestination] = useAirportByCode(destinationCode)
  const [date, setDate] = useState(from || tomorrow())
  const [flexibility, setFlexibility] = useState(() => flexibilityOf(from, to))
  const [cabinChoice, setCabinChoice] = useState<'' | Cabin>(cabin)
  const [error, setError] = useState<string | null>(null)

  const [flights, setFlights] = useState<FlightSummaryResponse[] | null>(null)
  const [flightsError, setFlightsError] = useState<string | null>(null)

  useEffect(() => {
    if (!searched) {
      setFlights(null)
      setFlightsError(null)
      return
    }

    const controller = new AbortController()
    setFlights(null)
    setFlightsError(null)

    searchFlights(
      { origin: originCode, destination: destinationCode, from, to, cabin: cabin || undefined, currency },
      controller.signal,
    )
      .then((results) => {
        if (!controller.signal.aborted) setFlights(results)
      })
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setFlightsError(cause instanceof ApiError ? cause.message : 'Could not load flights.')
      })

    return () => controller.abort()
  }, [searched, originCode, destinationCode, from, to, cabin, currency])

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
    const range = withRange(date, flexibility)
    const next = new URLSearchParams({
      origin: origin.iataCode,
      destination: destination.iataCode,
      from: range.from,
      to: range.to,
    })
    if (cabinChoice) next.set('cabin', cabinChoice)
    setParams(next)
  }

  return (
    <>
      <SearchForm
        origin={origin}
        destination={destination}
        date={date}
        flexibility={flexibility}
        cabin={cabinChoice}
        range={withRange(date, flexibility)}
        error={error}
        onOrigin={setOrigin}
        onDestination={setDestination}
        onDate={setDate}
        onFlexibility={setFlexibility}
        onCabin={setCabinChoice}
        onSubmit={submit}
      />

      {searched && (
        <SearchResults
          origin={originCode}
          destination={destinationCode}
          from={from}
          to={to}
          cabin={cabin}
          flights={flights}
          error={flightsError}
        />
      )}
    </>
  )
}
