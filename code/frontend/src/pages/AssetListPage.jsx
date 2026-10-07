import { useEffect, useState } from 'react'
import apiClient from '../api/client.js'
import { apiError } from '../utils/format.js'
import SymbolSearch from '../components/SymbolSearch.jsx'

const ASSET_TYPES = ['STOCK', 'ETF', 'BOND', 'MUTUAL_FUND', 'CRYPTO', 'CASH']
const EMPTY_FORM = { symbol: '', name: '', assetType: 'STOCK', exchange: '' }

function AssetListPage() {
  const [assets, setAssets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [searchKey, setSearchKey] = useState(0)

  const load = async () => {
    setLoading(true)
    try {
      const res = await apiClient.get('/assets')
      setAssets([...res.data].sort((a, b) => a.symbol.localeCompare(b.symbol)))
    } catch (err) {
      setError(apiError(err, 'โหลดรายการสินทรัพย์ไม่สำเร็จ'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  // เลือกจาก dropdown -> เติมชื่อ/ประเภท/ตลาดให้อัตโนมัติ (ยังแก้เองได้)
  const handleSelect = (s) => {
    setForm({ symbol: s.symbol, name: s.name, assetType: s.assetType, exchange: s.exchange || '' })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await apiClient.post('/assets', form)
      setForm(EMPTY_FORM)
      setSearchKey((k) => k + 1)
      await load()
    } catch (err) {
      setError(apiError(err, 'เพิ่มสินทรัพย์ไม่สำเร็จ'))
    } finally {
      setSaving(false)
    }
  }

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  return (
    <div className="page-narrow">
      <h1>สินทรัพย์ทั้งหมด</h1>

      <form className="card" onSubmit={handleSubmit}>
        <h2>เพิ่มสินทรัพย์</h2>
        <div className="form-row">
          <div className="form-group">
            <label>Symbol</label>
            <SymbolSearch
              key={searchKey}
              clearOnSelect={false}
              onSelect={handleSelect}
              onQueryChange={(q) => setForm((f) => ({ ...f, symbol: q }))}
              placeholder="เช่น SCB, AAPL"
            />
          </div>
          <div className="form-group">
            <label htmlFor="name">ชื่อ</label>
            <input id="name" value={form.name} onChange={update('name')} required />
          </div>
          <div className="form-group">
            <label htmlFor="assetType">ประเภท</label>
            <select id="assetType" value={form.assetType} onChange={update('assetType')}>
              {ASSET_TYPES.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label htmlFor="exchange">ตลาด</label>
            <input id="exchange" value={form.exchange} onChange={update('exchange')} placeholder="SET / US" />
          </div>
        </div>
        <button type="submit" className="btn btn-primary" disabled={saving || !form.symbol.trim()}>
          {saving ? 'กำลังบันทึก...' : 'เพิ่มสินทรัพย์'}
        </button>
        <p className="hint">หุ้นที่เพิ่มใหม่จะได้ข้อมูลราคาย้อนหลังแบบจำลอง 1 ปี เพื่อใช้ทดสอบกราฟและฟีเจอร์ต่าง ๆ</p>
      </form>

      {error && <p className="error-message">{error}</p>}

      {loading ? (
        <p className="muted">กำลังโหลด...</p>
      ) : assets.length === 0 ? (
        <p className="empty-state">ยังไม่มีสินทรัพย์ในระบบ</p>
      ) : (
        <div className="card table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Symbol</th>
                <th>ชื่อ</th>
                <th>ประเภท</th>
                <th>ตลาด</th>
              </tr>
            </thead>
            <tbody>
              {assets.map((a) => (
                <tr key={a.id}>
                  <td><strong>{a.symbol}</strong></td>
                  <td>{a.name}</td>
                  <td><span className="badge">{a.assetType}</span></td>
                  <td>{a.exchange || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export default AssetListPage
