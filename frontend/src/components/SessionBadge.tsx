import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useConfirm } from './confirm/ConfirmProvider'

export function SessionBadge() {
  const { user, loading, signOut } = useAuth()
  const confirm = useConfirm()
  const navigate = useNavigate()

  if (loading) return null
  if (!user) {
    return (
      <Link to="/auth" className="button">
        Sign in
      </Link>
    )
  }
  return (
    <div className="session-badge">
      <Link to="/reservations" className="link-button">
        My reservations
      </Link>
      <Link to="/profile" className="link-button">
        {user.firstName ? `${user.firstName} ${user.lastName}` : 'Your details'}
      </Link>
      <button
        type="button"
        className="link-button"
        onClick={async () => {
          const answer = await confirm({
            title: 'Sign out?',
            body: 'Your holds stay in place, but you will need to sign in again to see them.',
            confirmLabel: 'Sign out',
          })
          if (answer) void signOut().then(() => navigate('/'))
        }}
      >
        Sign out
      </button>
    </div>
  )
}
