import { useCallback, useEffect, useState } from 'react'
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import apiClient from '../../api/client.js'
import { apiError, formatNumber, formatPercent } from '../../utils/format.js'
import { useCurrency } from '../../context/CurrencyContext.jsx'

const COLORS = ['#3b82f6', '#f59e0b', '#10b981', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#84cc16', '#f97316', '#6366f1']

const METHODS = [
  { value: 'target', label: 'ตามมูลค่าตลาดจริง' },
  { value: 'equal', label: 'แบ่งเท่ากัน (Equal Weight)' },
  { value: 'risk', label: 'ถ่วงน้ำหนักความเสี่ยง (Risk-Based)' },
]

const TOOLTIP_STYLE = { backgroundColor: '#1b2030', border: '1px solid #2a2e39', borderRadius: 6 }

function AllocationTab({ portfolioId }) {
  const { currency, money } = useCurrency()
  const [holdings, setHoldings] = useState([])
  const [targets, setTargets] = useState({})
  const [targetSymbols, setTargetSymbols] = useState({})
  const [comparison, setComparison] = useState([])
  const [method, setMethod] = useState('target')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [message, setMessage] = useState(null)
  const [editing, setEditing] = useState(false)
  const [targetInputs, setTargetInputs] = useState({})
  const [saving, setSaving] = useState(false)

  const loadBase = useCallback(async () => {
    const [h, t] = await Promise.all([
      apiClient.get(`/portfolios/${portfolioId}/holdings`),
      apiClient.get(`/portfolios/${portfolioId}/allocation/targets`),
    ])
    setHoldings(h.data)
    const map = {}
    const symbols = {}
    t.data.forEach((x) => {
      map[x.assetId] = Number(x.targetPercent)
      symbols[x.assetId] = x.symbol
    })
    setTargets(map)
    setTargetSymbols(symbols)
  }, [portfolioId])

  const loadComparison = useCallback(async () => {
    const res = await apiClient.get(`/portfolios/${portfolioId}/allocation`, { params: { method } })
    setComparison(res.data.map((r) => ({
      symbol: r.symbol,
      current: Number(r.currentPercent),
      target: Number(r.targetPercent),
    })))
  }, [portfolioId, method])

  useEffect(() => {
    setLoading(true)
    setError(null)
    loadBase()
      .catch((err) => setError(apiError(err, 'โหลดสัดส่วนไม่สำเร็จ')))
      .finally(() => setLoading(false))
  }, [loadBase])

  useEffect(() => {
    loadComparison().catch((err) => setError(apiError(err, 'โหลดกราฟเปรียบเทียบไม่สำเร็จ')))
  }, [loadComparison])

  const total = holdings.reduce((s, h) => s + Number(h.marketValue), 0)
  const rows = holdings
    .map((h, i) => ({
      ...h,
      color: COLORS[i % COLORS.length],
      value: Number(h.marketValue),
      percent: total > 0 ? (Number(h.marketValue) / total) * 100 : 0,
      target: targets[h.assetId] ?? 0,
    }))
    .sort((a, b) => b.value - a.value)

  // สินทรัพย์ที่ขายหมดแล้วแต่ยังมีเป้าค้างอยู่ — backend นับรวมใน 100% ด้วย จึงต้องแสดงให้เห็นและแก้เป็น 0 ได้
  const heldIds = new Set(holdings.map((h) => h.assetId))
  const orphanRows = Object.entries(targets)
    .filter(([assetId, pct]) => !heldIds.has(Number(assetId)) && pct > 0)
    .map(([assetId, pct]) => ({
      assetId: Number(assetId), symbol: targetSymbols[assetId], name: 'ขายหมดแล้ว — ยังมีเป้าค้างอยู่',
      color: 'var(--muted)', value: 0, percent: 0, target: pct,
    }))
  const tableRows = [...rows, ...orphanRows]

  const startEditing = () => {
    const inputs = {}
    tableRows.forEach((r) => {
      inputs[r.assetId] = String(r.target)
    })
    setTargetInputs(inputs)
    setMessage(null)
    setEditing(true)
  }

  const inputSum = Object.values(targetInputs).reduce((s, v) => s + (Number(v) || 0), 0)
  const overLimit = inputSum > 100.01 // เกณฑ์เดียวกับ backend (เผื่อปัดเศษ 0.01%)

  const handleSave = async () => {
    setSaving(true)
    setError(null)
    try {
      const changed = tableRows.filter((r) => Number(targetInputs[r.assetId]) !== Number(r.target))
      // ส่งทั้งชุดในครั้งเดียว: backend ตรวจผลรวมไม่เกิน 100% กับค่าชุดใหม่ และบันทึกทั้งหมดหรือไม่บันทึกเลย
      if (changed.length) {
        await apiClient.put(`/portfolios/${portfolioId}/allocation/targets/bulk`, {
          targets: changed.map((r) => ({ assetId: r.assetId, targetPercent: Number(targetInputs[r.assetId]) || 0 })),
        })
      }
      await Promise.all([loadBase(), loadComparison()])
      setMessage(changed.length ? `บันทึกเป้าหมาย ${changed.length} รายการแล้ว` : 'ไม่มีรายการที่เปลี่ยนแปลง')
      setEditing(false)
    } catch (err) {
      setError(apiError(err, 'บันทึกเป้าหมายไม่สำเร็จ'))
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <p className="muted">กำลังโหลด...</p>

  if (holdings.length === 0) {
    return <p className="empty-state">พอร์ตยังไม่มีสินทรัพย์ — เพิ่มหุ้นในแท็บ "สินทรัพย์ที่ถือ" ก่อน</p>
  }

  return (
    <div>
      {error && <p className="error-message">{error}</p>}

      {/* ---------- Donut + รายละเอียด ---------- */}
      <div className="card allocation-overview">
        <div className="donut-wrap">
          <ResponsiveContainer width="100%" height={280}>
            <PieChart>
              <Pie data={rows} dataKey="value" nameKey="symbol" innerRadius={82} outerRadius={120}
                paddingAngle={1} stroke="none" isAnimationActive={false}>
                {rows.map((r) => (
                  <Cell key={r.assetId} fill={r.color} />
                ))}
              </Pie>
              <Tooltip contentStyle={TOOLTIP_STYLE}
                formatter={(v, name) => [`${money(v)} ${currency} (${formatNumber((v / total) * 100)}%)`, name]} />
            </PieChart>
          </ResponsiveContainer>
          <div className="donut-center">
            <span className="muted small">มูลค่ารวม ({currency})</span>
            <strong>{money(total)}</strong>
          </div>
        </div>

        <div className="allocation-details">
          <div className="card-header">
            <h2>รายละเอียดสินทรัพย์</h2>
            {!editing && (
              <button className="btn btn-secondary btn-sm" onClick={startEditing}>แก้ไขเป้าหมาย</button>
            )}
          </div>
          <table className="data-table compact">
            <thead>
              <tr>
                <th>สินทรัพย์</th>
                <th className="num">มูลค่า</th>
                <th className="num">สัดส่วน</th>
                <th className="num">เป้าหมาย</th>
                <th className="num">ส่วนต่าง</th>
              </tr>
            </thead>
            <tbody>
              {tableRows.map((r) => {
                const drift = r.percent - r.target
                return (
                  <tr key={r.assetId}>
                    <td>
                      <span className="color-dot" style={{ backgroundColor: r.color }} />
                      <strong>{r.symbol}</strong>
                      <div className="muted small">{r.name}</div>
                    </td>
                    <td className="num">{money(r.value)}</td>
                    <td className="num">{formatNumber(r.percent)}%</td>
                    <td className="num">
                      {editing ? (
                        <input className="input-small" type="number" step="0.01" min="0" max="100"
                          value={targetInputs[r.assetId] ?? ''}
                          onChange={(e) => setTargetInputs({ ...targetInputs, [r.assetId]: e.target.value })} />
                      ) : (
                        `${formatNumber(r.target)}%`
                      )}
                    </td>
                    <td className={`num ${Math.abs(drift) > 5 ? 'text-red' : ''}`}>{formatPercent(drift)}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>

          {editing && (
            <div className="edit-actions">
              <span className={Math.abs(inputSum - 100) > 0.01 ? 'text-red' : 'text-green'}>
                รวมเป้าหมาย {formatNumber(inputSum)}%{' '}
                {overLimit ? '(เกิน 100% บันทึกไม่ได้)' : Math.abs(inputSum - 100) > 0.01 && '(ควรเท่ากับ 100% ก่อนรีบาลานซ์)'}
              </span>
              <div>
                <button className="btn btn-secondary btn-sm" onClick={() => setEditing(false)} disabled={saving}>ยกเลิก</button>
                <button className="btn btn-primary btn-sm" onClick={handleSave} disabled={saving || overLimit}>
                  {saving ? 'กำลังบันทึก...' : 'บันทึก'}
                </button>
              </div>
            </div>
          )}
          {message && <p className="success-message">{message}</p>}
        </div>
      </div>

      {/* ---------- กราฟแท่งเทียบปัจจุบัน vs เป้าหมาย (ล่างสุด) ---------- */}
      <div className="card">
        <div className="card-header">
          <h2>เทียบสัดส่วนปัจจุบันกับเป้าหมาย</h2>
          <select className="select-sm" value={method} onChange={(e) => setMethod(e.target.value)} title="วิธีคำนวณ (Strategy)">
            {METHODS.map((m) => (
              <option key={m.value} value={m.value}>{m.label}</option>
            ))}
          </select>
        </div>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={comparison}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="symbol" />
            <YAxis unit="%" />
            <Tooltip contentStyle={TOOLTIP_STYLE} formatter={(v) => `${formatNumber(v)}%`} />
            <Legend />
            <Bar dataKey="current" name="สัดส่วนปัจจุบัน" fill="#3b82f6" />
            <Bar dataKey="target" name="เป้าหมาย" fill="#f59e0b" />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}

export default AllocationTab
