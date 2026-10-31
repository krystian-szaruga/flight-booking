import http from 'k6/http'
import { check, sleep } from 'k6'
import { BASE_URL, departureDate, DESTINATION, ORIGIN } from './lib.js'

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
}

export default function () {
  const health = http.get(`${BASE_URL}/actuator/health`)
  check(health, { 'health is up': (response) => response.status === 200 })

  const search = http.get(
    `${BASE_URL}/api/v1/flights?origin=${ORIGIN}&destination=${DESTINATION}&from=${departureDate()}`,
  )
  check(search, {
    'search answers 200': (response) => response.status === 200,
  })

  sleep(1)
}
