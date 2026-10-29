import type { Cabin, FlightSummaryResponse } from '../api/types'
import { AfterAuthLink } from './AfterAuthLink'

const formatter = new Intl.NumberFormat(undefined, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

function timeOfDay(iso: string): string {
  return new Date(iso).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
}

function duration(departureAt: string, arrivalAt: string): string {
  const minutes = Math.round(
    (new Date(arrivalAt).getTime() - new Date(departureAt).getTime()) / 60000,
  )
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest === 0 ? `${hours}h` : `${hours}h ${rest}m`
}

export function FlightCard({
  flight,
  cabinFilter,
}: {
  flight: FlightSummaryResponse
  cabinFilter?: Cabin
}) {
  const cheapest = Math.min(...flight.fares.map((fare) => fare.price))
  const seatPath = `/flights/${flight.flightId}/seats${cabinFilter ? `?cabin=${cabinFilter}` : ''}`

  return (
    <article className="flight-card">
      <header className="flight-card-header">
        <div>
          <p className="flight-number">{flight.flightNumber}</p>
          <p className="muted">
            {flight.aircraft} · {flight.status.toLowerCase()}
          </p>
        </div>
        <div className="flight-times">
          <span>{timeOfDay(flight.departureAt)}</span>
          <span className="muted">{duration(flight.departureAt, flight.arrivalAt)}</span>
          <span>{timeOfDay(flight.arrivalAt)}</span>
        </div>
      </header>

      <table className="fare-table">
        <thead>
          <tr>
            <th>Cabin</th>
            <th>Price</th>
            <th>Seats left</th>
          </tr>
        </thead>
        <tbody>
          {flight.fares.map((fare) => (
            <tr key={fare.cabin}>
              <td>{fare.cabin.replace('_', ' ').toLowerCase()}</td>
              <td className={fare.price === cheapest ? 'price cheapest' : 'price'}>
                {formatter.format(fare.price)} {fare.currency}
              </td>
              <td>{fare.seatsAvailable}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <footer className="flight-card-footer">
        <span className="muted">{flight.availableSeats} seats available in total</span>
        <AfterAuthLink to={seatPath} state={{ afterAuth: seatPath }} className="button">
          Choose a seat
        </AfterAuthLink>
      </footer>
    </article>
  )
}
