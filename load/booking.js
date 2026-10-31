import { check, sleep } from 'k6'
import { findFlight, holdSeat, login, pay, seatMap } from './lib.js'
import { Rate } from 'k6/metrics'

const bookings = new Rate('booking_completed')

export const options = {
  scenarios: {
    booking: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '5s', target: 2 },
        { duration: '10s', target: 2 },
        { duration: '5s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.10'],
    booking_completed: ['rate>0.80'],
  },
}

export function setup() {
  const cookie = login()
  return { cookie, flightId: findFlight(cookie) }
}

export default function (data) {
  const seats = seatMap(data.flightId, data.cookie).seats.filter((seat) => seat.status === 'AVAILABLE')
  if (seats.length === 0) {
    bookings.add(false)
    sleep(1)
    return
  }

  const seat = seats[(__VU * 97 + __ITER) % seats.length].seat
  const hold = holdSeat(data.flightId, seat, data.cookie)
  const held = check(hold, { 'seat is held': (response) => response.status === 201 })
  if (!held) {
    bookings.add(false)
    return
  }

  const reservationId = hold.json().reservationId
  const payment = pay(reservationId, `load-${__VU}-${__ITER}-${Date.now()}`, data.cookie)
  const paid = check(payment, { 'payment is approved': (response) => response.status === 201 })
  bookings.add(paid)

  sleep(1)
}
