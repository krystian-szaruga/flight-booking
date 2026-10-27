import type { FlightSummaryResponse, SearchFlightsParams, SeatMapResponse } from './types'

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
  const response = await fetch(path, { signal, headers: { Accept: 'application/json' } })
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
