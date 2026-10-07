import { useEffect, useRef, useState } from 'react'
import apiClient from '../../api/client.js'
import { ensureAsset } from '../../api/assets.js'
import { apiError, formatNumber, formatPercent } from '../../utils/format.js'
import SymbolSearch from '../SymbolSearch.jsx'
import '../../source-tag.css'

// คอลัมน์ขวา: Watchlist — % รายวันเป็นสีเขียว (บวก) / แดง (ลบ) ชัดเจน
function Watchlist({ selectedAssetId, onSelect, refreshKey, onLoaded }) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const onLoadedRef = useRef(onLoaded)
  onLoadedRef.current = onLoaded

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      try {
        const res = await apiClient.get('/watchlist')
        if (!cancelled) {
          setItems(res.data)
          onLoadedRef.current?.(res.data)
        }
      } catch (err) {
        if (!cancelled) setError(apiError(err, 'โหลด Watchlist ไม่สำเร็จ'))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [refreshKey])

  const reload = async () => {
    const res = await apiClient.get('/watchlist')
    setItems(res.data)
    onLoadedRef.current?.(res.data)
  }

  const handleAdd = async (suggestion) => {
    setError(null)
    try {
      const assetId = await ensureAsset(suggestion)
      await apiClient.post('/watchlist', { assetId })
      await reload()
      onSelect(assetId)
    } catch (err) {
      setError(apiError(err, 'เพิ่มไม่สำเร็จ'))
    }
  }

  const handleRemove = async (e, assetId) => {
    e.stopPropagation()
    try {
      await apiClient.delete(`/watchlist/${assetId}`)
      await reload()
    } catch (err) {
      setError(apiError(err, 'ลบไม่สำเร็จ'))
    }
  }

  return (
    <div className="panel">
      <div className="panel-header">
        <h2>Watchlist</h2>
        <span className="muted small">{items.length} รายการ</span>
      </div>

      <SymbolSearch onSelect={handleAdd} placeholder="+ เพิ่มหุ้น เช่น NVDA, MSFT" />

      {error && <p className="error-message">{error}</p>}

      {loading ? (
        <p className="muted">กำลังโหลด...</p>
      ) : items.length === 0 ? (
        <p className="empty-state">ยังไม่มีหุ้นใน Watchlist ค้นหาด้านบนเพื่อเพิ่ม</p>
      ) : (
        <ul className="watchlist">
          {items.map((q) => {
            const pct = q.changePercent === null ? null : Number(q.changePercent)
            const tone = pct === null ? '' : pct > 0 ? 'up' : pct < 0 ? 'down' : 'flat'
            return (
              <li
                key={q.assetId}
                className={`watchlist-row ${q.assetId === selectedAssetId ? 'selected' : ''}`}
                onClick={() => onSelect(q.assetId)}
              >
                <div className="watchlist-symbol">
                  <strong>
                    {q.symbol}
                    {q.priceSource !== 'YAHOO' && <span className="source-tag" title="ข้อมูลราคาจำลอง">จำลอง</span>}
                  </strong>
                  <span className="muted small watchlist-name">{q.name}</span>
                </div>
                <div className="watchlist-price">
                  <span>{formatNumber(q.price)}</span>
                  <span className={`change-pill ${tone}`}>{pct === null ? '-' : formatPercent(pct)}</span>
                </div>
                <button className="remove-btn" title="เอาออกจาก Watchlist" onClick={(e) => handleRemove(e, q.assetId)}>
                  ×
                </button>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}

export default Watchlist
