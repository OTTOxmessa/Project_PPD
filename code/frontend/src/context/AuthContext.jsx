import { createContext, useContext, useState, useEffect } from 'react'
import apiClient from '../api/client.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'))
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })

  useEffect(() => {
    if (token) {
      localStorage.setItem('token', token)
    } else {
      localStorage.removeItem('token')
    }
  }, [token])

  useEffect(() => {
    if (user) {
      localStorage.setItem('user', JSON.stringify(user))
    } else {
      localStorage.removeItem('user')
    }
  }, [user])

  const login = async (email, password) => {
    const response = await apiClient.post('/auth/login', { email, password })
    setToken(response.data.token)
    setUser({ userId: response.data.userId, username: response.data.username, email: response.data.email })
  }

  const register = async (username, email, password) => {
    const response = await apiClient.post('/auth/register', { username, email, password })
    setToken(response.data.token)
    setUser({ userId: response.data.userId, username: response.data.username, email: response.data.email })
  }

  const logout = () => {
    setToken(null)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ token, user, login, register, logout, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth ต้องถูกเรียกใน AuthProvider เท่านั้น')
  }
  return context
}
