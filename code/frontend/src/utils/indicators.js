// ตัวชี้วัดทางเทคนิค — คืน array ยาวเท่า input โดยช่วงที่ยังคำนวณไม่ได้เป็น null

export function sma(values, period) {
  const out = new Array(values.length).fill(null)
  let sum = 0
  for (let i = 0; i < values.length; i++) {
    sum += values[i]
    if (i >= period) sum -= values[i - period]
    if (i >= period - 1) out[i] = sum / period
  }
  return out
}

// EMA ที่ข้ามค่า null ช่วงต้นได้ (ใช้ต่อกับ MACD line ที่เริ่มต้นด้วย null)
export function ema(values, period) {
  const out = new Array(values.length).fill(null)
  const k = 2 / (period + 1)
  const seed = []
  let prev = null
  for (let i = 0; i < values.length; i++) {
    const v = values[i]
    if (v === null || v === undefined) continue
    if (prev === null) {
      seed.push(v)
      if (seed.length === period) {
        prev = seed.reduce((a, b) => a + b, 0) / period
        out[i] = prev
      }
    } else {
      prev = v * k + prev * (1 - k)
      out[i] = prev
    }
  }
  return out
}

// RSI แบบ Wilder
export function rsi(closes, period = 14) {
  const out = new Array(closes.length).fill(null)
  if (closes.length <= period) return out
  let gain = 0
  let loss = 0
  for (let i = 1; i <= period; i++) {
    const d = closes[i] - closes[i - 1]
    if (d >= 0) gain += d
    else loss -= d
  }
  let avgGain = gain / period
  let avgLoss = loss / period
  const value = () => (avgLoss === 0 ? 100 : 100 - 100 / (1 + avgGain / avgLoss))
  out[period] = value()
  for (let i = period + 1; i < closes.length; i++) {
    const d = closes[i] - closes[i - 1]
    avgGain = (avgGain * (period - 1) + Math.max(d, 0)) / period
    avgLoss = (avgLoss * (period - 1) + Math.max(-d, 0)) / period
    out[i] = value()
  }
  return out
}

export function macd(closes, fast = 12, slow = 26, signal = 9) {
  const emaFast = ema(closes, fast)
  const emaSlow = ema(closes, slow)
  const macdLine = closes.map((_, i) =>
    emaFast[i] !== null && emaSlow[i] !== null ? emaFast[i] - emaSlow[i] : null)
  const signalLine = ema(macdLine, signal)
  const histogram = macdLine.map((m, i) =>
    m !== null && signalLine[i] !== null ? m - signalLine[i] : null)
  return { macdLine, signalLine, histogram }
}

export function heikinAshi(bars) {
  const out = []
  for (let i = 0; i < bars.length; i++) {
    const b = bars[i]
    const close = (b.open + b.high + b.low + b.close) / 4
    const open = i === 0 ? (b.open + b.close) / 2 : (out[i - 1].open + out[i - 1].close) / 2
    out.push({
      time: b.time,
      open,
      close,
      high: Math.max(b.high, open, close),
      low: Math.min(b.low, open, close),
    })
  }
  return out
}

// วันจันทร์ของสัปดาห์ (ใช้จัดกลุ่มแท่งรายวันเป็นรายสัปดาห์)
function weekKey(dateStr) {
  const d = new Date(`${dateStr}T00:00:00Z`)
  const dayFromMonday = (d.getUTCDay() + 6) % 7
  d.setUTCDate(d.getUTCDate() - dayFromMonday)
  return d.toISOString().slice(0, 10)
}

export function toWeekly(bars) {
  const weeks = []
  let current = null
  let currentKey = null
  for (const b of bars) {
    const key = weekKey(b.time)
    if (key !== currentKey) {
      if (current) weeks.push(current)
      current = { ...b }
      currentKey = key
    } else {
      current.high = Math.max(current.high, b.high)
      current.low = Math.min(current.low, b.low)
      current.close = b.close
      current.volume = (current.volume || 0) + (b.volume || 0)
    }
  }
  if (current) weeks.push(current)
  return weeks
}
