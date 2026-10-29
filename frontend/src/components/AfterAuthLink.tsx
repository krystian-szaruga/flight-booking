import type { ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

interface AfterAuth {
  afterAuth?: string
}

export function useAuthRedirect(): string | null {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) return null
  if (user?.emailVerifiedAt) return null
  if (!(location.state as AfterAuth | null)?.afterAuth) return null

  const target = `${location.pathname}${location.search}`
  return `/auth?redirect=${encodeURIComponent(target)}`
}

export function AfterAuthLink({
  to,
  state,
  className,
  children,
}: {
  to: string
  state?: AfterAuth
  className?: string
  children: ReactNode
}) {
  const redirect = useAuthRedirect()
  return (
    <Link to={redirect ?? to} state={state} className={className}>
      {children}
    </Link>
  )
}

export function SignInToContinue({ target }: { target: string }) {
  return (
    <section className="hold-panel">
      <h2>Sign in to pick a seat</h2>
      <p className="muted">After signing in you land right back on this flight, ready to select seats.</p>
      <Link to={`/auth?redirect=${encodeURIComponent(target)}`} className="button">
        Sign in and continue
      </Link>
    </section>
  )
}
