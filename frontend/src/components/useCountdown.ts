import { useEffect, useState } from 'react'

export function useCountdown(initialSeconds: number): number {
  const [seconds, setSeconds] = useState(initialSeconds)

  useEffect(() => {
    setSeconds(initialSeconds)
    if (initialSeconds <= 0) return
    const deadline = Date.now() + initialSeconds * 1000
    const timer = window.setInterval(() => {
      const left = Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
      setSeconds(left)
      if (left === 0) window.clearInterval(timer)
    }, 1000)
    return () => window.clearInterval(timer)
  }, [initialSeconds])

  return seconds
}
