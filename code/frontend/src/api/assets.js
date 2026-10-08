import apiClient from './client.js'

// หา หรือ สร้างสินทรัพย์จากผลการค้นหา แล้วคืน assetId
// PUT /assets/by-symbol/{symbol} เป็น idempotent — เรียกซ้ำได้ ไม่สร้างซ้ำ
// referencePrice: ราคาที่ผู้ใช้กรอก ใช้เป็นราคาล่าสุดของข้อมูลราคาจำลอง (เฉพาะหุ้นที่ยังไม่มีราคา)
export async function ensureAsset(suggestion, referencePrice = null) {
  const res = await apiClient.put(`/assets/by-symbol/${encodeURIComponent(suggestion.symbol)}`, {
    name: suggestion.name,
    assetType: suggestion.assetType,
    exchange: suggestion.exchange,
    referencePrice: referencePrice ? Number(referencePrice) : null,
  })
  return res.data.id
}
