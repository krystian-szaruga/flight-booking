import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { formatRemaining } from '../components/useRemainingSeconds'
import { useCountdown } from '../components/useCountdown'

type Mode = 'login' | 'register'

export function AuthPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { user, signIn, signUp, verify, resend, refresh, signOut } = useAuth()

  const [mode, setMode] = useState<Mode>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [code, setCode] = useState('')
  const [pendingEmail, setPendingEmail] = useState<string | null>(null)
  const [cooldownSeconds, setCooldownSeconds] = useState(0)
  const [notice, setNotice] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const remaining = useCountdown(cooldownSeconds)
  const awaitingCode = pendingEmail ?? (user && !user.emailVerifiedAt ? user.email : null)

  useEffect(() => {
    if (user?.emailVerifiedAt) navigate(params.get('redirect') ?? '/', { replace: true })
  }, [user, navigate, params])

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (awaitingCode) {
        await verify(awaitingCode, code)
        navigate(params.get('redirect') ?? '/', { replace: true })
      } else if (mode === 'login') {
        await signIn(email, password)
        navigate(params.get('redirect') ?? '/', { replace: true })
      } else {
        const started = await signUp(email, password)
        setPendingEmail(email.trim().toLowerCase())
        setPassword('')
        setShowPassword(false)
        setCooldownSeconds(started.retryAfterSeconds)
        setNotice(
          started.stage === 'EMAIL_VERIFIED'
            ? 'This address already has an account. Sign in with your password instead.'
            : `We sent a 6-digit code to ${email.trim().toLowerCase()}.`,
        )
      }
    } catch (cause) {
      if (!(await handleVerifyConflict(cause))) {
        setError(cause instanceof ApiError ? describe(cause) : 'Something went wrong. Try again.')
      }
    } finally {
      setBusy(false)
    }
  }

  const handleVerifyConflict = async (cause: unknown): Promise<boolean> => {
    if (!(cause instanceof ApiError) || cause.stage !== 'EMAIL_VERIFIED') return false
    setPendingEmail(null)
    setCode('')
    setCooldownSeconds(0)
    setMode('login')
    setNotice('This address is already verified. Sign in with your password.')
    await refresh()
    return true
  }

  const onResend = async () => {
    if (!awaitingCode || remaining > 0) return
    setBusy(true)
    setError(null)
    try {
      const seconds = await resend(awaitingCode)
      setCooldownSeconds(seconds)
      setNotice('A new code is on its way.')
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 429) {
        setCooldownSeconds(15)
        setError('Too many requests. Try again in a moment.')
      } else {
        setError('Could not resend the code.')
      }
    } finally {
      setBusy(false)
    }
  }

  const backToLogin = async () => {
    setPendingEmail(null)
    setNotice(null)
    setError(null)
    setCode('')
    setPassword('')
    setShowPassword(false)
    setCooldownSeconds(0)
    setMode('login')
    if (user) await signOut()
  }

  const switchMode = () => {
    setError(null)
    setNotice(null)
    setPassword('')
    setShowPassword(false)
    setMode(mode === 'login' ? 'register' : 'login')
  }

  if (awaitingCode) {
    const canResend = remaining === 0 && !busy
    return (
      <section className="auth-card">
        <h1>Enter the code from your email</h1>
        <p className="muted">We sent a 6-digit code to {awaitingCode}.</p>
        <form className="auth-form" onSubmit={submit}>
          <label>
            Verification code
            <input
              type="text"
              inputMode="numeric"
              pattern="\d{6}"
              maxLength={6}
              value={code}
              onChange={(event) => setCode(event.target.value.replace(/\D/g, ''))}
              required
              autoComplete="one-time-code"
            />
          </label>
          {notice && <p className="muted">{notice}</p>}
          {error && <p className="error">{error}</p>}
          <button type="submit" disabled={busy || code.length !== 6}>
            {busy ? 'Verifying…' : 'Verify email'}
          </button>
        </form>
        <button
          type="button"
          className="link-button"
          onClick={onResend}
          disabled={!canResend}
          aria-live="polite"
        >
          {remaining > 0 ? `Send a new code in ${formatRemaining(remaining)}` : 'Send a new code'}
        </button>
        <button type="button" className="link-button" onClick={backToLogin} disabled={busy}>
          Back to sign in
        </button>
      </section>
    )
  }

  return (
    <section className="auth-card">
      <h1>{mode === 'login' ? 'Sign in' : 'Create an account'}</h1>
      <p className="muted">
        {mode === 'login'
          ? 'Sign in to pick your seats.'
          : 'An account is needed to hold seats and keep your bookings.'}
      </p>
      <form className="auth-form" onSubmit={submit}>
        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
            autoComplete="email"
          />
        </label>
        <label>
          Password
          <span className="password-field">
            <input
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              minLength={8}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            />
            <button
              type="button"
              className="password-toggle"
              aria-pressed={showPassword}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              title={showPassword ? 'Hide password' : 'Show password'}
              onClick={() => setShowPassword((current) => !current)}
            >
              {showPassword ? <EyeOffIcon /> : <EyeIcon />}
            </button>
          </span>
        </label>
        {notice && <p className="muted">{notice}</p>}
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={busy}>
          {busy ? 'Working…' : mode === 'login' ? 'Sign in' : 'Sign up'}
        </button>
      </form>
      <button type="button" className="link-button" onClick={switchMode}>
        {mode === 'login' ? 'No account? Sign up' : 'Already registered? Sign in'}
      </button>
    </section>
  )
}

function EyeIcon() {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false">
      <path
        d="M12 5c-5 0-9 4.5-10 7 1 2.5 5 7 10 7s9-4.5 10-7c-1-2.5-5-7-10-7z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
      <circle cx="12" cy="12" r="3" fill="none" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  )
}

function EyeOffIcon() {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false">
      <path
        d="M3 3l18 18"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
      <path
        d="M10.6 6.2A10.9 10.9 0 0 1 12 6c5 0 9 4.5 10 7a16 16 0 0 1-3.6 4.2M6.4 7.6A16.4 16.4 0 0 0 2 13c1 2.5 5 7 10 7a10.6 10.6 0 0 0 4.4-1"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function describe(error: ApiError): string {
  if (error.message === 'invalid_or_expired_code') return 'That code is invalid or has expired.'
  if (error.status === 429) return 'Too many attempts. Wait a moment and try again.'
  return error.message
}
