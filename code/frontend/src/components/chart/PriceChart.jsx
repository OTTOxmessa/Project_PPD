import { useEffect, useRef } from 'react'
import { createChart, CrosshairMode, LineStyle } from 'lightweight-charts'
import { heikinAshi, macd, rsi, sma } from '../../utils/indicators.js'

const THEME = {
  bg: '#131722',
  text: '#b2b5be',
  grid: '#1f2330',
  border: '#2a2e39',
  up: '#26a69a',
  down: '#ef5350',
}

export const MA_CONFIG = [
  { key: 'ma20', label: 'MA20', period: 20, color: '#e5e7eb', style: LineStyle.Solid },
  { key: 'ma50', label: 'MA50', period: 50, color: '#f472b6', style: LineStyle.Dotted },
  { key: 'ma200', label: 'MA200', period: 200, color: '#f5b83d', style: LineStyle.Solid },
]

function baseOptions(height, width) {
  return {
    width,
    height,
    layout: { background: { type: 'solid', color: THEME.bg }, textColor: THEME.text, fontSize: 11 },
    grid: { vertLines: { color: THEME.grid }, horzLines: { color: THEME.grid } },
    rightPriceScale: { borderColor: THEME.border, minimumWidth: 72 },
    timeScale: { borderColor: THEME.border },
    crosshair: { mode: CrosshairMode.Normal },
  }
}

// เติม whitespace ({time} ไม่มี value) ช่วงที่ตัวชี้วัดยังคำนวณไม่ได้ ให้จำนวนแท่งเท่ากราฟหลัก
// จำเป็นสำหรับการ sync การเลื่อน/ซูมระหว่าง 3 กราฟด้วย logical index
function withValues(times, values) {
  return times.map((time, i) => (values[i] === null ? { time } : { time, value: values[i] }))
}

// กราฟ 3 ชั้นแบบกระดานเทรด: แท่งเทียน + MA + แนวรับ-แนวต้าน / RSI(14) / MACD(12,26,9)
function PriceChart({ bars, chartType, overlays, levels, visibleFrom }) {
  const mainRef = useRef(null)
  const rsiRef = useRef(null)
  const macdRef = useRef(null)

  useEffect(() => {
    if (!bars.length || !mainRef.current) return undefined

    const width = mainRef.current.clientWidth
    const main = createChart(mainRef.current, baseOptions(380, width))
    const rsiChart = createChart(rsiRef.current, baseOptions(130, width))
    const macdChart = createChart(macdRef.current, baseOptions(130, width))
    const charts = [main, rsiChart, macdChart]

    const times = bars.map((b) => b.time)
    const closes = bars.map((b) => b.close)
    const display = chartType === 'ha' ? heikinAshi(bars) : bars

    // --- กราฟหลัก ---
    const candles = main.addCandlestickSeries({
      upColor: THEME.up,
      downColor: THEME.down,
      borderVisible: false,
      wickUpColor: THEME.up,
      wickDownColor: THEME.down,
    })
    candles.setData(display.map((b) => ({ time: b.time, open: b.open, high: b.high, low: b.low, close: b.close })))

    MA_CONFIG.forEach(({ key, period, color, style }) => {
      if (!overlays[key]) return
      const line = main.addLineSeries({
        color,
        lineWidth: 1,
        lineStyle: style,
        priceLineVisible: false,
        lastValueVisible: false,
        crosshairMarkerVisible: false,
      })
      line.setData(withValues(times, sma(closes, period))) // MA คำนวณจากราคาจริงเสมอ ไม่ใช่ Heikin Ashi
    })

    if (levels) {
      candles.createPriceLine({
        price: Number(levels.resistance), color: THEME.down, lineWidth: 1,
        lineStyle: LineStyle.Dashed, axisLabelVisible: true, title: 'แนวต้าน',
      })
      candles.createPriceLine({
        price: Number(levels.pivot), color: '#64748b', lineWidth: 1,
        lineStyle: LineStyle.Dotted, axisLabelVisible: false, title: 'Pivot',
      })
      candles.createPriceLine({
        price: Number(levels.support), color: THEME.up, lineWidth: 1,
        lineStyle: LineStyle.Dashed, axisLabelVisible: true, title: 'แนวรับ',
      })
    }

    // --- RSI ---
    const rsiSeries = rsiChart.addLineSeries({ color: '#e879f9', lineWidth: 1, priceLineVisible: false })
    rsiSeries.setData(withValues(times, rsi(closes, 14)))
    rsiSeries.createPriceLine({ price: 70, color: THEME.down, lineWidth: 1, lineStyle: LineStyle.Solid, axisLabelVisible: true, title: '' })
    rsiSeries.createPriceLine({ price: 30, color: THEME.up, lineWidth: 1, lineStyle: LineStyle.Solid, axisLabelVisible: true, title: '' })

    // --- MACD ---
    const { macdLine, signalLine, histogram } = macd(closes)
    const histSeries = macdChart.addHistogramSeries({ priceLineVisible: false, lastValueVisible: false })
    histSeries.setData(times.map((time, i) => (histogram[i] === null
      ? { time }
      : { time, value: histogram[i], color: histogram[i] >= 0 ? 'rgba(38,166,154,0.55)' : 'rgba(239,83,80,0.55)' })))
    macdChart.addLineSeries({ color: '#38bdf8', lineWidth: 1, priceLineVisible: false })
      .setData(withValues(times, macdLine))
    macdChart.addLineSeries({ color: '#f59e0b', lineWidth: 1, priceLineVisible: false })
      .setData(withValues(times, signalLine))

    // --- ช่วงเวลาที่แสดง (Range) + sync การเลื่อน/ซูม 3 กราฟ ---
    const from = visibleFrom && visibleFrom > times[0] ? visibleFrom : times[0]
    main.timeScale().setVisibleRange({ from, to: times[times.length - 1] })

    let syncing = false
    const unsubscribers = charts.map((source) => {
      const handler = (range) => {
        if (syncing || !range) return
        syncing = true
        charts.forEach((c) => {
          if (c !== source) c.timeScale().setVisibleLogicalRange(range)
        })
        syncing = false
      }
      source.timeScale().subscribeVisibleLogicalRangeChange(handler)
      return () => source.timeScale().unsubscribeVisibleLogicalRangeChange(handler)
    })
    const initialRange = main.timeScale().getVisibleLogicalRange()
    if (initialRange) {
      rsiChart.timeScale().setVisibleLogicalRange(initialRange)
      macdChart.timeScale().setVisibleLogicalRange(initialRange)
    }

    const resizeObserver = new ResizeObserver((entries) => {
      const w = entries[0].contentRect.width
      charts.forEach((c) => c.applyOptions({ width: w }))
    })
    resizeObserver.observe(mainRef.current)

    return () => {
      resizeObserver.disconnect()
      unsubscribers.forEach((unsubscribe) => unsubscribe())
      charts.forEach((c) => c.remove())
    }
  }, [bars, chartType, overlays, levels, visibleFrom])

  return (
    <div className="price-chart">
      <div ref={mainRef} className="chart-pane" />
      <div className="chart-pane-label">RSI (14)</div>
      <div ref={rsiRef} className="chart-pane" />
      <div className="chart-pane-label">MACD (12, 26, 9)</div>
      <div ref={macdRef} className="chart-pane" />
    </div>
  )
}

export default PriceChart
