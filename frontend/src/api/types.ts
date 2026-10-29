export type Cabin = 'ECONOMY' | 'PREMIUM_ECONOMY' | 'BUSINESS' | 'FIRST'

export type SeatStatus = 'AVAILABLE' | 'HELD' | 'SOLD'

export interface FareResponse {
  cabin: Cabin
  price: number
  currency: string
  seatsAvailable: number
}

export interface FlightSummaryResponse {
  flightId: string
  flightNumber: string
  origin: string
  destination: string
  departureAt: string
  arrivalAt: string
  aircraft: string
  status: string
  availableSeats: number
  fares: FareResponse[]
}

export interface SeatResponse {
  seat: string
  cabin: Cabin
  price: number
  currency: string
  status: SeatStatus
}

export interface SeatMapResponse {
  flightId: string
  flightNumber: string
  origin: string
  destination: string
  departureAt: string
  aircraft: string
  status: string
  seatRows: number
  fares: FareResponse[]
  seats: SeatResponse[]
}

export interface SearchFlightsParams {
  origin: string
  destination: string
  from: string
  to?: string
  cabin?: Cabin
  zone?: string
}

export interface AirportSummary {
  iataCode: string
  name: string
  city: string
  countryCode: string
}

export type PassengerType = 'SELF' | 'OTHER'

export interface HoldSeat {
  seat: string
  passengerType: PassengerType
  firstName?: string
  lastName?: string
}

export interface GroundedSeat {
  seat: string
  firstName: string
  lastName: string
}

export interface ReservationResponse {
  reservationId: string
  flightId: string
  status: 'PENDING' | 'CONFIRMED' | 'EXPIRED' | 'CANCELLED'
  expiresAt: string | null
  seats: GroundedSeat[]
}

export interface UserResponse {
  id: string
  email: string
  createdAt: string
  emailVerifiedAt: string | null
  firstName: string | null
  lastName: string | null
}

export interface DealResponse {
  flightId: string
  flightNumber: string
  origin: string
  destination: string
  departureAt: string
  arrivalAt: string
  cabin: Cabin
  price: number
  currency: string
  seatsAvailable: number
}
