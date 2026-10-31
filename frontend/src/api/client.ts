import type {
  AirportSummary,
  CurrencyOptionsResponse,
  DealResponse,
  FlightSummaryResponse,
  HoldSeat,
  PayReservationRequest,
  PaymentResponse,
  ReservationResponse,
  SearchFlightsParams,
  SeatMapResponse,
} from './types'

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly stage: string | null = null,
    readonly code: string | null = null,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function problemDetail(
  response: Response,
): Promise<{ message: string; stage: string | null; code: string | null }> {
  try {
    const body = (await response.json()) as { detail?: string; title?: string; stage?: string; code?: string }
    return {
      message: body.detail ?? body.title ?? `Request failed with status ${response.status}`,
      stage: body.stage ?? null,
      code: body.code ?? null,
    }
  } catch {
    return { message: `Request failed with status ${response.status}`, stage: null, code: null }
  }
}

export async function fail(response: Response): Promise<never> {
  const problem = await problemDetail(response)
  throw new ApiError(response.status, problem.message, problem.stage, problem.code)
}

async function request<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await fetchWithRefresh(path, {
    signal,
    credentials: 'include',
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) await fail(response)
  return (await response.json()) as T
}

async function refreshSession(): Promise<boolean> {
  try {
    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
      headers: { Accept: 'application/json' },
    })
    return response.ok
  } catch {
    return false
  }
}

export async function fetchWithRefresh(path: string, init: RequestInit): Promise<Response> {
  const response = await fetch(path, init)
  if (response.status !== 401 || path.startsWith('/api/auth/')) return response
  if (!(await refreshSession())) return response
  return fetch(path, init)
}

async function send<T>(
  method: 'POST' | 'PUT' | 'DELETE',
  path: string,
  body?: unknown,
  signal?: AbortSignal,
): Promise<T> {
  const response = await fetchWithRefresh(path, {
    method,
    signal,
    credentials: 'include',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) await fail(response)
  return (await response.json()) as T
}

function withCurrency(path: string, currency?: string): string {
  if (!currency) return path
  return `${path}${path.includes('?') ? '&' : '?'}currency=${encodeURIComponent(currency)}`
}

export function getCurrencyOptions(signal?: AbortSignal): Promise<CurrencyOptionsResponse> {
  return request<CurrencyOptionsResponse>('/api/v1/currencies', signal)
}

export function searchFlights(
  params: SearchFlightsParams,
  signal?: AbortSignal,
): Promise<FlightSummaryResponse[]> {
  const query = new URLSearchParams({
    origin: params.origin,
    destination: params.destination,
    from: params.from,
  })
  if (params.to && params.to !== params.from) query.set('to', params.to)
  if (params.cabin) query.set('cabin', params.cabin)
  if (params.zone) query.set('zone', params.zone)
  if (params.currency) query.set('currency', params.currency)
  return request<FlightSummaryResponse[]>(`/api/v1/flights?${query.toString()}`, signal)
}

export function getSeatMap(
  flightId: string,
  currency?: string,
  signal?: AbortSignal,
): Promise<SeatMapResponse> {
  return request<SeatMapResponse>(
    withCurrency(`/api/v1/flights/${encodeURIComponent(flightId)}/seats`, currency),
    signal,
  )
}

export function getDeals(
  limit = 6,
  currency?: string,
  signal?: AbortSignal,
): Promise<DealResponse[]> {
  return request<DealResponse[]>(withCurrency(`/api/v1/flights/deals?limit=${limit}`, currency), signal)
}

export function searchAirports(query: string, signal?: AbortSignal): Promise<AirportSummary[]> {
  return request<AirportSummary[]>(`/api/v1/airports?q=${encodeURIComponent(query)}`, signal)
}

export function holdSeats(
  flightId: string,
  seats: HoldSeat[],
  currency?: string,
  signal?: AbortSignal,
): Promise<ReservationResponse> {
  return send<ReservationResponse>(
    'POST',
    withCurrency(`/api/v1/reservations/flights/${encodeURIComponent(flightId)}`, currency),
    { seats },
    signal,
  )
}

export function updateReservationPassengers(
  reservationId: string,
  seats: HoldSeat[],
  currency?: string,
  signal?: AbortSignal,
): Promise<ReservationResponse> {
  return send<ReservationResponse>(
    'PUT',
    withCurrency(`/api/v1/reservations/${encodeURIComponent(reservationId)}/passengers`, currency),
    { seats },
    signal,
  )
}

export function cancelReservation(
  reservationId: string,
  currency?: string,
  signal?: AbortSignal,
): Promise<ReservationResponse> {
  return send<ReservationResponse>(
    'DELETE',
    withCurrency(`/api/v1/reservations/${encodeURIComponent(reservationId)}`, currency),
    undefined,
    signal,
  )
}

export function cancelReservationSeats(
  reservationId: string,
  seats: string[],
  currency?: string,
  signal?: AbortSignal,
): Promise<ReservationResponse> {
  return send<ReservationResponse>(
    'DELETE',
    withCurrency(`/api/v1/reservations/${encodeURIComponent(reservationId)}/seats`, currency),
    { seats },
    signal,
  )
}

export function getMyReservations(
  currency?: string,
  signal?: AbortSignal,
): Promise<ReservationResponse[]> {
  return request<ReservationResponse[]>(withCurrency('/api/v1/reservations', currency), signal)
}

export async function payReservation(
  reservationId: string,
  payment: PayReservationRequest,
): Promise<PaymentResponse> {
  const response = await fetchWithRefresh(`/api/v1/reservations/${encodeURIComponent(reservationId)}/payment`, {
    method: 'POST',
    credentials: 'include',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify(payment),
  })
  if (response.status === 402) return (await response.json()) as PaymentResponse
  if (!response.ok) await fail(response)
  return (await response.json()) as PaymentResponse
}
