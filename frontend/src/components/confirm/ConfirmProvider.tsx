import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { ConfirmDialog } from './ConfirmDialog'

export interface ConfirmRequest {
  title: string
  body?: string
  confirmLabel?: string
  cancelLabel?: string
  danger?: boolean
}

interface ConfirmValue {
  confirm: (request: ConfirmRequest) => Promise<boolean>
}

const ConfirmContext = createContext<ConfirmValue | null>(null)

export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [request, setRequest] = useState<ConfirmRequest | null>(null)
  const [resolver, setResolver] = useState<((answer: boolean) => void) | null>(null)

  const confirm = useCallback(
    (next: ConfirmRequest) =>
      new Promise<boolean>((resolve) => {
        setRequest(next)
        setResolver(() => resolve)
      }),
    [],
  )

  const settle = useCallback(
    (answer: boolean) => {
      resolver?.(answer)
      setResolver(null)
      setRequest(null)
    },
    [resolver],
  )

  const value = useMemo<ConfirmValue>(() => ({ confirm }), [confirm])

  return (
    <ConfirmContext.Provider value={value}>
      {children}
      {request && <ConfirmDialog request={request} onAnswer={settle} />}
    </ConfirmContext.Provider>
  )
}

export function useConfirm(): (request: ConfirmRequest) => Promise<boolean> {
  const value = useContext(ConfirmContext)
  if (!value) throw new Error('useConfirm must be used inside ConfirmProvider')
  return value.confirm
}
