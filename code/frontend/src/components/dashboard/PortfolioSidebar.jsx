import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient from '../../api/client.js'
import { apiError, formatPercent } from '../../utils/format.js'
import { useCurrency } from '../../context/CurrencyContext.jsx'

// คอลัมน์ซ้าย: พอร์ตหุ้นทั้งหมด พร้อมมูลค่าและกำไร/ขาดทุน
function PortfolioSidebar() {
  const { currency: shownCurrency, money } = useCurrency()
  const navigate = useNavigate()
  const [portfolios, setPortfolios] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [name, setName] = useState('')
  const [creating, setCreating] = useState(false)

  const load = async () => {
    setLoading(true)
    try {
      const res = await apiClient.get('/portfolios/summary')
      setPortfolios(res.data)
    } catch (err) {
      setError(apiError(err, 'โหลดพอร์ตไม่สำเร็จ'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const handleCreate = async (e) => {
    e.preventDefault()
    setCreating(true)
    setError(null)
    try {
      // ระบบรองรับเฉพาะหุ้นสหรัฐฯ พอร์ตทุกพอร์ตจึงเป็น USD
      const res = await apiClient.post('/portfolios', { name, baseCurrency: 'USD' })
      navigate(`/portfolios/${res.data.id}`)
    } catch (err) {
      setError(apiError(err, 'สร้างพอร์ตไม่สำเร็จ'))
      setCreating(false)
    }
  }

  // รวมมูลค่าแยกตามสกุลเงิน (ตอนนี้มีแค่ USD แต่เผื่อพอร์ตสกุลอื่นในอนาคต จะไม่ถูกบวกปนกัน)
  const totalsByCurrency = portfolios.reduce((acc, p) => {
    const t = acc[p.baseCurrency] || { value: 0, gain: 0 }
    t.value += Number(p.marketValue)
    t.gain += Number(p.gain)
    acc[p.baseCurrency] = t
    return acc
  }, {})

  return (
    <div className="panel">
      <div className="panel-header">
        <h2>พอร์ตหุ้นทั้งหมด</h2>
        <button className="icon-btn" title="สร้างพอร์ตใหม่" onClick={() => setShowForm((v) => !v)}>
          {showForm ? '×' : '+'}
        </button>
      </div>

      {Object.entries(totalsByCurrency).map(([currency, t]) => (
        <div key={currency} className="total-row">
          <span className="muted">มูลค่ารวม ({shownCurrency})</span>
          <strong>{money(t.value)}</strong>
          <span className={t.gain >= 0 ? 'text-green' : 'text-red'}>
            {t.gain >= 0 ? '+' : ''}{money(t.gain)}
          </span>
        </div>
      ))}

      {showForm && (
        <form className="mini-form" onSubmit={handleCreate}>
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="ชื่อพอร์ต" required />
          <span className="muted small" title="ระบบรองรับเฉพาะหุ้นและ ETF ตลาดสหรัฐฯ">USD</span>
          <button type="submit" className="btn btn-primary btn-sm" disabled={creating}>
            {creating ? '...' : 'สร้าง'}
          </button>
        </form>
      )}

      {error && <p className="error-message">{error}</p>}

      {loading ? (
        <p className="muted">กำลังโหลด...</p>
      ) : portfolios.length === 0 ? (
        <p className="empty-state">ยังไม่มีพอร์ต กด + เพื่อสร้าง</p>
      ) : (
        <div className="portfolio-cards">
          {portfolios.map((p) => {
            const positive = Number(p.gain) >= 0
            return (
              <button key={p.id} className="portfolio-card" onClick={() => navigate(`/portfolios/${p.id}`)}>
                <div className="portfolio-card-top">
                  <span className="portfolio-card-name">{p.name}</span>
                  <span className={`gain-label ${positive ? 'text-green' : 'text-red'}`}>
                    {positive ? 'กำไร' : 'ขาดทุน'}
                  </span>
                </div>
                <div className="portfolio-card-bottom">
                  <div>
                    <div className="muted small">มูลค่า ({shownCurrency})</div>
                    <div className="portfolio-card-value">{money(p.marketValue)}</div>
                  </div>
                  <div className={`portfolio-card-gain ${positive ? 'text-green' : 'text-red'}`}>
                    <div>({formatPercent(p.gainPercent)})</div>
                    <div>{positive ? '+' : ''}{money(p.gain)}</div>
                  </div>
                </div>
                <div className="muted small">{p.holdingsCount} สินทรัพย์</div>
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default PortfolioSidebar
