import { useEffect, useRef } from 'react'
import type { ConfirmRequest } from './ConfirmProvider'

export function ConfirmDialog({
  request,
  onAnswer,
}: {
  request: ConfirmRequest
  onAnswer: (answer: boolean) => void
}) {
  const confirmRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    confirmRef.current?.focus()
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onAnswer(false)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onAnswer])

  return (
    <div className="dialog-backdrop" role="presentation" onClick={() => onAnswer(false)}>
      <div
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="confirm-title">{request.title}</h2>
        {request.body && <p className="muted">{request.body}</p>}
        <div className="dialog-actions">
          <button type="button" className="link-button" onClick={() => onAnswer(false)}>
            {request.cancelLabel ?? 'Cancel'}
          </button>
          <button
            ref={confirmRef}
            type="button"
            className={request.danger ? 'button dialog-danger' : 'button'}
            onClick={() => onAnswer(true)}
          >
            {request.confirmLabel ?? 'Confirm'}
          </button>
        </div>
      </div>
    </div>
  )
}
