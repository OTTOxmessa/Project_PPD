import { useEffect, useRef, useState } from 'react'
import apiClient from '../api/client.js'

// ช่องค้นหาหุ้นพร้อม dropdown แนะนำอัตโนมัติ
// พิมพ์ "aa" -> AA Alcoa Corporation, AAPL Apple Inc., AAOI Applied Optoelectronics ...
// รองรับคีย์บอร์ด: ↑ ↓ เลือก, Enter ยืนยัน, Esc ปิด
function SymbolSearch({
  onSelect,
  onQueryChange,
  placeholder = 'ค้นหาหุ้น เช่น AAPL, PTT',
  clearOnSelect = true,
  initialValue = '',
  className = '',
}) {
  const [query, setQuery] = useState(initialValue)
  const [results, setResults] = useState([])
  const [open, setOpen] = useState(false)
  const [highlight, setHighlight] = useState(0)
  const [loading, setLoading] = useState(false)
  const boxRef = useRef(null)
  const skipNextSearch = useRef(false)

  useEffect(() => {
    if (skipNextSearch.current) {
      skipNextSearch.current = false
      return
    }
    const q = query.trim()
    if (!q) {
      setResults([])
      setOpen(false)
      return
    }
    let cancelled = false
    const timer = setTimeout(async () => {
      setLoading(true)
      try {
        const res = await apiClient.get('/symbols/search', { params: { q, limit: 10 } })
        if (!cancelled) {
          setResults(res.data)
          setHighlight(0)
          setOpen(true)
        }
      } catch {
        if (!cancelled) setResults([])
      } finally {
        if (!cancelled) setLoading(false)
      }
    }, 200) // debounce: รอหยุดพิมพ์ 0.2 วินาทีค่อยค้นหา
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [query])

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (boxRef.current && !boxRef.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const choose = (item) => {
    onSelect(item)
    setOpen(false)
    setResults([])
    skipNextSearch.current = true
    const next = clearOnSelect ? '' : item.symbol
    setQuery(next)
    onQueryChange?.(next)
  }

  const handleChange = (e) => {
    setQuery(e.target.value)
    onQueryChange?.(e.target.value)
  }

  const handleKeyDown = (e) => {
    if (!open || results.length === 0) return
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setHighlight((h) => (h + 1) % results.length)
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setHighlight((h) => (h - 1 + results.length) % results.length)
    } else if (e.key === 'Enter') {
      e.preventDefault()
      choose(results[highlight])
    } else if (e.key === 'Escape') {
      setOpen(false)
    }
  }

  return (
    <div className={`symbol-search ${className}`} ref={boxRef}>
      <input
        type="text"
        value={query}
        onChange={handleChange}
        onFocus={() => results.length > 0 && setOpen(true)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        autoComplete="off"
      />
      {open && query.trim() && (
        <ul className="symbol-dropdown">
          {loading && results.length === 0 && <li className="symbol-empty">กำลังค้นหา...</li>}
          {!loading && results.length === 0 && <li className="symbol-empty">ไม่พบ "{query}"</li>}
          {results.map((r, i) => (
            <li
              key={r.symbol}
              className={`symbol-option ${i === highlight ? 'active' : ''}`}
              onMouseDown={(e) => {
                e.preventDefault()
                choose(r)
              }}
              onMouseEnter={() => setHighlight(i)}
            >
              <span className="symbol-code">{r.symbol}</span>
              <span className="symbol-name">{r.name}</span>
              {r.assetId && <span className="symbol-tag">ในระบบ</span>}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default SymbolSearch
