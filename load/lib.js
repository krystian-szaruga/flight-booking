import http from 'k6/http'

export const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '')
export const EMAIL = __ENV.LOAD_EMAIL || 'load@example.com'
export const PASSWORD = __ENV.LOAD_PASSWORD || 'Password123!'
export const ORIGIN = __ENV.ORIGIN || 'WAW'
export const DESTINATION = __ENV.DESTINATION || 'LIS'
export const CURRENCY = __ENV.CURRENCY || 'PLN'

export function departureDate() {
  if (__ENV.FROM) return __ENV.FROM
  const date = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)
  return date.toISOString().slice(0, 10)
}

export function login() {
  const response = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  )
  if (response.status !== 200) {
    throw new Error(`login failed with ${response.status}: ${response.body}`)
  }
  const cookie = response.cookies.access_token?.[0]?.value
  if (!cookie) throw new Error('login returned no access_token cookie')
  return `access_token=${cookie}`
}

export function findFlight(cookie) {
  const direct = searchFlights(cookie, ORIGIN, DESTINATION, departureDate())
  if (direct.length > 0) return direct[0].flightId

  const deals = http.get(`${BASE_URL}/api/v1/flights/deals?limit=20`, {
    headers: { Accept: 'application/json', Cookie: cookie },
  })
  if (deals.status !== 200) {
    throw new Error(`deals failed with ${deals.status}: ${deals.body}`)
  }
  for (const deal of deals.json()) {
    const flights = searchFlights(cookie, deal.origin, deal.destination, deal.departureAt.slice(0, 10))
    const match = flights.find((flight) => flight.flightId === deal.flightId)
    if (match) return match.flightId
  }
  throw new Error(`no bookable flight found for ${ORIGIN} -> ${DESTINATION} or in the deals feed`)
}

function searchFlights(cookie, origin, destination, from) {
  const response = http.get(
    `${BASE_URL}/api/v1/flights?origin=${origin}&destination=${destination}&from=${from}`,
    { headers: { Accept: 'application/json', Cookie: cookie } },
  )
  if (response.status !== 200) {
    throw new Error(`flight search failed with ${response.status}: ${response.body}`)
  }
  return response.json()
}

export function seatMap(flightId, cookie) {
  const response = http.get(
    `${BASE_URL}/api/v1/flights/${flightId}/seats?currency=${CURRENCY}`,
    { headers: { Accept: 'application/json', Cookie: cookie } },
  )
  if (response.status !== 200) {
    throw new Error(`seat map failed with ${response.status}: ${response.body}`)
  }
  return response.json()
}

export function holdSeat(flightId, seat, cookie) {
  return http.post(
    `${BASE_URL}/api/v1/reservations/flights/${flightId}?currency=${CURRENCY}`,
    JSON.stringify({
      seats: [
        {
          seat,
          passengerType: 'OTHER',
          firstName: 'Load',
          lastName: 'Tester',
          passportNumber: 'AB1234567',
          phoneNumber: '+48600123456',
          dateOfBirth: '1990-01-01',
        },
      ],
    }),
    {
      headers: { Accept: 'application/json', 'Content-Type': 'application/json', Cookie: cookie },
      tags: { name: 'hold-seat' },
    },
  )
}

export function pay(reservationId, idempotencyKey, cookie) {
  return http.post(
    `${BASE_URL}/api/v1/reservations/${reservationId}/payment`,
    JSON.stringify({
      idempotencyKey,
      cardNumber: '4242424242424242',
      cardHolder: 'Load Tester',
    }),
    {
      headers: { Accept: 'application/json', 'Content-Type': 'application/json', Cookie: cookie },
      tags: { name: 'pay' },
    },
  )
}
