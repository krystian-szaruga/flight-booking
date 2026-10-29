import type {
  AirportSummary,
  DealResponse,
  FlightSummaryResponse,
  GroundedSeat,
  ReservationResponse,
  SearchFlightsParams,
  SeatMapResponse,
} from './types'

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function request<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(path, {
    signal,
    credentials: 'include',
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) {
    throw new ApiError(response.status, await problemDetail(response))
  }
  return (await response.json()) as T
}

async function send<T>(
  method: 'POST' | 'DELETE',
  path: string,
  body?: unknown,
  signal?: AbortSignal,
): Promise<T> {
  const response = await fetch(path, {
    method,
    signal,
    credentials: 'include',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) {
    throw new ApiError(response.status, await problemDetail(response))
  }
  return (await response.json()) as T
}

async function problemDetail(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { detail?: string; title?: string }
    return body.detail ?? body.title ?? `Request failed with status ${response.status}`
  } catch {
    return `Request failed with status ${response.status}`
  }
}

export function searchFlights(
  params: SearchFlightsParams,
  signal?: AbortSignal,
): Promise<FlightSummaryResponse[]> {
  const query = new URLSearchParams({
    origin: params.origin,
    destination: params.destination,
    date: params.date,
  })
  if (params.cabin) query.set('cabin', params.cabin)
  if (params.zone) query.set('zone', params.zone)
  return request<FlightSummaryResponse[]>(`/api/v1/flights?${query.toString()}`, signal)
}

export function getSeatMap(flightId: string, signal?: AbortSignal): Promise<SeatMapResponse> {
  return request<SeatMapResponse>(`/api/v1/flights/${encodeURIComponent(flightId)}/seats`, signal)
}

export function getDeals(limit = 6, signal?: AbortSignal): Promise<DealResponse[]> {
  return request<DealResponse[]>(`/api/v1/flights/deals?limit=${limit}`, signal)
}

export function searchAirports(query: string, signal?: AbortSignal): Promise<AirportSummary[]> {
  return request<AirportSummary[]>(`/api/v1/airports?q=${encodeURIComponent(query)}`, signal)
}

export function holdSeats(
  flightId: string,
  seats: GroundedSeat[],
  signal?: AbortSignal,
): Promise<ReservationResponse> {
  return send<ReservationResponse>(
    'POST',
    `/api/v1/reservations/flights/${encodeURIComponent(flightId)}`,
    { seats },
    signal,
  )
}

export function cancelReservation(reservationId: string, signal?: AbortSignal): Promise<ReservationResponse> {
  return send<ReservationResponse>(
    'DELETE',
    `/api/v1/reservations/${encodeURIComponent(reservationId)}`,
    undefined,
    signal,
  )
}

export function getMyReservations(signal?: AbortSignal): Promise<ReservationResponse[]> {
  return request<ReservationResponse[]>('/api/v1/reservations', signal)
}
