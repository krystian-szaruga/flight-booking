import { check, sleep } from 'k6'
import { Counter } from 'k6/metrics'
import { findFlight, holdSeat, login, seatMap } from './lib.js'

const holdsAccepted = new Counter('holds_accepted')
const holdsRejected = new Counter('holds_rejected')

export const options = {
  scenarios: {
    contention: {
      executor: 'per-vu-iterations',
      vus: 50,
      iterations: 1,
      maxDuration: '30s',
    },
  },
  thresholds: {
    holds_accepted: ['count==1'],
    holds_rejected: ['count==49'],
  },
}

export function setup() {
  const cookie = login()
  const flightId = findFlight(cookie)
  const seat = seatMap(flightId, cookie).seats.find((candidate) => candidate.status === 'AVAILABLE')
  if (!seat) throw new Error('no available seat to contend for')
  return { cookie, flightId, seat: seat.seat, startAt: Date.now() + 5000 }
}

export default function (data) {
  const wait = data.startAt - Date.now()
  if (wait > 0) sleep(wait / 1000)

  const response = holdSeat(data.flightId, data.seat, data.cookie)
  const accepted = response.status === 201
  check(response, { 'hold is accepted or conflicts': () => accepted || response.status === 409 })
  if (accepted) holdsAccepted.add(1)
  if (response.status === 409) holdsRejected.add(1)
}
