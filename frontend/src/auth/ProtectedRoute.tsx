import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) return <p className="muted">Checking your session…</p>
  const target = `${location.pathname}${location.search}`
  if (!user) {
    return <Navigate to={`/auth?redirect=${encodeURIComponent(target)}`} replace />
  }
  if (!user.emailVerifiedAt) {
    return <Navigate to={`/auth?redirect=${encodeURIComponent(target)}`} replace />
  }
  return <>{children}</>
}
