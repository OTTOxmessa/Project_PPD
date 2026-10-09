import { useState } from 'react'
import apiClient from '../../api/client.js'
import { apiError, formatDateTime, formatNumber, today } from '../../utils/format.js'
import { useCurrency } from '../../context/CurrencyContext.jsx'

const TYPE_LABEL = { BUY: 'ซื้อ (BUY)', SELL: 'ขาย (SELL)' }

// ประวัติรายการซื้อขาย: ปกติดูอย่างเดียว กดปุ่ม "แก้ไข" มุมขวาบนเพื่อเข้าโหมดแก้ไข แล้วแต่ละแถวจะมีปุ่มแก้ไข/ลบ
// แก้/ลบแล้ว backend คำนวณจำนวนหุ้นและต้นทุนเฉลี่ยใหม่จากประวัติทั้งหมด จึงต้องโหลดตาราง holding ใหม่ด้วย (onChanged)
function TransactionHistory({ portfolioId, transactions, onChanged }) {
  const { currency, money } = useCurrency()
  const [editMode, setEditMode] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [draft, setDraft] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const [error, setError] = useState(null)

  const startEdit = (t) => {
    setError(null)
    setEditingId(t.id)
    setDraft({
      type: t.type,
      quantity: String(t.quantity),
      price: String(t.price),
      executedAt: String(t.executedAt).slice(0, 10),
    })
  }

  const cancelEdit = () => {
    setEditingId(null)
    setDraft(null)
  }

  // ออกจากโหมดแก้ไข: แถวที่แก้ค้างอยู่ถูกยกเลิก (ไม่บันทึก)
  const finishEditMode = () => {
    cancelEdit()
    setError(null)
    setEditMode(false)
  }

  const saveEdit = async (id) => {
    setBusyId(id)
    setError(null)
    try {
      await apiClient.put(`/portfolios/${portfolioId}/transactions/${id}`, {
        type: draft.type,
        quantity: draft.quantity,
        price: draft.price,
        executedAt: draft.executedAt || null,
      })
      cancelEdit()
      await onChanged()
    } catch (err) {
      setError(apiError(err, 'แก้ไขรายการไม่สำเร็จ'))
    } finally {
      setBusyId(null)
    }
  }

  const remove = async (t) => {
    if (!window.confirm(`ลบรายการ ${t.type} ${t.symbol} จำนวน ${formatNumber(t.quantity, 4)} หน่วย?\nจำนวนหุ้นและต้นทุนเฉลี่ยจะคำนวณใหม่`)) return
    setBusyId(t.id)
    setError(null)
    try {
      await apiClient.delete(`/portfolios/${portfolioId}/transactions/${t.id}`)
      if (editingId === t.id) cancelEdit()
      await onChanged()
    } catch (err) {
      setError(apiError(err, 'ลบรายการไม่สำเร็จ'))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="card table-wrap">
      <div className="card-header">
        <h2>ประวัติรายการ</h2>
        {transactions.length > 0 && (
          editMode
            ? <button className="btn btn-primary btn-sm" onClick={finishEditMode}>เสร็จ</button>
            : <button className="btn btn-secondary btn-sm" onClick={() => setEditMode(true)}>แก้ไข</button>
        )}
      </div>
      {error && <p className="error-message">{error}</p>}
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
              <th className="num">ราคา ({editingId ? 'USD' : currency})</th>
              {editMode && <th />}
            </tr>
          </thead>
          <tbody>
            {transactions.map((t) => {
              const busy = busyId === t.id
              if (editingId === t.id && draft) {
                // ตัวเลือกประเภท: ซื้อ/ขาย และประเภทเดิมของรายการ (เผื่อเป็นปันผลจากข้อมูลเก่า)
                const types = Array.from(new Set(['BUY', 'SELL', t.type]))
                return (
                  <tr key={t.id} className="row-editing">
                    <td>
                      <input type="date" className="input-date" max={today()} value={draft.executedAt}
                        onChange={(e) => setDraft({ ...draft, executedAt: e.target.value })} />
                    </td>
                    <td>{t.symbol}</td>
                    <td>
                      <select className="select-sm" value={draft.type} onChange={(e) => setDraft({ ...draft, type: e.target.value })}>
                        {types.map((type) => <option key={type} value={type}>{TYPE_LABEL[type] ?? type}</option>)}
                      </select>
                    </td>
                    <td className="num">
                      <input type="number" step="any" min="0" className="input-small" value={draft.quantity}
                        onChange={(e) => setDraft({ ...draft, quantity: e.target.value })} />
                    </td>
                    <td className="num">
                      <input type="number" step="any" min="0" className="input-small" value={draft.price}
                        title="กรอกเป็น USD" onChange={(e) => setDraft({ ...draft, price: e.target.value })} />
                    </td>
                    <td className="row-actions">
                      <button className="btn btn-primary btn-sm" disabled={busy} onClick={() => saveEdit(t.id)}>
                        {busy ? '...' : 'บันทึก'}
                      </button>
                      <button className="btn btn-secondary btn-sm" disabled={busy} onClick={cancelEdit}>ยกเลิก</button>
                    </td>
                  </tr>
                )
              }
              return (
                <tr key={t.id}>
                  <td>{formatDateTime(t.executedAt)}</td>
                  <td>{t.symbol}</td>
                  <td><span className={`badge ${t.type === 'BUY' ? 'badge-green' : 'badge-red'}`}>{t.type}</span></td>
                  <td className="num">{formatNumber(t.quantity, 4)}</td>
                  <td className="num">{money(t.price)}</td>
                  {editMode && (
                    <td className="row-actions">
                      <button className="btn-text" disabled={busy || editingId !== null} onClick={() => startEdit(t)}>แก้ไข</button>
                      <button className="btn-text-danger" disabled={busy || editingId !== null} onClick={() => remove(t)}>
                        {busy ? '...' : 'ลบ'}
                      </button>
                    </td>
                  )}
                </tr>
              )
            })}
          </tbody>
        </table>
      )}
    </div>
  )
}

export default TransactionHistory
