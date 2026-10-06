import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import apiClient from '../api/client.js'
import { apiError } from '../utils/format.js'
import HoldingsTab from '../components/portfolio/HoldingsTab.jsx'
import AllocationTab from '../components/portfolio/AllocationTab.jsx'
import SupportResistanceTab from '../components/portfolio/SupportResistanceTab.jsx'
import AlertsTab from '../components/portfolio/AlertsTab.jsx'
import PerformanceTab from '../components/portfolio/PerformanceTab.jsx'
import RebalanceTab from '../components/portfolio/RebalanceTab.jsx'

const TABS = [
  { key: 'holdings', label: 'สินทรัพย์ที่ถือ' },
  { key: 'allocation', label: 'สัดส่วนสินทรัพย์' },
  { key: 'analysis', label: 'แนวรับ-แนวต้าน' },
  { key: 'alerts', label: 'แจ้งเตือนราคา' },
  { key: 'performance', label: 'เทียบกับตลาด' },
  { key: 'rebalance', label: 'รีบาลานซ์' },
]

function PortfolioDetailPage() {
  const { portfolioId } = useParams()
  const navigate = useNavigate()
  const [portfolio, setPortfolio] = useState(null)
  const [assets, setAssets] = useState([])
  const [activeTab, setActiveTab] = useState('holdings')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const loadAssets = useCallback(async () => {
    const res = await apiClient.get('/assets')
    setAssets([...res.data].sort((a, b) => a.symbol.localeCompare(b.symbol)))
  }, [])

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const res = await apiClient.get(`/portfolios/${portfolioId}`)
        setPortfolio(res.data)
        await loadAssets()
      } catch (err) {
        setError(apiError(err, 'โหลดข้อมูลพอร์ตไม่สำเร็จ'))
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [portfolioId, loadAssets])

  const handleDelete = async () => {
    if (!window.confirm(`ลบพอร์ต "${portfolio.name}" และข้อมูลทั้งหมดในพอร์ตนี้ถาวร?`)) return
    try {
      await apiClient.delete(`/portfolios/${portfolioId}`)
      navigate('/')
    } catch (err) {
      setError(apiError(err, 'ลบพอร์ตไม่สำเร็จ'))
    }
  }

  if (loading) return <p className="muted">กำลังโหลด...</p>
  if (error && !portfolio) {
    return (
      <div className="page-narrow">
        <p className="error-message">{error}</p>
        <Link to="/">← กลับหน้าหลัก</Link>
      </div>
    )
  }

  const tabProps = { portfolioId, assets, onAssetsChanged: loadAssets }

  return (
    <div className="page-narrow">
      <div className="page-header">
        <div>
          <Link to="/" className="muted">← หน้าหลัก</Link>
          <h1>{portfolio.name}</h1>
          <p className="muted">สกุลเงินหลัก: {portfolio.baseCurrency}</p>
        </div>
        <button className="btn btn-danger" onClick={handleDelete}>ลบพอร์ต</button>
      </div>

      {error && <p className="error-message">{error}</p>}

      <div className="tabs">
        {TABS.map((tab) => (
          <button key={tab.key} className={`tab ${activeTab === tab.key ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.key)}>
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'holdings' && <HoldingsTab {...tabProps} />}
      {activeTab === 'allocation' && <AllocationTab {...tabProps} />}
      {activeTab === 'analysis' && <SupportResistanceTab {...tabProps} />}
      {activeTab === 'alerts' && <AlertsTab {...tabProps} />}
      {activeTab === 'performance' && <PerformanceTab {...tabProps} />}
      {activeTab === 'rebalance' && <RebalanceTab {...tabProps} />}
    </div>
  )
}

export default PortfolioDetailPage
