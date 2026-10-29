import type { FormEvent } from 'react'
import { formatRemaining } from '../../components/useRemainingSeconds'

export function VerifyCodeForm({
  email,
  code,
  notice,
  error,
  busy,
  remaining,
  canResend,
  onCode,
  onSubmit,
  onResend,
  onBack,
}: {
  email: string
  code: string
  notice: string | null
  error: string | null
  busy: boolean
  remaining: number
  canResend: boolean
  onCode: (value: string) => void
  onSubmit: (event: FormEvent) => void
  onResend: () => void
  onBack: () => void
}) {
  return (
    <section className="auth-card">
      <h1>Enter the code from your email</h1>
      <p className="muted">We sent a 6-digit code to {email}.</p>
      <form className="auth-form" onSubmit={onSubmit}>
        <label>
          Verification code
          <input
            type="text"
            inputMode="numeric"
            pattern="\d{6}"
            maxLength={6}
            value={code}
            onChange={(event) => onCode(event.target.value.replace(/\D/g, ''))}
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
      <button type="button" className="link-button" onClick={onResend} disabled={!canResend} aria-live="polite">
        {remaining > 0 ? `Send a new code in ${formatRemaining(remaining)}` : 'Send a new code'}
      </button>
      <button type="button" className="link-button" onClick={onBack} disabled={busy}>
        Back to sign in
      </button>
    </section>
  )
}
