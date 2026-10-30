import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { getCurrencyOptions } from '../api/client'

const STORAGE_KEY = 'currency'

export interface CurrencyValue {
  currency: string
  options: string[]
  setCurrency: (code: string) => void
}

const CurrencyContext = createContext<CurrencyValue | null>(null)

export function CurrencyProvider({ children }: { children: ReactNode }) {
  const [currency, setCurrencyState] = useState(() => localStorage.getItem(STORAGE_KEY) ?? 'PLN')
  const [options, setOptions] = useState<string[]>([currency])

  useEffect(() => {
    const controller = new AbortController()
    getCurrencyOptions(controller.signal)
      .then((settings) => {
        setOptions(settings.supported)
        if (!localStorage.getItem(STORAGE_KEY)) setCurrencyState(settings.default)
      })
      .catch(() => {
        setOptions([currency])
      })
    return () => controller.abort()
  }, [])

  const setCurrency = useCallback((code: string) => {
    setCurrencyState(code)
    localStorage.setItem(STORAGE_KEY, code)
  }, [])

  const value = useMemo<CurrencyValue>(
    () => ({ currency, options, setCurrency }),
    [currency, options, setCurrency],
  )

  return <CurrencyContext.Provider value={value}>{children}</CurrencyContext.Provider>
}

export function useCurrency(): CurrencyValue {
  const value = useContext(CurrencyContext)
  if (!value) throw new Error('useCurrency must be used inside CurrencyProvider')
  return value
}
