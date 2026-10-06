import { useState } from 'react'
import { Bar, BarChart, CartesianGrid, Cell, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import apiClient from '../../api/client.js'
import { apiError, daysAgo, formatPercent, today } from '../../utils/format.js'

function PerformanceTab({ portfolioId }) {
  const [benchmark, setBenchmark] = useState('SET')
  const [from, setFrom] = useState(daysAgo(90))
  const [to, setTo] = useState(today())
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const res = await apiClient.get(`/portfolios/${portfolioId}/performance`, { params: { benchmark, from, to } })
      setReport(res.data)
    } catch (err) {
      setReport(null)
      setError(apiError(err, 'คำนวณผลตอบแทนไม่สำเร็จ'))
    } finally {
      setLoading(false)
    }
  }

  const chartData = report
    ? [
        { name: 'พอร์ตของฉัน', value: Number(report.portfolioReturnPercent) },
        { name: `ตลาด (${report.benchmarkCode})`, value: Number(report.benchmarkReturnPercent) },
      ]
    : []

  const outperform = report ? Number(report.outperformancePercent) : 0

  return (
    <div className="card">
      <form className="form-row" onSubmit={handleSubmit}>
        <div className="form-group">
          <label>ดัชนีอ้างอิง</label>
          <select value={benchmark} onChange={(e) => setBenchmark(e.target.value)}>
            <option value="SET">SET Index</option>
          </select>
        </div>
        <div className="form-group">
          <label>จากวันที่</label>
          <input type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} required />
        </div>
        <div className="form-group">
          <label>ถึงวันที่</label>
          <input type="date" value={to} max={today()} onChange={(e) => setTo(e.target.value)} required />
        </div>
        <div className="form-group form-group-button">
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'กำลังคำนวณ...' : 'เปรียบเทียบ'}
          </button>
        </div>
      </form>

      {error && <p className="error-message">{error}</p>}

      {report && (
        <>
          <div className="summary-grid">
            <div className="stat">
              <span className="stat-label">ผลตอบแทนพอร์ต</span>
              <span className="stat-value">{formatPercent(report.portfolioReturnPercent)}</span>
            </div>
            <div className="stat">
              <span className="stat-label">ผลตอบแทนตลาด</span>
              <span className="stat-value">{formatPercent(report.benchmarkReturnPercent)}</span>
            </div>
            <div className="stat">
              <span className="stat-label">{outperform >= 0 ? 'ชนะตลาด' : 'แพ้ตลาด'}</span>
              <span className={`stat-value ${outperform >= 0 ? 'text-green' : 'text-red'}`}>
                {formatPercent(report.outperformancePercent)}
              </span>
            </div>
          </div>

          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis unit="%" />
              <Tooltip formatter={(v) => formatPercent(v)} />
              <ReferenceLine y={0} stroke="#94a3b8" />
              <Bar dataKey="value" name="ผลตอบแทน">
                {chartData.map((d, i) => (
                  <Cell key={i} fill={i === 0 ? '#2563eb' : '#94a3b8'} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>

          {Number(report.portfolioReturnPercent) === 0 && (
            <p className="hint">
              ผลตอบแทนพอร์ตเป็น 0% อาจเพราะยังไม่มีการซื้อก่อนวันเริ่มต้นที่เลือก — ลองเลือกวันเริ่มต้นหลังวันที่ซื้อครั้งแรก
            </p>
          )}
        </>
      )}
    </div>
  )
}

export default PerformanceTab
