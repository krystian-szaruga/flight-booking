import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function SessionBadge() {
  const { user, loading, signOut } = useAuth()
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
      <span className="muted">{user.email}</span>
      <button
        type="button"
        className="link-button"
        onClick={() => {
          void signOut().then(() => navigate('/'))
        }}
      >
        Sign out
      </button>
    </div>
  )
}
