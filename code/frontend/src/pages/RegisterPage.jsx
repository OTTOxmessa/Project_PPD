import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

function RegisterPage() {
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const { register } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await register(username, email, password)
      navigate('/')
    } catch (err) {
      setError(err.response?.data?.message || 'สมัครสมาชิกไม่สำเร็จ')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={handleSubmit}>
        <h1>สมัครสมาชิก</h1>
        <div className="form-group">
          <label htmlFor="username">ชื่อผู้ใช้</label>
          <input id="username" type="text" value={username} onChange={(e) => setUsername(e.target.value)} required />
        </div>
        <div className="form-group">
          <label htmlFor="email">อีเมล</label>
          <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </div>
        <div className="form-group">
          <label htmlFor="password">รหัสผ่าน (อย่างน้อย 8 ตัวอักษร)</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={8}
            required
          />
        </div>
        {error && <p className="error-message">{error}</p>}
        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? 'กำลังสมัคร...' : 'สมัครสมาชิก'}
        </button>
        <p className="muted">
          มีบัญชีอยู่แล้ว? <Link to="/login">เข้าสู่ระบบ</Link>
        </p>
      </form>
    </div>
  )
}

export default RegisterPage
