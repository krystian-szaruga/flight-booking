import { useEffect, useState } from 'react'

function remaining(expiresAt: string): number {
  return Math.max(0, Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000))
}

export function formatRemaining(seconds: number): string {
  const minutes = Math.floor(seconds / 60)
  return `${minutes}:${String(seconds % 60).padStart(2, '0')}`
}

export function useRemainingSeconds(expiresAt: string | null): number | null {
  const [seconds, setSeconds] = useState(() => (expiresAt ? remaining(expiresAt) : null))

  useEffect(() => {
    if (!expiresAt) {
      setSeconds(null)
      return
    }
    setSeconds(remaining(expiresAt))
    const timer = window.setInterval(() => setSeconds(remaining(expiresAt)), 1000)
    return () => window.clearInterval(timer)
  }, [expiresAt])

  return seconds
}
