import { useCurrency } from '../currency/CurrencyContext'

export function CurrencySelector() {
  const { currency, options, setCurrency } = useCurrency()

  return (
    <select
      className="currency-selector"
      aria-label="Currency"
      value={currency}
      onChange={(event) => setCurrency(event.target.value)}
    >
      {options.map((code) => (
        <option key={code} value={code}>
          {code}
        </option>
      ))}
    </select>
  )
}
