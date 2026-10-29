import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import type { RegistrationResponse } from '../api/auth'
import { ApiError } from '../api/client'
import type { UserResponse } from '../api/types'

export interface AuthValue {
  user: UserResponse | null
  loading: boolean
  signIn: (email: string, password: string) => Promise<void>
  signUp: (email: string, password: string) => Promise<RegistrationResponse>
  verify: (email: string, code: string) => Promise<void>
  resend: (email: string) => Promise<number>
  signOut: () => Promise<void>
  refresh: () => Promise<UserResponse | null>
}

const AuthContext = createContext<AuthValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    try {
      const current = await authApi.fetchCurrentUser()
      setUser(current)
      return current
    } catch (cause) {
      if (cause instanceof ApiError && cause.status !== 401) throw cause
      setUser(null)
      return null
    }
  }, [])

  useEffect(() => {
    let active = true
    refresh()
      .catch(() => {
        if (active) setUser(null)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [refresh])

  const signIn = useCallback(async (email: string, password: string) => {
    setUser(await authApi.login(email, password))
  }, [])

  const signUp = useCallback(async (email: string, password: string) => authApi.register(email, password), [])

  const verify = useCallback(async (email: string, code: string) => {
    setUser(await authApi.verifyEmail(email, code))
  }, [])

  const resend = useCallback(async (email: string) => {
    const sent = await authApi.resendVerification(email)
    return sent?.retryAfterSeconds ?? 60
  }, [])

  const signOut = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      setUser(null)
    }
  }, [])

  const value = useMemo<AuthValue>(
    () => ({ user, loading, signIn, signUp, verify, resend, signOut, refresh }),
    [user, loading, signIn, signUp, verify, resend, signOut, refresh],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used inside AuthProvider')
  return value
}
