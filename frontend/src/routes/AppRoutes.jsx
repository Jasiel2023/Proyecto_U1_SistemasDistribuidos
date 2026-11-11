import { Routes, Route, Navigate } from 'react-router-dom'
import { ProtectedRoute } from '../components/ProtectedRoute'
import { Layout } from '../components/Layout'
import { LoginPage } from '../pages/LoginPage'
import { DashboardPage } from '../pages/DashboardPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import VotacionPage from '../features/votacion/VotacionPage'
import AdminPage from '../features/admin/AdminPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/votacion" element={<VotacionPage />} />
        <Route path="/admin" element={<AdminPage />} />
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
