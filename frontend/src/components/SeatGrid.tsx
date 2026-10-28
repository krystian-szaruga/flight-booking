import type { SeatResponse } from '../api/types'

function seatsByRow(seats: SeatResponse[]): Map<number, SeatResponse[]> {
  const rows = new Map<number, SeatResponse[]>()
  for (const seat of seats) {
    const row = Number(seat.seat.replace(/[A-F]$/, ''))
    const bucket = rows.get(row) ?? []
    bucket.push(seat)
    rows.set(row, bucket)
  }
  for (const bucket of rows.values()) {
    bucket.sort((a, b) => a.seat.localeCompare(b.seat))
  }
  return new Map([...rows.entries()].sort((a, b) => a[0] - b[0]))
}

interface SeatGridProps {
  seats: SeatResponse[]
  selected?: ReadonlySet<string>
  onToggle?: (seat: SeatResponse) => void
}

export function SeatGrid({ seats, selected, onToggle }: SeatGridProps) {
  const rows = seatsByRow(seats)

  return (
    <div className="seat-grid" role="grid" aria-label="Seat map">
      {[...rows.entries()].map(([row, rowSeats]) => (
        <div className="seat-row" role="row" key={row}>
          <span className="seat-row-label">{row}</span>
          {rowSeats.map((seat) => {
            const isSelected = selected?.has(seat.seat) ?? false
            const classes = ['seat', `seat-${seat.status.toLowerCase()}`]
            if (isSelected) classes.push('seat-selected')
            return (
              <button
                key={seat.seat}
                type="button"
                role="gridcell"
                className={classes.join(' ')}
                disabled={seat.status !== 'AVAILABLE'}
                aria-pressed={isSelected}
                title={`${seat.seat} · ${seat.cabin.replace('_', ' ').toLowerCase()} · ${seat.price} ${seat.currency}`}
                aria-label={`${seat.seat}, ${isSelected ? 'selected' : seat.status.toLowerCase()}`}
                onClick={() => onToggle?.(seat)}
              >
                {seat.seat.replace(/^\d+/, '')}
              </button>
            )
          })}
        </div>
      ))}
    </div>
  )
}
