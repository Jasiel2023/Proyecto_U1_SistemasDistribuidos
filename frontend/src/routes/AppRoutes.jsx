import { Routes, Route, Navigate } from 'react-router-dom'
import { ProtectedRoute } from '../components/ProtectedRoute'
import { AdminRoute } from '../components/AdminRoute'
import { Layout } from '../components/Layout'
import { LoginPage } from '../pages/LoginPage'
import { DashboardPage } from '../pages/DashboardPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import VotacionPage from '../features/votacion/VotacionPage'
import { AdminDashboardPage } from '../pages/AdminDashboardPage'
import { AdminUsuariosPage } from '../pages/AdminUsuariosPage'
import AdminPage from '../features/admin/AdminPage'
import ResultadosPage from '../features/resultados/ResultadosPage.jsx'
export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/resultados" element={<ResultadosPage />} />
      </Route>

      <Route element={<AdminRoute><Layout /></AdminRoute>}>
        <Route path="/admin" element={<Navigate to="/admin/dashboard" replace />} />
        <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
        <Route path="/admin/elecciones" element={<AdminPage />} />
        <Route path="/admin/usuarios" element={<AdminUsuariosPage />} />
      </Route>

        <Route path="/votacion" element={<VotacionPage />} />
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
