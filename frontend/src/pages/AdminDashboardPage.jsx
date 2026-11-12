import { useState, useEffect } from 'react'
import { listUsuarios } from '../api/usuarioApi'

export function AdminDashboardPage() {
  const [stats, setStats] = useState({
    totalUsuarios: 0,
    totalVotantes: 0,
    totalAdmins: 0,
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    cargarStats()
  }, [])

  const cargarStats = async () => {
    try {
      const { content = [] } = await listUsuarios({ page: 0, size: 1000 })
      const totalVotantes = content.filter((u) => u.rol === 'VOTANTE').length
      const totalAdmins = content.filter((u) => u.rol === 'ADMIN').length
      setStats({
        totalUsuarios: content.length,
        totalVotantes,
        totalAdmins,
      })
    } catch (err) {
      console.error('Error cargando estadísticas:', err)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="admin-dashboard">
        <div className="admin-header">
          <h1>Panel de Administración</h1>
          <p>Cargando estadísticas...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="admin-dashboard">
      <div className="admin-header">
        <div>
          <h1>Panel de Administración</h1>
        </div>
      </div>

      <div className="admin-stats">
        <div className="stat-card">
          <h3>Total Usuarios</h3>
          <span className="stat-number">{stats.totalUsuarios}</span>
        </div>
        <div className="stat-card">
          <h3>Votantes</h3>
          <span className="stat-number">{stats.totalVotantes}</span>
        </div>
        <div className="stat-card">
          <h3>Administradores</h3>
          <span className="stat-number">{stats.totalAdmins}</span>
        </div>
      </div>

      <div className="admin-actions">
        <h2>Operaciones disponibles</h2>
        <p>Desde aquí puedes crear, editar y gestionar los usuarios del sistema de votación.</p>
        <div className="action-grid">
          <a href="/admin/usuarios" className="btn btn-primary">
            Gestionar Usuarios
          </a>
        </div>
      </div>
    </div>
  )
}
