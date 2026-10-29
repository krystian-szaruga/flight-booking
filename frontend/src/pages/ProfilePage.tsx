import { useEffect, useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useRequireAuth } from '../auth/useRequireAuth'
import { useConfirm } from '../components/confirm/ConfirmProvider'

export function ProfilePage() {
  const { user, updateProfile } = useAuth()
  const confirm = useConfirm()
  useRequireAuth()

  const [firstName, setFirstName] = useState(user?.firstName ?? '')
  const [lastName, setLastName] = useState(user?.lastName ?? '')
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    setFirstName(user?.firstName ?? '')
    setLastName(user?.lastName ?? '')
  }, [user])

  const complete = Boolean(user?.firstName && user?.lastName)
  const dirty = firstName.trim() !== (user?.firstName ?? '') || lastName.trim() !== (user?.lastName ?? '')

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setBusy(true)
    setError(null)
    setNotice(null)
    try {
      const answer = await confirm({
        title: 'Save these details?',
        body: `Seats you book for yourself will use ${firstName.trim()} ${lastName.trim()}.`,
        confirmLabel: 'Save',
      })
      if (!answer) return
      await updateProfile(firstName, lastName)
      setNotice('Saved.')
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Could not save your details.')
    } finally {
      setBusy(false)
    }
  }

  if (!user) return <p className="muted">Checking your session…</p>

  return (
    <section className="auth-card">
      <h1>Your details</h1>
      <p className="muted">{user.email}</p>
      <p className="muted">
        {complete
          ? 'Seats you book for yourself use this name.'
          : 'Add your name so you can book seats for yourself in one click.'}
      </p>
      <form className="auth-form" onSubmit={submit}>
        <label>
          First name
          <input
            type="text"
            value={firstName}
            onChange={(event) => setFirstName(event.target.value)}
            maxLength={100}
            required
            autoComplete="given-name"
          />
        </label>
        <label>
          Last name
          <input
            type="text"
            value={lastName}
            onChange={(event) => setLastName(event.target.value)}
            maxLength={100}
            required
            autoComplete="family-name"
          />
        </label>
        {notice && <p className="muted">{notice}</p>}
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={busy || !dirty}>
          {busy ? 'Saving…' : 'Save details'}
        </button>
      </form>
    </section>
  )
}
