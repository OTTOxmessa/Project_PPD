import { BrowserRouter, Navigate, NavLink, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import PortfolioDetailPage from './pages/PortfolioDetailPage.jsx'
import AssetListPage from './pages/AssetListPage.jsx'
import './App.css'

function AppNav() {
  const { isAuthenticated, user, logout } = useAuth()
  if (!isAuthenticated) return null

  return (
    <nav className="navbar">
      <span className="brand">Portfolio</span>
      <NavLink to="/" end>กระดานเทรด</NavLink>
      <NavLink to="/assets">สินทรัพย์</NavLink>
      <span className="navbar-spacer" />
      <span className="navbar-user">{user?.username}</span>
      <button className="btn-link" onClick={logout}>ออกจากระบบ</button>
    </nav>
  )
}

function AppRoutes() {
  const location = useLocation()
  // หน้าหลัก 3 คอลัมน์ใช้ความกว้างเต็มจอ หน้าอื่นจำกัดความกว้างให้อ่านง่าย
  const wide = location.pathname === '/'

  return (
    <>
      <AppNav />
      <main className={`content ${wide ? 'content-wide' : ''}`}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
          <Route path="/portfolios/:portfolioId" element={<ProtectedRoute><PortfolioDetailPage /></ProtectedRoute>} />
          <Route path="/assets" element={<ProtectedRoute><AssetListPage /></ProtectedRoute>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  )
}

function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  )
}

export default App
