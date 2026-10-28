import { Link } from 'react-router-dom'
import type { DealResponse } from '../api/types'

const priceFormatter = new Intl.NumberFormat(undefined, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

function day(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' })
}

function timeOfDay(iso: string): string {
  return new Date(iso).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
}

export function DealCard({ deal }: { deal: DealResponse }) {
  return (
    <Link to={`/flights/${deal.flightId}/seats?cabin=${deal.cabin}`} className="deal-card">
      <div className="deal-route">
        <span className="deal-code">{deal.origin}</span>
        <span className="deal-arrow" aria-hidden="true">
          →
        </span>
        <span className="deal-code">{deal.destination}</span>
      </div>
      <p className="muted">
        {day(deal.departureAt)} · {timeOfDay(deal.departureAt)} · {deal.flightNumber}
      </p>
      <div className="deal-footer">
        <span className="deal-price">
          {priceFormatter.format(deal.price)} {deal.currency}
        </span>
        <span className="muted">{deal.cabin.replace('_', ' ').toLowerCase()}</span>
      </div>
      <span className="muted deal-seats">{deal.seatsAvailable} seats left</span>
    </Link>
  )
}
