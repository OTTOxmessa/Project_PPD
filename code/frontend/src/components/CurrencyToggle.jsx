import { useCurrency } from '../context/CurrencyContext.jsx'
import { formatNumber } from '../utils/format.js'

// ปุ่มสลับสกุลเงินที่แสดงบน navbar (ดูอย่างเดียว ไม่เปลี่ยนข้อมูลในระบบ)
function CurrencyToggle() {
  const { requested, setCurrency, fx, fxError } = useCurrency()

  let note = null
  if (requested === 'THB') {
    if (fx) {
      note = <span className="muted small" title={`อัตราปิดจาก Yahoo Finance ณ ${fx.asOf}`}>1 USD = {formatNumber(fx.rate)} THB</span>
    } else if (fxError) {
      note = <span className="text-red small">{fxError}</span>
    } else {
      note = <span className="muted small">กำลังโหลดอัตรา...</span>
    }
  }

  return (
    <div className="currency-toggle" title="แสดงมูลค่าเป็นสกุลเงินที่เลือก ข้อมูลและการคำนวณในระบบยังเป็น USD">
      <span className="toolbar-label">แสดงเป็น</span>
      {['USD', 'THB'].map((code) => (
        <button key={code} type="button" className={`chip ${requested === code ? 'active' : ''}`} onClick={() => setCurrency(code)}>
          {code}
        </button>
      ))}
      {note}
    </div>
  )
}

export default CurrencyToggle
