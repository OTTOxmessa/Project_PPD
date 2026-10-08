import { useCallback, useEffect, useState } from 'react'
import apiClient from '../../api/client.js'
import { ensureAsset } from '../../api/assets.js'
import { apiError, formatDateTime, formatNumber, formatPercent, today } from '../../utils/format.js'
import SymbolSearch from '../SymbolSearch.jsx'
import '../../source-tag.css'

const EMPTY_FORM = { type: 'BUY', quantity: '', price: '', executedAt: '' }

// ค้นหาหุ้นแล้วซื้อได้เลย — หุ้นที่ยังไม่อยู่ในระบบจะถูกเพิ่มให้อัตโนมัติ (ไม่ต้องไปหน้าสินทรัพย์ก่อน)
function HoldingsTab({ portfolioId, onAssetsChanged }) {
  const [holdings, setHoldings] = useState([])
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selected, setSelected] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [searchKey, setSearchKey] = useState(0)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [h, t] = await Promise.all([
        apiClient.get(`/portfolios/${portfolioId}/holdings`),
        apiClient.get(`/portfolios/${portfolioId}/transactions`),
      ])
      setHoldings(h.data)
      setTransactions([...t.data].sort((a, b) => new Date(b.executedAt) - new Date(a.executedAt)))
    } catch (err) {
      setError(apiError(err, 'โหลดข้อมูลไม่สำเร็จ'))
    } finally {
      setLoading(false)
    }
  }, [portfolioId])

  useEffect(() => {
    load()
  }, [load])

  const handleSelect = async (suggestion) => {
    setSelected(suggestion)
    setForm((f) => ({ ...f, price: '' }))
    if (!suggestion.assetId) return // หุ้นใหม่: ให้ผู้ใช้กรอกราคาเอง
    try {
      const res = await apiClient.get(`/assets/${suggestion.assetId}/quote`)
      // ใส่ราคาล่าสุดให้เฉพาะตอนช่องราคายังว่าง — ถ้าผู้ใช้พิมพ์ราคาเองก่อนที่ราคาจะโหลดเสร็จ ห้ามเขียนทับ
      if (res.data.price !== null) {
        setForm((f) => (f.price === '' ? { ...f, price: String(res.data.price) } : f))
      }
    } catch {
      // ไม่มีราคา ให้กรอกเอง
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!selected) {
      setError('กรุณาค้นหาและเลือกหุ้นก่อน')
      return
    }
    setSaving(true)
    setError(null)
    try {
      const assetId = await ensureAsset(selected, form.price)
      await apiClient.post(`/portfolios/${portfolioId}/transactions`, {
        assetId,
        type: form.type,
        quantity: form.quantity,
        price: form.price,
        executedAt: form.executedAt || null,
      })
      setSelected(null)
      setForm(EMPTY_FORM)
      setSearchKey((k) => k + 1)
      await load()
      if (!selected.assetId) onAssetsChanged?.() // มีหุ้นใหม่เข้าระบบ ให้แท็บอื่นเห็นด้วย
    } catch (err) {
      setError(apiError(err, 'บันทึกรายการไม่สำเร็จ'))
    } finally {
      setSaving(false)
    }
  }

  const totals = holdings.reduce(
    (acc, h) => ({ market: acc.market + Number(h.marketValue), cost: acc.cost + Number(h.costValue) }),
    { market: 0, cost: 0 },
  )
  const totalGain = totals.market - totals.cost

  return (
    <div>
      <form className="card" onSubmit={handleSubmit}>
        <h2>ซื้อ-ขายหุ้น</h2>
        <div className="form-row">
          <div className="form-group form-group-wide">
            <label>หุ้น</label>
            <SymbolSearch key={searchKey} onSelect={handleSelect} placeholder="พิมพ์ชื่อหรือ symbol เช่น aapl, ptt" />
            {selected && (
              <div className="selected-asset">
                <strong>{selected.symbol}</strong> {selected.name}
                {!selected.assetId && <span className="symbol-tag new">หุ้นใหม่ — จะเพิ่มเข้าระบบให้อัตโนมัติ</span>}
              </div>
            )}
          </div>
          <div className="form-group">
            <label>ประเภท</label>
            <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
              <option value="BUY">ซื้อ (BUY)</option>
              <option value="SELL">ขาย (SELL)</option>
            </select>
          </div>
          <div className="form-group">
            <label>จำนวน</label>
            <input type="number" step="any" min="0" value={form.quantity}
              onChange={(e) => setForm({ ...form, quantity: e.target.value })} required />
          </div>
          <div className="form-group">
            <label>ราคาต่อหน่วย</label>
            <input type="number" step="any" min="0" value={form.price}
              onChange={(e) => setForm({ ...form, price: e.target.value })} required />
          </div>
          <div className="form-group">
            <label>วันที่ (เว้นว่าง = วันนี้)</label>
            <input type="date" max={today()} value={form.executedAt}
              onChange={(e) => setForm({ ...form, executedAt: e.target.value })} />
          </div>
        </div>
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? 'กำลังบันทึก...' : 'บันทึกรายการ'}
        </button>
      </form>

      {error && <p className="error-message">{error}</p>}

      {loading ? (
        <p className="muted">กำลังโหลด...</p>
      ) : (
        <>
          <div className="card table-wrap">
            <h2>สินทรัพย์ที่ถืออยู่</h2>
            {holdings.length === 0 ? (
              <p className="empty-state">ยังไม่มีสินทรัพย์ในพอร์ต — ค้นหาหุ้นด้านบนเพื่อซื้อได้เลย</p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>หุ้น</th>
                    <th className="num">จำนวน</th>
                    <th className="num">ต้นทุนเฉลี่ย</th>
                    <th className="num">ราคาล่าสุด</th>
                    <th className="num">มูลค่าตลาด</th>
                    <th className="num">กำไร/ขาดทุน</th>
                  </tr>
                </thead>
                <tbody>
                  {holdings.map((h) => {
                    const up = Number(h.gain) >= 0
                    return (
                      <tr key={h.assetId}>
                        <td>
                          <strong>{h.symbol}</strong>
                          {h.priceSource !== 'YAHOO' && (
                            <span className="source-tag" title="เชื่อมต่อแหล่งราคาจริงไม่ได้ มูลค่าคำนวณจากราคาจำลอง">จำลอง</span>
                          )}
                          <div className="muted small">{h.name}</div>
                        </td>
                        <td className="num">{formatNumber(h.quantity, 4)}</td>
                        <td className="num">{formatNumber(h.avgCost)}</td>
                        <td className="num">{formatNumber(h.latestPrice)}</td>
                        <td className="num">{formatNumber(h.marketValue)}</td>
                        <td className={`num ${up ? 'text-green' : 'text-red'}`}>
                          {up ? '+' : ''}{formatNumber(h.gain)}
                          <div className="small">{formatPercent(h.gainPercent)}</div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
                <tfoot>
                  <tr>
                    <td colSpan={4}><strong>รวม</strong></td>
                    <td className="num"><strong>{formatNumber(totals.market)}</strong></td>
                    <td className={`num ${totalGain >= 0 ? 'text-green' : 'text-red'}`}>
                      <strong>{totalGain >= 0 ? '+' : ''}{formatNumber(totalGain)}</strong>
                    </td>
                  </tr>
                </tfoot>
              </table>
            )}
          </div>

          <div className="card table-wrap">
            <h2>ประวัติรายการ</h2>
            {transactions.length === 0 ? (
              <p className="empty-state">ยังไม่มีรายการ</p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>วันที่</th>
                    <th>หุ้น</th>
                    <th>ประเภท</th>
                    <th className="num">จำนวน</th>
                    <th className="num">ราคา</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((t) => (
                    <tr key={t.id}>
                      <td>{formatDateTime(t.executedAt)}</td>
                      <td>{t.symbol}</td>
                      <td><span className={`badge ${t.type === 'BUY' ? 'badge-green' : 'badge-red'}`}>{t.type}</span></td>
                      <td className="num">{formatNumber(t.quantity, 4)}</td>
                      <td className="num">{formatNumber(t.price)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}
    </div>
  )
}

export default HoldingsTab
