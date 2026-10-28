import { useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from './AuthContext'

export function useRequireAuth(): void {
  const { user, loading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    if (user || loading) return
    const target = `${location.pathname}${location.search}`
    navigate(`/auth?redirect=${encodeURIComponent(target)}`, { replace: true })
  }, [user, loading, navigate, location])
}

export function isUnauthorized(cause: unknown): boolean {
  return cause instanceof ApiError && cause.status === 401
}
