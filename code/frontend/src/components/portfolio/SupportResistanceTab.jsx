import { useEffect, useState } from 'react'
import { CartesianGrid, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import apiClient from '../../api/client.js'
import { apiError, daysAgo, today } from '../../utils/format.js'
import { useCurrency } from '../../context/CurrencyContext.jsx'

function SupportResistanceTab({ assets }) {
  const { money } = useCurrency()
  const [assetId, setAssetId] = useState(assets[0]?.id ?? '')
  const [method, setMethod] = useState('pivot')
  const [from, setFrom] = useState(daysAgo(60))
  const [to, setTo] = useState(today())
  const [levels, setLevels] = useState(null)
  const [prices, setPrices] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!assetId || !from || !to) return
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const [levelRes, priceRes] = await Promise.all([
          apiClient.get(`/assets/${assetId}/support-resistance`, { params: { method, from, to } }),
          apiClient.get(`/assets/${assetId}/prices`, { params: { from, to } }),
        ])
        setLevels(levelRes.data)
        setPrices(priceRes.data.map((p) => ({ date: p.date, close: Number(p.close) })))
      } catch (err) {
        setLevels(null)
        setPrices([])
        setError(apiError(err, 'คำนวณแนวรับ-แนวต้านไม่สำเร็จ'))
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [assetId, method, from, to])

  if (assets.length === 0) {
    return <p className="empty-state">ยังไม่มีสินทรัพย์ในระบบ</p>
  }

  return (
    <div className="card">
      <div className="form-row">
        <div className="form-group">
          <label>สินทรัพย์</label>
          <select value={assetId} onChange={(e) => setAssetId(e.target.value)}>
            {assets.map((a) => (
              <option key={a.id} value={a.id}>{a.symbol}</option>
            ))}
          </select>
        </div>
        <div className="form-group">
          <label>วิธีคำนวณ (Strategy)</label>
          <select value={method} onChange={(e) => setMethod(e.target.value)}>
            <option value="pivot">Pivot Point</option>
            <option value="ma">Moving Average Band</option>
          </select>
        </div>
        <div className="form-group">
          <label>จากวันที่</label>
          <input type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div className="form-group">
          <label>ถึงวันที่</label>
          <input type="date" value={to} max={today()} onChange={(e) => setTo(e.target.value)} />
        </div>
      </div>

      {error && <p className="error-message">{error}</p>}
      {loading && <p>กำลังคำนวณ...</p>}

      {levels && !loading && (
        <>
          <div className="summary-grid">
            <div className="stat">
              <span className="stat-label">แนวต้าน</span>
              <span className="stat-value text-red">{money(levels.resistance)}</span>
            </div>
            <div className="stat">
              <span className="stat-label">{method === 'pivot' ? 'Pivot' : 'ค่าเฉลี่ย (MA)'}</span>
              <span className="stat-value">{money(levels.pivot)}</span>
            </div>
            <div className="stat">
              <span className="stat-label">แนวรับ</span>
              <span className="stat-value text-green">{money(levels.support)}</span>
            </div>
          </div>

          <ResponsiveContainer width="100%" height={340}>
            <LineChart data={prices}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" minTickGap={30} />
              <YAxis domain={['auto', 'auto']} tickFormatter={(v) => money(v, 0)} />
              <Tooltip formatter={(v) => money(v)} />
              <Line type="monotone" dataKey="close" name="ราคาปิด" stroke="#2563eb" dot={false} />
              <ReferenceLine y={Number(levels.resistance)} stroke="#dc2626" strokeDasharray="6 4"
                label={{ value: 'แนวต้าน', position: 'insideTopRight', fill: '#dc2626' }} />
              <ReferenceLine y={Number(levels.pivot)} stroke="#64748b" strokeDasharray="2 4" />
              <ReferenceLine y={Number(levels.support)} stroke="#16a34a" strokeDasharray="6 4"
                label={{ value: 'แนวรับ', position: 'insideBottomRight', fill: '#16a34a' }} />
            </LineChart>
          </ResponsiveContainer>
        </>
      )}
    </div>
  )
}

export default SupportResistanceTab
