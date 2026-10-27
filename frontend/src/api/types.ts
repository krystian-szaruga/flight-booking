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
  date: string
  cabin?: Cabin
  zone?: string
}
