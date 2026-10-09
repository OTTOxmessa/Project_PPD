import { useCallback, useEffect, useState } from 'react'
import apiClient from '../../api/client.js'
import { apiError, formatDateTime, formatNumber } from '../../utils/format.js'

const CONDITION_LABEL = {
  PRICE_ABOVE: 'ราคาสูงกว่าหรือเท่ากับ',
  PRICE_BELOW: 'ราคาต่ำกว่าหรือเท่ากับ',
}

const STATUS_BADGE = {
  PENDING: 'badge-yellow',
  TRIGGERED: 'badge-blue',
  NOTIFIED: 'badge-green',
  EXPIRED: '',
  CANCELLED: '',
}

function AlertsTab({ portfolioId, assets }) {
  const [alerts, setAlerts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [form, setForm] = useState({ assetId: '', condition: 'PRICE_ABOVE', targetPrice: '' })
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await apiClient.get(`/portfolios/${portfolioId}/alerts`)
      setAlerts([...res.data].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)))
    } catch (err) {
      setError(apiError(err, 'โหลดรายการแจ้งเตือนไม่สำเร็จ'))
    } finally {
      setLoading(false)
    }
  }, [portfolioId])

  useEffect(() => {
    load()
  }, [load])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await apiClient.post(`/portfolios/${portfolioId}/alerts`, {
        assetId: Number(form.assetId),
        condition: form.condition,
        targetPrice: form.targetPrice,
      })
      setForm({ assetId: '', condition: 'PRICE_ABOVE', targetPrice: '' })
      await load()
    } catch (err) {
      setError(apiError(err, 'สร้างการแจ้งเตือนไม่สำเร็จ'))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (alertId) => {
    if (!window.confirm('ลบการแจ้งเตือนนี้?')) return
    try {
      await apiClient.delete(`/portfolios/${portfolioId}/alerts/${alertId}`)
      await load()
    } catch (err) {
      setError(apiError(err, 'ลบไม่สำเร็จ'))
    }
  }

  return (
    <div>
      <form className="card" onSubmit={handleSubmit}>
        <h2>ตั้งการแจ้งเตือนราคา</h2>
        <div className="form-row">
          <div className="form-group">
            <label>สินทรัพย์</label>
            <select value={form.assetId} onChange={(e) => setForm({ ...form, assetId: e.target.value })} required>
              <option value="">-- เลือก --</option>
              {assets.map((a) => (
                <option key={a.id} value={a.id}>{a.symbol}</option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>เงื่อนไข</label>
            <select value={form.condition} onChange={(e) => setForm({ ...form, condition: e.target.value })}>
              <option value="PRICE_ABOVE">{CONDITION_LABEL.PRICE_ABOVE}</option>
              <option value="PRICE_BELOW">{CONDITION_LABEL.PRICE_BELOW}</option>
            </select>
          </div>
          <div className="form-group">
            <label>ราคาเป้าหมาย (USD)</label>
            <input type="number" step="any" min="0" value={form.targetPrice}
              onChange={(e) => setForm({ ...form, targetPrice: e.target.value })} required />
          </div>
        </div>
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? 'กำลังบันทึก...' : 'ตั้งการแจ้งเตือน'}
        </button>
        <p className="hint">
          ระบบตรวจราคาอัตโนมัติเป็นระยะ เมื่อถึงเงื่อนไขสถานะจะเปลี่ยนเป็น NOTIFIED — กด "รีเฟรช" เพื่อดูสถานะล่าสุด
        </p>
      </form>

      {error && <p className="error-message">{error}</p>}

      <div className="card table-wrap">
        <div className="card-header">
          <h2>รายการแจ้งเตือน</h2>
          <button className="btn btn-secondary" onClick={load}>รีเฟรช</button>
        </div>
        {loading ? (
          <p>กำลังโหลด...</p>
        ) : alerts.length === 0 ? (
          <p className="empty-state">ยังไม่มีการแจ้งเตือน</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Symbol</th>
                <th>เงื่อนไข</th>
                <th className="num">ราคาเป้าหมาย (USD)</th>
                <th>สถานะ</th>
                <th>สร้างเมื่อ</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {alerts.map((a) => (
                <tr key={a.id}>
                  <td><strong>{a.symbol}</strong></td>
                  <td>{CONDITION_LABEL[a.condition] || a.condition}</td>
                  <td className="num">{formatNumber(a.targetPrice)}</td>
                  <td><span className={`badge ${STATUS_BADGE[a.status] || ''}`}>{a.status}</span></td>
                  <td>{formatDateTime(a.createdAt)}</td>
                  <td><button className="btn-text-danger" onClick={() => handleDelete(a.id)}>ลบ</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

export default AlertsTab
