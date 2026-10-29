import { vi } from 'vitest'

export interface RouteHandler {
  status?: number
  body?: unknown
  method?: string
  path: RegExp | string
  once?: boolean
}

export interface CapturedRequest {
  method: string
  url: string
  path: string
  search: URLSearchParams
  body: unknown
}

export interface Backend {
  requests: CapturedRequest[]
  callsTo(pattern: RegExp): CapturedRequest[]
  lastCall(pattern: RegExp): CapturedRequest
}

function toUrl(input: RequestInfo | URL): URL {
  if (typeof input === 'string') return new URL(input, 'http://localhost')
  if (input instanceof URL) return input
  return new URL(input.url, 'http://localhost')
}

export function startBackend(routes: RouteHandler[]): Backend {
  const requests: CapturedRequest[] = []
  const callsTo = (pattern: RegExp) =>
    requests.filter((request) => pattern.test(`${request.method} ${request.path}`))

  const backend: Backend = {
    requests,
    callsTo,
    lastCall: (pattern) => {
      const calls = callsTo(pattern)
      if (calls.length === 0) {
        const seen = requests.map((request) => `${request.method} ${request.path}`).join(', ')
        throw new Error(`No request matched ${pattern}; got ${seen}`)
      }
      return calls[calls.length - 1]
    },
  }

  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = toUrl(input)
    const method = (init?.method ?? 'GET').toUpperCase()
    const rawBody = init?.body
    requests.push({
      method,
      url: `${url.pathname}${url.search}`,
      path: url.pathname,
      search: url.searchParams,
      body: typeof rawBody === 'string' ? JSON.parse(rawBody) : undefined,
    })

    const next = routes.findIndex((candidate) => {
      const pathMatches =
        typeof candidate.path === 'string' ? candidate.path === url.pathname : candidate.path.test(url.pathname)
      return pathMatches && (!candidate.method || candidate.method === method)
    })
    if (next === -1) throw new Error(`Unstubbed request: ${method} ${url.pathname}${url.search}`)
    const [route] = routes.splice(next, 1)
    if (!route.once) routes.push(route)

    const status = route.status ?? 200
    if (status === 204) return new Response(null, { status })
    return new Response(JSON.stringify(route.body ?? null), {
      status,
      headers: { 'Content-Type': 'application/json' },
    })
  })

  vi.stubGlobal('fetch', fetchMock)
  return backend
}

export function problem(
  path: RegExp | string,
  detail: string,
  status: number,
  extra: Record<string, unknown> = {},
): RouteHandler {
  return { path, status, body: { detail, ...extra } }
}
