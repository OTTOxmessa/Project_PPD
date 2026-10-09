import { useEffect, useMemo, useState } from 'react'
import apiClient from '../../api/client.js'
import { ensureAsset } from '../../api/assets.js'
import { apiError, daysAgo, formatNumber, formatPercent, today } from '../../utils/format.js'
import { toWeekly } from '../../utils/indicators.js'
import PriceChart, { MA_CONFIG } from '../chart/PriceChart.jsx'
import SymbolSearch from '../SymbolSearch.jsx'
import '../../source-tag.css'

const RANGES = [
  { key: '1M', days: 30 },
  { key: '3M', days: 90 },
  { key: '6M', days: 180 },
  { key: '1Y', days: 365 },
  { key: 'ALL', days: null },
]

// คอลัมน์กลาง: กระดานเทรด
function TradingBoard({ assetId, onSelectAsset, watchlistIds, onWatchlistChanged }) {
  const [quote, setQuote] = useState(null)
  const [bars, setBars] = useState([])
  const [chartType, setChartType] = useState('candle')
  const [range, setRange] = useState('6M')
  const [timeframe, setTimeframe] = useState('day')
  const [overlays, setOverlays] = useState({ ma20: true, ma50: true, ma200: true })
  const [showLevels, setShowLevels] = useState(true)
  const [levelMethod, setLevelMethod] = useState('pivot')
  const [levels, setLevels] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  // โหลดราคาทั้งหมดครั้งเดียวต่อหุ้น (MA200 ต้องใช้ข้อมูลย้อนหลังมากกว่าช่วงที่แสดง)
  useEffect(() => {
    if (!assetId) return undefined
    let cancelled = false
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const [quoteRes, pricesRes] = await Promise.all([
          apiClient.get(`/assets/${assetId}/quote`),
          apiClient.get(`/assets/${assetId}/prices`, { params: { from: '2000-01-01', to: today() } }),
        ])
        if (cancelled) return
        setQuote(quoteRes.data)
        setBars(pricesRes.data.map((p) => ({
          time: p.date,
          open: Number(p.open),
          high: Number(p.high),
          low: Number(p.low),
          close: Number(p.close),
          volume: Number(p.volume || 0),
        })))
      } catch (err) {
        if (!cancelled) setError(apiError(err, 'โหลดข้อมูลราคาไม่สำเร็จ'))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [assetId])

  const rangeFrom = useMemo(() => {
    const r = RANGES.find((x) => x.key === range)
    if (!r.days) return bars[0]?.time ?? daysAgo(3650)
    return daysAgo(r.days)
  }, [range, bars])

  // แนวรับ-แนวต้าน คำนวณจากช่วงที่เลือก (ใช้ API เดียวกับแท็บในหน้าพอร์ต)
  useEffect(() => {
    if (!assetId || !showLevels) {
      setLevels(null)
      return undefined
    }
    let cancelled = false
    apiClient
      .get(`/assets/${assetId}/support-resistance`, { params: { method: levelMethod, from: rangeFrom, to: today() } })
      .then((res) => !cancelled && setLevels(res.data))
      .catch(() => !cancelled && setLevels(null))
    return () => {
      cancelled = true
    }
  }, [assetId, showLevels, levelMethod, rangeFrom])

  const displayBars = useMemo(() => (timeframe === 'week' ? toWeekly(bars) : bars), [bars, timeframe])

  const handleSearchSelect = async (suggestion) => {
    try {
      onSelectAsset(await ensureAsset(suggestion))
    } catch (err) {
      setError(apiError(err, 'เปิดหุ้นนี้ไม่สำเร็จ'))
    }
  }

  const handleAddWatchlist = async () => {
    try {
      await apiClient.post('/watchlist', { assetId })
      onWatchlistChanged()
    } catch (err) {
      setError(apiError(err, 'เพิ่มใน Watchlist ไม่สำเร็จ'))
    }
  }

  const inWatchlist = watchlistIds.includes(assetId)
  const change = quote?.changePercent === null || quote?.changePercent === undefined ? null : Number(quote.changePercent)
  const tone = change === null ? '' : change >= 0 ? 'text-green' : 'text-red'
  const isRealPrice = quote?.priceSource === 'YAHOO'

  return (
    <div className="panel board">
      <div className="board-search">
        <SymbolSearch onSelect={handleSearchSelect} placeholder="ค้นหาหุ้นเพื่อเปิดกราฟ เช่น MSFT, NVDA, VOO" />
      </div>

      {!assetId ? (
        <p className="empty-state">เลือกหุ้นจาก Watchlist หรือค้นหาด้านบนเพื่อดูกราฟ</p>
      ) : (
        <>
          <div className="board-header">
            <div>
              <div className="board-title">
                <span className="board-symbol">{quote?.symbol}</span>
                <span className="board-name">{quote?.name}</span>
              </div>
              {!inWatchlist && (
                <button className="btn btn-secondary btn-sm" onClick={handleAddWatchlist}>☆ เพิ่มใน Watchlist</button>
              )}
            </div>
            <div className="board-price">
              <div className="muted small">
                ราคาปิด ณ {quote?.asOf ?? '-'}{' '}
                {isRealPrice
                  ? <span className="source-tag real">Yahoo Finance</span>
                  : <span className="source-tag" title="เชื่อมต่อแหล่งราคาจริงไม่ได้ หรือไม่มีข้อมูลหุ้นนี้">ข้อมูลจำลอง</span>}
              </div>
              <div className="board-last">{formatNumber(quote?.price)}</div>
              <div className={tone}>
                {change === null ? '-' : `${change >= 0 ? '▲' : '▼'} ${formatNumber(Math.abs(Number(quote.change)))} (${formatPercent(change)}) วันล่าสุด`}
              </div>
            </div>
          </div>

          <div className="board-toolbar">
            <div className="toolbar-group">
              <span className="toolbar-label">รูปแบบกราฟ:</span>
              <button className={`chip ${chartType === 'candle' ? 'active' : ''}`} onClick={() => setChartType('candle')}>แท่งเทียน</button>
              <button className={`chip ${chartType === 'ha' ? 'active' : ''}`} onClick={() => setChartType('ha')}>Heikin Ashi</button>
            </div>
            <div className="toolbar-group">
              <span className="toolbar-label">ระยะ:</span>
              {RANGES.map((r) => (
                <button key={r.key} className={`chip ${range === r.key ? 'active' : ''}`} onClick={() => setRange(r.key)}>{r.key}</button>
              ))}
            </div>
            <div className="toolbar-group">
              <span className="toolbar-label">TF:</span>
              <button className={`chip ${timeframe === 'day' ? 'active' : ''}`} onClick={() => setTimeframe('day')}>day</button>
              <button className={`chip ${timeframe === 'week' ? 'active' : ''}`} onClick={() => setTimeframe('week')}>week</button>
            </div>
            <div className="toolbar-group">
              {MA_CONFIG.map((m) => (
                <label key={m.key} className="toggle">
                  <input type="checkbox" checked={overlays[m.key]}
                    onChange={(e) => setOverlays({ ...overlays, [m.key]: e.target.checked })} />
                  <span style={{ color: m.color }}>{m.label}</span>
                </label>
              ))}
            </div>
            <div className="toolbar-group">
              <label className="toggle">
                <input type="checkbox" checked={showLevels} onChange={(e) => setShowLevels(e.target.checked)} />
                <span>แนวรับ-แนวต้าน</span>
              </label>
              <select className="select-sm" value={levelMethod} onChange={(e) => setLevelMethod(e.target.value)} disabled={!showLevels}>
                <option value="pivot">Pivot</option>
                <option value="ma">MA Band</option>
              </select>
            </div>
          </div>

          {error && <p className="error-message">{error}</p>}

          {loading ? (
            <p className="muted">กำลังโหลดกราฟ...</p>
          ) : bars.length === 0 ? (
            <p className="empty-state">ยังไม่มีข้อมูลราคาของหุ้นนี้</p>
          ) : (
            <>
              <PriceChart
                bars={displayBars}
                chartType={chartType}
                overlays={overlays}
                levels={showLevels ? levels : null}
                visibleFrom={rangeFrom}
              />
              {showLevels && levels && (
                <div className="levels-bar">
                  <span>แนวต้าน <strong className="text-red">{formatNumber(levels.resistance)}</strong></span>
                  <span>Pivot <strong>{formatNumber(levels.pivot)}</strong></span>
                  <span>แนวรับ <strong className="text-green">{formatNumber(levels.support)}</strong></span>
                </div>
              )}
            </>
          )}
        </>
      )}
    </div>
  )
}

export default TradingBoard
