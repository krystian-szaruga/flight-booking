import type { GroundedSeat } from '../api/types'

export function HoldSeatList({
  seats,
  picked,
  disabled,
  onToggle,
}: {
  seats: GroundedSeat[]
  picked: string[]
  disabled: boolean
  onToggle: (seat: string) => void
}) {
  return (
    <ul className="hold-seat-picker">
      {seats.map((seat) => (
        <li key={seat.seat}>
          <label>
            <input
              type="checkbox"
              checked={picked.includes(seat.seat)}
              disabled={disabled}
              onChange={() => onToggle(seat.seat)}
            />
            <span>
              <strong>{seat.seat}</strong> · {seat.firstName} {seat.lastName}
            </span>
          </label>
        </li>
      ))}
    </ul>
  )
}
