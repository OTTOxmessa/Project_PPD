import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import apiClient from '../api/client.js'
import { useAuth } from './AuthContext.jsx'
import { formatNumber } from '../utils/format.js'

// สกุลเงินที่ใช้ "แสดงผล" เท่านั้น: ข้อมูลจาก API และการคำนวณทั้งหมดเป็น USD
// เลือก THB แล้วหน้าเว็บคูณด้วยอัตรา USD/THB ล่าสุดจาก Yahoo ตอนแสดงผล ช่องกรอกราคายังเป็น USD เสมอ
const CurrencyContext = createContext(null)
const STORAGE_KEY = 'displayCurrency'
const REFRESH_MS = 60 * 60 * 1000 // ขออัตราใหม่ทุก 1 ชั่วโมง (backend จำค่าไว้ 1 ชั่วโมงเช่นกัน)

function readStoredChoice() {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'THB' ? 'THB' : 'USD'
  } catch {
    return 'USD'
  }
}

export function CurrencyProvider({ children }) {
  const { isAuthenticated } = useAuth()
  const [requested, setRequested] = useState(readStoredChoice)
  const [fx, setFx] = useState(null) // { rate, asOf }
  const [fxError, setFxError] = useState(null)

  const setCurrency = useCallback((code) => {
    setRequested(code)
    try {
      localStorage.setItem(STORAGE_KEY, code)
    } catch {
      // เบราว์เซอร์ไม่ให้เก็บค่า -> เลือกได้เฉพาะรอบนี้
    }
  }, [])

  useEffect(() => {
    if (!isAuthenticated || requested !== 'THB') return undefined
    let cancelled = false
    const load = () => {
      apiClient.get('/exchange-rates/usd-thb')
        .then((res) => {
          if (cancelled) return
          setFx({ rate: Number(res.data.rate), asOf: res.data.asOf })
          setFxError(null)
        })
        .catch(() => {
          if (!cancelled) setFxError('ดึงอัตราแลกเปลี่ยนไม่ได้ ตอนนี้แสดงเป็น USD')
        })
    }
    load()
    const timer = setInterval(load, REFRESH_MS)
    return () => {
      cancelled = true
      clearInterval(timer)
    }
  }, [isAuthenticated, requested])

  // ถ้าเลือก THB แต่ยังไม่มีอัตรา (กำลังโหลด/ดึงไม่ได้) ให้แสดง USD ไปก่อน ตัวเลขกับหน่วยจะได้ตรงกันเสมอ
  const currency = requested === 'THB' && fx ? 'THB' : 'USD'
  const factor = currency === 'THB' ? fx.rate : 1

  const convert = useCallback((value) => {
    if (value === null || value === undefined || value === '' || Number.isNaN(Number(value))) return value
    return Number(value) * factor
  }, [factor])

  const money = useCallback((value, digits = 2) => formatNumber(convert(value), digits), [convert])

  const value = useMemo(
    () => ({ currency, requested, setCurrency, fx, fxError, convert, money }),
    [currency, requested, setCurrency, fx, fxError, convert, money],
  )

  return <CurrencyContext.Provider value={value}>{children}</CurrencyContext.Provider>
}

export function useCurrency() {
  const context = useContext(CurrencyContext)
  if (!context) {
    throw new Error('useCurrency ต้องถูกเรียกใน CurrencyProvider เท่านั้น')
  }
  return context
}
