// ฟังก์ชันจัดรูปแบบที่ใช้ร่วมกันทุกหน้า

export function formatNumber(value, digits = 2) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  return Number(value).toLocaleString('th-TH', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  })
}

export function formatPercent(value, digits = 2) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  const n = Number(value)
  const sign = n > 0 ? '+' : ''
  return `${sign}${n.toFixed(digits)}%`
}

export function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('th-TH', { dateStyle: 'medium', timeStyle: 'short' })
}

// YYYY-MM-DD ตามเวลาท้องถิ่น (toISOString ใช้ UTC ทำให้วันที่เลื่อนช่วงเช้ามืดในไทย)
export function toISODate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function daysAgo(n) {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return toISODate(d)
}

export function today() {
  return toISODate(new Date())
}

// ดึงข้อความ error จาก ErrorResponse ของ GlobalExceptionHandler ฝั่ง backend
export function apiError(err, fallback = 'เกิดข้อผิดพลาด') {
  const data = err?.response?.data
  if (data?.details?.length) return `${data.message}: ${data.details.join(', ')}`
  return data?.message || err?.message || fallback
}
