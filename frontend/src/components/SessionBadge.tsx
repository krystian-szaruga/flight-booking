import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useConfirm } from './confirm/ConfirmProvider'

export function SessionBadge() {
  const { user, loading, signOut } = useAuth()
  const confirm = useConfirm()
  const navigate = useNavigate()
  const location = useLocation()
  const [open, setOpen] = useState(false)
  const container = useRef<HTMLDivElement>(null)

  useEffect(() => {
    setOpen(false)
  }, [location.pathname])

  useEffect(() => {
    if (!open) return
    const onPointer = (event: MouseEvent) => {
      if (container.current && !container.current.contains(event.target as Node)) setOpen(false)
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onPointer)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onPointer)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  if (loading) return null
  if (!user) {
    return (
      <Link to="/auth" className="button">
        Sign in
      </Link>
    )
  }

  const label = user.firstName ? `${user.firstName} ${user.lastName}` : 'Your account'

  const handleSignOut = async () => {
    setOpen(false)
    const answer = await confirm({
      title: 'Sign out?',
      body: 'Your holds stay in place, but you will need to sign in again to see them.',
      confirmLabel: 'Sign out',
    })
    if (answer) void signOut().then(() => navigate('/'))
  }

  return (
    <div className="account-menu" ref={container}>
      <button
        type="button"
        className="account-trigger"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        <span className="account-avatar" aria-hidden="true">
          {(user.firstName?.[0] ?? user.email[0]).toUpperCase()}
        </span>
        <span className="account-name">{label}</span>
        <span className="account-caret" aria-hidden="true">
          ▾
        </span>
      </button>
      {open && (
        <div className="account-dropdown" role="menu">
          <div className="account-dropdown-header">
            <strong>{label}</strong>
            <small>{user.email}</small>
          </div>
          <Link to="/profile" className="account-item" role="menuitem">
            Your details
          </Link>
          <Link to="/reservations" className="account-item" role="menuitem">
            My reservations
          </Link>
          <Link to="/settings" className="account-item" role="menuitem">
            Settings
          </Link>
          <button type="button" className="account-item account-item-danger" role="menuitem" onClick={handleSignOut}>
            Sign out
          </button>
        </div>
      )}
    </div>
  )
}
