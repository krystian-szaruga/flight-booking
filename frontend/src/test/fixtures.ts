import type {
  AirportSummary,
  FlightSummaryResponse,
  ReservationResponse,
  SeatMapResponse,
  SeatResponse,
  UserResponse,
} from '../api/types'

export const WARSAW: AirportSummary = {
  iataCode: 'WAW',
  name: 'Warsaw Chopin',
  city: 'Warsaw',
  countryCode: 'PL',
}

export const LISBON: AirportSummary = {
  iataCode: 'LIS',
  name: 'Lisbon Humberto Delgado',
  city: 'Lisbon',
  countryCode: 'PT',
}

export const VERIFIED_USER: UserResponse = {
  id: 'user-1',
  email: 'ada@example.com',
  createdAt: '2026-01-01T10:00:00Z',
  emailVerifiedAt: '2026-01-01T10:05:00Z',
  firstName: 'Ada',
  lastName: 'Lovelace',
}

export function flight(overrides: Partial<FlightSummaryResponse> = {}): FlightSummaryResponse {
  return {
    flightId: 'LO26',
    flightNumber: 'LO 26',
    origin: 'WAW',
    destination: 'LIS',
    departureAt: '2026-11-10T08:00:00Z',
    arrivalAt: '2026-11-10T12:30:00Z',
    aircraft: 'Boeing 737-800',
    status: 'SCHEDULED',
    availableSeats: 120,
    fares: [
      { cabin: 'ECONOMY', price: 120, currency: 'EUR', seatsAvailable: 90 },
      { cabin: 'BUSINESS', price: 480, currency: 'EUR', seatsAvailable: 12 },
    ],
    ...overrides,
  }
}

export function seat(seatCode: string, overrides: Partial<SeatResponse> = {}): SeatResponse {
  const cabin = Number(seatCode.replace(/\D/g, '')) <= 2 ? 'BUSINESS' : 'ECONOMY'
  return {
    seat: seatCode,
    cabin,
    price: cabin === 'BUSINESS' ? 480 : 120,
    currency: 'EUR',
    status: 'AVAILABLE',
    ...overrides,
  }
}

export function seatMap(overrides: Partial<SeatMapResponse> = {}): SeatMapResponse {
  return {
    flightId: 'LO26',
    flightNumber: 'LO 26',
    origin: 'WAW',
    destination: 'LIS',
    departureAt: '2026-11-10T08:00:00Z',
    aircraft: 'Boeing 737-800',
    status: 'SCHEDULED',
    seatRows: 3,
    fares: [
      { cabin: 'BUSINESS', price: 480, currency: 'EUR', seatsAvailable: 3 },
      { cabin: 'ECONOMY', price: 120, currency: 'EUR', seatsAvailable: 6 },
    ],
    seats: ['1A', '1B', '1C', '3A', '3B', '3C'].map((code) => seat(code)),
    ...overrides,
  }
}

export function reservation(overrides: Partial<ReservationResponse> = {}): ReservationResponse {
  return {
    reservationId: 'res-1',
    flightId: 'LO26',
    status: 'PENDING',
    expiresAt: new Date(Date.now() + 20 * 60 * 1000).toISOString(),
    seats: [{ seat: '3A', firstName: 'Grace', lastName: 'Hopper' }],
    ...overrides,
  }
}
