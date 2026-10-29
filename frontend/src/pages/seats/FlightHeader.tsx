import type { SeatMapResponse } from '../../api/types'

const formatter = new Intl.NumberFormat(undefined, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

export function FlightHeader({ seatMap }: { seatMap: SeatMapResponse }) {
  return (
    <div className="results-header">
      <h1>
        {seatMap.flightNumber} · {seatMap.origin} to {seatMap.destination}
      </h1>
      <p className="muted">
        {new Date(seatMap.departureAt).toLocaleString()} · {seatMap.aircraft} · {seatMap.seatRows} rows
      </p>
    </div>
  )
}

export function FareSummary({ fares }: { fares: SeatMapResponse['fares'] }) {
  return (
    <ul className="fare-summary">
      {fares.map((fare) => (
        <li key={fare.cabin}>
          <strong>{fare.cabin.replace('_', ' ').toLowerCase()}</strong>{' '}
          <span className="muted">
            from {formatter.format(fare.price)} {fare.currency}
          </span>
        </li>
      ))}
    </ul>
  )
}

export function SeatLegend() {
  return (
    <div className="legend">
      <span className="seat seat-available legend-swatch" aria-hidden="true" /> available
      <span className="seat seat-held legend-swatch" aria-hidden="true" /> on hold
      <span className="seat seat-sold legend-swatch" aria-hidden="true" /> sold
      <span className="seat seat-selected legend-swatch" aria-hidden="true" /> selected
    </div>
  )
}
