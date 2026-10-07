import { useState } from 'react'
import PortfolioSidebar from '../components/dashboard/PortfolioSidebar.jsx'
import TradingBoard from '../components/dashboard/TradingBoard.jsx'
import Watchlist from '../components/dashboard/Watchlist.jsx'

// หน้าหลัก 3 คอลัมน์: พอร์ตทั้งหมด | กระดานเทรด | Watchlist
function DashboardPage() {
  const [selectedAssetId, setSelectedAssetId] = useState(null)
  const [watchlistIds, setWatchlistIds] = useState([])
  const [watchlistVersion, setWatchlistVersion] = useState(0)

  const handleWatchlistLoaded = (items) => {
    setWatchlistIds(items.map((q) => q.assetId))
    // เปิดหน้ามาครั้งแรก เลือกหุ้นตัวแรกใน watchlist ให้อัตโนมัติ
    setSelectedAssetId((current) => current ?? items[0]?.assetId ?? null)
  }

  return (
    <div className="dashboard">
      <aside className="dash-left">
        <PortfolioSidebar />
      </aside>
      <section className="dash-center">
        <TradingBoard
          assetId={selectedAssetId}
          onSelectAsset={setSelectedAssetId}
          watchlistIds={watchlistIds}
          onWatchlistChanged={() => setWatchlistVersion((v) => v + 1)}
        />
      </section>
      <aside className="dash-right">
        <Watchlist
          selectedAssetId={selectedAssetId}
          onSelect={setSelectedAssetId}
          refreshKey={watchlistVersion}
          onLoaded={handleWatchlistLoaded}
        />
      </aside>
    </div>
  )
}

export default DashboardPage
