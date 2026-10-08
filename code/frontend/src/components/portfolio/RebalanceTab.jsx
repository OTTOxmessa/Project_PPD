import { useCallback, useEffect, useState } from 'react'
import apiClient from '../../api/client.js'
import { apiError, formatDateTime, formatNumber } from '../../utils/format.js'

// แสดงจำนวนหน่วยตามความละเอียดจริงจาก backend (สูงสุด 6 ตำแหน่ง) — หุ้นเป็นจำนวนเต็ม คริปโตมีทศนิยม
// ถ้าปัดเหลือ 4 ตำแหน่ง จำนวน x ราคา จะไม่เท่ากับมูลค่าที่แสดง (เช่น BTC 0.0063 x 5.18 ล้าน ≠ 32,614.62)
function formatQuantity(value) {
  return Number(value).toLocaleString('th-TH', { maximumFractionDigits: 6 })
}

const METHOD_INFO = {
  threshold: 'รีบาลานซ์เฉพาะสินทรัพย์ที่สัดส่วนเบี่ยงจากเป้าหมายเกิน 5%',
  calendar: 'ปรับทุกสินทรัพย์กลับสู่เป้าหมายเต็มจำนวน (ใช้กับการรีบาลานซ์ตามรอบเวลา)',
}

function RebalanceTab({ portfolioId }) {
  const [method, setMethod] = useState('threshold')
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [executing, setExecuting] = useState(false)
  const [error, setError] = useState(null)
  const [previewError, setPreviewError] = useState(null)
  const [result, setResult] = useState(null)

  const loadPreview = useCallback(async () => {
    setLoading(true)
    setPreviewError(null)
    try {
      const res = await apiClient.get(`/portfolios/${portfolioId}/rebalance-plan`, { params: { method } })
      setOrders(res.data)
    } catch (err) {
      // backend ตรวจความพร้อมของพอร์ตตั้งแต่ตอนดูแผน (เช่น เป้ารวมไม่ถึง 100%) — ล้างแผนเก่าแล้วแสดงเหตุผลแทน
      setOrders([])
      setPreviewError(apiError(err, 'คำนวณแผนรีบาลานซ์ไม่สำเร็จ'))
    } finally {
      setLoading(false)
    }
  }, [portfolioId, method])

  useEffect(() => {
    loadPreview()
  }, [loadPreview])

  const handleExecute = async () => {
    if (!window.confirm(`ยืนยันซื้อขาย ${orders.length} รายการตามแผนนี้? รายการจะถูกบันทึกลงประวัติจริง`)) return
    setExecuting(true)
    setError(null)
    try {
      const res = await apiClient.post(`/portfolios/${portfolioId}/rebalances`, null, { params: { method } })
      setResult(res.data)
      await loadPreview()
    } catch (err) {
      setError(apiError(err, 'รีบาลานซ์ไม่สำเร็จ'))
    } finally {
      setExecuting(false)
    }
  }

  const sellValue = orders.filter((o) => o.type === 'SELL')
    .reduce((s, o) => s + Number(o.quantity) * Number(o.estimatedPrice), 0)
  const buyValue = orders.filter((o) => o.type === 'BUY')
    .reduce((s, o) => s + Number(o.quantity) * Number(o.estimatedPrice), 0)
  // ยอดขาย - ยอดซื้อ: บวก = เหลือเงินสด, ลบ = ต้องใช้เงินเพิ่ม (Threshold ปรับเฉพาะบางตัว ยอดจึงไม่เท่ากันเสมอ)
  const netCash = sellValue - buyValue

  return (
    <div>
      <div className="card">
        <div className="form-row">
          <div className="form-group">
            <label>วิธีรีบาลานซ์ (Strategy)</label>
            <select value={method} onChange={(e) => setMethod(e.target.value)}>
              <option value="threshold">Threshold (เบี่ยงเกิน 5%)</option>
              <option value="calendar">Calendar (ปรับเต็มจำนวน)</option>
            </select>
          </div>
        </div>
        <p className="hint">{METHOD_INFO[method]}</p>
        <p className="hint">
          สินทรัพย์ที่ไม่ได้กำหนดเป้าหมาย จะถูกนับว่าเป้าหมายเป็น 0% (ระบบจะเสนอให้ขายทั้งหมด) —
          ตั้งเป้าหมายให้ครบในแท็บ "สัดส่วนสินทรัพย์" ก่อน
        </p>
      </div>

      {previewError && <p className="error-message">{previewError}</p>}
      {error && <p className="error-message">{error}</p>}

      <div className="card table-wrap">
        <h2>แผนการซื้อขาย (Preview)</h2>
        {loading ? (
          <p>กำลังคำนวณ...</p>
        ) : previewError ? (
          <p className="empty-state">ยังแสดงแผนไม่ได้ — แก้ตามข้อความด้านบน แล้วกลับมาที่แท็บนี้อีกครั้ง</p>
        ) : orders.length === 0 ? (
          <p className="empty-state">พอร์ตอยู่ในสัดส่วนเป้าหมายแล้ว ไม่ต้องรีบาลานซ์</p>
        ) : (
          <>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Symbol</th>
                  <th>คำสั่ง</th>
                  <th className="num">จำนวน</th>
                  <th className="num">ราคาประมาณ</th>
                  <th className="num">มูลค่า</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((o) => (
                  <tr key={`${o.assetId}-${o.type}`}>
                    <td><strong>{o.symbol}</strong></td>
                    <td><span className={`badge ${o.type === 'BUY' ? 'badge-green' : 'badge-red'}`}>{o.type}</span></td>
                    <td className="num">{formatQuantity(o.quantity)}</td>
                    <td className="num">{formatNumber(o.estimatedPrice)}</td>
                    <td className="num">{formatNumber(Number(o.quantity) * Number(o.estimatedPrice))}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="muted">ขายรวม {formatNumber(sellValue)} · ซื้อรวม {formatNumber(buyValue)}</p>
            {Math.abs(netCash) >= 0.01 && (
              <p className="hint">
                {netCash > 0
                  ? `ได้เงินสดเหลือจากการรีบาลานซ์ ${formatNumber(netCash)} บาท`
                  : `ต้องใช้เงินเพิ่ม ${formatNumber(-netCash)} บาท`}
                {' '}— ระบบบันทึกเฉพาะรายการซื้อขาย ยังไม่ได้ติดตามยอดเงินสดในพอร์ต
              </p>
            )}
            <button className="btn btn-primary" onClick={handleExecute} disabled={executing}>
              {executing ? 'กำลังดำเนินการ...' : 'ยืนยันรีบาลานซ์'}
            </button>
          </>
        )}
      </div>

      {result && (
        <div className="card">
          <h2>ผลการรีบาลานซ์ล่าสุด</h2>
          <p className="success-message">
            รีบาลานซ์แบบ {result.method} สำเร็จเมื่อ {formatDateTime(result.triggeredAt)} — ดูรายการที่เกิดขึ้นได้ในแท็บ "สินทรัพย์ที่ถือ"
          </p>
        </div>
      )}
    </div>
  )
}

export default RebalanceTab
