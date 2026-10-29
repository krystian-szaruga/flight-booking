import type { Cabin, SeatResponse } from '../api/types'

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

function cabinLabel(cabin: Cabin): string {
  return cabin.replace('_', ' ').toLowerCase()
}

interface SeatGridProps {
  seats: SeatResponse[]
  selected?: ReadonlySet<string>
  onToggle?: (seat: SeatResponse) => void
  allowedCabin?: Cabin
}

export function SeatGrid({ seats, selected, onToggle, allowedCabin }: SeatGridProps) {
  const rows = seatsByRow(seats)
  const cabins: Cabin[] = []
  for (const seat of seats) {
    if (!cabins.includes(seat.cabin)) cabins.push(seat.cabin)
  }

  return (
    <div className="seat-deck">
      {cabins.map((cabin) => {
        const cabinRows = [...rows.entries()]
          .map(([row, rowSeats]) => [row, rowSeats.filter((seat) => seat.cabin === cabin)] as const)
          .filter(([, rowSeats]) => rowSeats.length > 0)
        const blocked = allowedCabin !== undefined && allowedCabin !== cabin
        return (
          <div
            key={cabin}
            className={`seat-cabin ${blocked ? 'seat-cabin-blocked' : ''}`}
            aria-label={`${cabinLabel(cabin)} cabin`}
          >
            <p className="seat-cabin-label">{cabinLabel(cabin)}</p>
            <div className="seat-grid" role="grid" aria-label={`${cabinLabel(cabin)} seats`}>
              {cabinRows.map(([row, rowSeats]) => (
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
                        disabled={seat.status !== 'AVAILABLE' || blocked}
                        aria-pressed={isSelected}
                        aria-label={`${seat.seat}, ${cabinLabel(seat.cabin)}, ${
                          isSelected ? 'selected' : seat.status.toLowerCase()
                        }`}
                        title={`${seat.seat} · ${cabinLabel(seat.cabin)} · ${seat.price} ${seat.currency}`}
                        onClick={() => onToggle?.(seat)}
                      >
                        {seat.seat.replace(/^\d+/, '')}
                      </button>
                    )
                  })}
                </div>
              ))}
            </div>
          </div>
        )
      })}
    </div>
  )
}
