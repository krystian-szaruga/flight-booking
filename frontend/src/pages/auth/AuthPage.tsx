import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { useCountdown } from '../../components/useCountdown'
import { describeAuthError, safeRedirect } from './authShared'
import { SignInForm, type AuthMode } from './SignInForm'
import { VerifyCodeForm } from './VerifyCodeForm'

export function AuthPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { user, signIn, signUp, verify, resend, refresh, signOut } = useAuth()

  const [mode, setMode] = useState<AuthMode>('login')
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
  const redirectTo = safeRedirect(params.get('redirect'))

  useEffect(() => {
    if (user?.emailVerifiedAt) navigate(redirectTo, { replace: true })
  }, [user, navigate, redirectTo])

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (awaitingCode) {
        await verify(awaitingCode, code)
        navigate(redirectTo, { replace: true })
      } else if (mode === 'login') {
        await signIn(email, password)
        navigate(redirectTo, { replace: true })
      } else {
        await startRegistration()
      }
    } catch (cause) {
      if (!(await handleVerifyConflict(cause))) {
        setError(cause instanceof ApiError ? describeAuthError(cause) : 'Something went wrong. Try again.')
      }
    } finally {
      setBusy(false)
    }
  }

  const startRegistration = async () => {
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
      setCooldownSeconds(await resend(awaitingCode))
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
    return (
      <VerifyCodeForm
        email={awaitingCode}
        code={code}
        notice={notice}
        error={error}
        busy={busy}
        remaining={remaining}
        canResend={remaining === 0 && !busy}
        onCode={setCode}
        onSubmit={submit}
        onResend={onResend}
        onBack={backToLogin}
      />
    )
  }

  return (
    <SignInForm
      mode={mode}
      email={email}
      password={password}
      showPassword={showPassword}
      notice={notice}
      error={error}
      busy={busy}
      onEmail={setEmail}
      onPassword={setPassword}
      onTogglePassword={() => setShowPassword((current) => !current)}
      onSubmit={submit}
      onSwitchMode={switchMode}
    />
  )
}
