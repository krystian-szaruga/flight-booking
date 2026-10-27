import { useEffect, useId, useRef, useState } from 'react'
import { searchAirports } from '../api/client'
import type { AirportSummary } from '../api/types'

const DEBOUNCE_MS = 200

function label(airport: AirportSummary): string {
  return `${airport.city} (${airport.iataCode})`
}

export function AirportCombobox({
  value,
  onChange,
  placeholder,
}: {
  value: AirportSummary | null
  onChange: (airport: AirportSummary | null) => void
  placeholder: string
}) {
  const listId = useId()
  const [text, setText] = useState(value ? label(value) : '')
  const [options, setOptions] = useState<AirportSummary[]>([])
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(-1)
  const skipNextSearch = useRef(false)

  useEffect(() => {
    setText(value ? label(value) : '')
    skipNextSearch.current = true
  }, [value])

  useEffect(() => {
    if (skipNextSearch.current) {
      skipNextSearch.current = false
      return
    }
    if (value && text === label(value)) return

    const query = text.trim()
    if (query.length < 1) {
      setOptions([])
      return
    }

    const controller = new AbortController()
    const timer = window.setTimeout(() => {
      searchAirports(query, controller.signal)
        .then((results) => {
          setOptions(results)
          setActive(results.length > 0 ? 0 : -1)
        })
        .catch(() => setOptions([]))
    }, DEBOUNCE_MS)

    return () => {
      window.clearTimeout(timer)
      controller.abort()
    }
  }, [text])

  function choose(airport: AirportSummary) {
    onChange(airport)
    setText(label(airport))
    setOptions([])
    setOpen(false)
    setActive(-1)
  }

  function handleKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setOpen(true)
      setActive((current) => Math.min(current + 1, options.length - 1))
      return
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault()
      setActive((current) => Math.max(current - 1, 0))
      return
    }
    if (event.key === 'Enter' && open && active >= 0 && options[active]) {
      event.preventDefault()
      choose(options[active])
      return
    }
    if (event.key === 'Escape') {
      setOpen(false)
    }
  }

  return (
    <div className="combobox">
      <input
        role="combobox"
        aria-expanded={open}
        aria-controls={listId}
        aria-autocomplete="list"
        autoComplete="off"
        placeholder={placeholder}
        value={text}
        onFocus={() => setOpen(true)}
        onBlur={() => window.setTimeout(() => setOpen(false), 120)}
        onChange={(event) => {
          setText(event.target.value)
          onChange(null)
          setOpen(true)
        }}
        onKeyDown={handleKeyDown}
      />
      {open && options.length > 0 && (
        <ul className="combobox-list" id={listId} role="listbox">
          {options.map((airport, index) => (
            <li
              key={airport.iataCode}
              role="option"
              aria-selected={index === active}
              className={index === active ? 'combobox-option active' : 'combobox-option'}
              onMouseDown={(event) => {
                event.preventDefault()
                choose(airport)
              }}
              onMouseEnter={() => setActive(index)}
            >
              <span className="combobox-code">{airport.iataCode}</span>
              <span>
                {airport.city} · {airport.name}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
