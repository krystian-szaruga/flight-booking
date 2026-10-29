import type { FormEvent } from 'react'
import { EyeIcon, EyeOffIcon } from './authShared'

export type AuthMode = 'login' | 'register'

export function SignInForm({
  mode,
  email,
  password,
  showPassword,
  notice,
  error,
  busy,
  onEmail,
  onPassword,
  onTogglePassword,
  onSubmit,
  onSwitchMode,
}: {
  mode: AuthMode
  email: string
  password: string
  showPassword: boolean
  notice: string | null
  error: string | null
  busy: boolean
  onEmail: (value: string) => void
  onPassword: (value: string) => void
  onTogglePassword: () => void
  onSubmit: (event: FormEvent) => void
  onSwitchMode: () => void
}) {
  return (
    <section className="auth-card">
      <h1>{mode === 'login' ? 'Sign in' : 'Create an account'}</h1>
      <p className="muted">
        {mode === 'login'
          ? 'Sign in to pick your seats.'
          : 'An account is needed to hold seats and keep your bookings.'}
      </p>
      <form className="auth-form" onSubmit={onSubmit}>
        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(event) => onEmail(event.target.value)}
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
              onChange={(event) => onPassword(event.target.value)}
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
              onClick={onTogglePassword}
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
      <button type="button" className="link-button" onClick={onSwitchMode}>
        {mode === 'login' ? 'No account? Sign up' : 'Already registered? Sign in'}
      </button>
    </section>
  )
}
