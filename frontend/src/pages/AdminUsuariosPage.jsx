import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import {
  listUsuarios,
  createUsuario,
  updateUsuario,
  deleteUsuario,
} from '../api/usuarioApi'

const ROLES = [
  { value: 'VOTANTE', label: 'Votante' },
  { value: 'ADMIN', label: 'Administrador' },
]

export function AdminUsuariosPage() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  const [usuarios, setUsuarios] = useState([])
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)

  // Modal estado
  const [modalOpen, setModalOpen] = useState(false)
  const [modalMode, setModalMode] = useState('crear') // 'crear' | 'editar'
  const [editingId, setEditingId] = useState(null)

  // Errores de validación por campo
  const [formErrors, setFormErrors] = useState({})

  const [form, setForm] = useState({
    cedula: '',
    correo: '',
    nickname: '',
    nombre: '',
    apellido: '',
    clave: '',
    rol: 'VOTANTE',
  })

  const handleLogout = () => {
    logout()
  }

  const handleVolver = () => {
    navigate('/admin/dashboard')
  }

  useEffect(() => {
    cargarUsuarios()
  }, [page, size])

  const cargarUsuarios = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await listUsuarios({ page, size })
      setUsuarios(data.content || [])
      setTotalPages(data.totalPages || 0)
    } catch (err) {
      setError('No se pudo cargar la lista de usuarios')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleChange = (e) => {
    const { name, value } = e.target

    // Validación en tiempo real: cédula solo números y máximo 10 dígitos
    if (name === 'cedula') {
      const soloNumeros = value.replace(/\D/g, '')
      setForm((prev) => ({ ...prev, cedula: soloNumeros.slice(0, 10) }))
    } else {
      setForm((prev) => ({ ...prev, [name]: value }))
    }

    // Limpiar error del campo si el usuario está escribiendo
    if (formErrors[name]) {
      setFormErrors((prev) => {
        const next = { ...prev }
        delete next[name]
        return next
      })
    }
  }

  const resetForm = () => {
    setForm({
      cedula: '',
      correo: '',
      nickname: '',
      nombre: '',
      apellido: '',
      clave: '',
      rol: 'VOTANTE',
    })
    setFormErrors({})
    setEditingId(null)
    setError(null)
    setSuccess(null)
  }

  const abrirCrear = () => {
    setModalMode('crear')
    setEditingId(null)
    resetForm()
    setModalOpen(true)
  }

  const abrirEditar = (usuario) => {
    setModalMode('editar')
    setEditingId(usuario.id)
    setForm({
      cedula: usuario.cedula || '',
      correo: usuario.correo || '',
      nickname: usuario.nickname || '',
      nombre: usuario.nombre || '',
      apellido: usuario.apellido || '',
      clave: '',
      rol: usuario.rol || 'VOTANTE',
    })
    setFormErrors({})
    setModalOpen(true)
  }

  const cerrarModal = () => {
    setModalOpen(false)
    resetForm()
  }

  const validarFormulario = () => {
    const errores = {}
    const cedulaRegex = /^\d{10}$/
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

    if (!form.cedula.trim()) {
      errores.cedula = 'La cédula es obligatoria'
    } else if (!cedulaRegex.test(form.cedula.trim())) {
      errores.cedula = 'La cédula debe tener exactamente 10 dígitos numéricos'
    }

    if (!form.correo.trim()) {
      errores.correo = 'El correo es obligatorio'
    } else if (!emailRegex.test(form.correo.trim())) {
      errores.correo = 'El correo no tiene un formato válido'
    }

    if (!form.nickname.trim()) {
      errores.nickname = 'El nickname es obligatorio'
    }

    if (!form.nombre.trim()) {
      errores.nombre = 'El nombre es obligatorio'
    }

    if (!form.apellido.trim()) {
      errores.apellido = 'El apellido es obligatorio'
    }

    if (!form.clave.trim()) {
      errores.clave = 'La clave es obligatoria'
    } else if (form.clave.trim().length < 6) {
      errores.clave = 'La clave debe tener al menos 6 caracteres'
    }

    setFormErrors(errores)
    return Object.keys(errores).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)

    if (!validarFormulario()) {
      setLoading(false)
      return
    }

    setLoading(true)

    try {
      const payload = {
        cedula: form.cedula.trim(),
        correo: form.correo.trim().toLowerCase(),
        nickname: form.nickname.trim(),
        nombre: form.nombre.trim(),
        apellido: form.apellido.trim(),
        clave: form.clave.trim(),
        rol: form.rol,
      }

      if (modalMode === 'crear') {
        await createUsuario(payload)
        setSuccess('Usuario creado correctamente')
      } else {
        await updateUsuario(editingId, payload)
        setSuccess('Usuario actualizado correctamente')
      }

      cerrarModal()
      cargarUsuarios()
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.mensaje ||
        'Error al guardar el usuario'
      setError(msg)
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleEliminar = async (id) => {
    if (!window.confirm('¿Estás seguro de que deseas eliminar este usuario?')) return
    setLoading(true)
    setError(null)
    try {
      await deleteUsuario(id)
      setSuccess('Usuario eliminado correctamente')
      cargarUsuarios()
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.mensaje ||
        'Error al eliminar el usuario'
      setError(msg)
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="admin-usuarios">
      <div className="page-header">
        <div>
          <h1>Gestión de Usuarios</h1>
          <p>CRUD de usuarios y cuentas del sistema de votación</p>
        </div>
        <div className="page-actions">
          <button className="btn btn-outline" onClick={handleVolver}>
            Volver
          </button>
          <button className="btn btn-primary" onClick={abrirCrear}>
            + Nuevo Usuario
          </button>
        </div>
      </div>

      {error && (
        <div className="alert alert-error" role="alert">
          {error}
        </div>
      )}
      {success && (
        <div className="alert alert-success" role="status">
          {success}
        </div>
      )}

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Cédula</th>
              <th>Correo</th>
              <th>Nickname</th>
              <th>Nombre</th>
              <th>Apellido</th>
              <th>ID Cuenta</th>
              <th>Rol</th>
              <th className="actions">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="9">Cargando usuarios...</td>
              </tr>
            ) : usuarios.length === 0 ? (
              <tr>
                <td colSpan="9">No se encontraron usuarios</td>
              </tr>
            ) : (
              usuarios.map((u) => (
                <tr key={u.id}>
                  <td>{u.id}</td>
                  <td>{u.cedula}</td>
                  <td>{u.correo}</td>
                  <td>{u.nickname}</td>
                  <td>{u.nombre}</td>
                  <td>{u.apellido}</td>
                  <td>{u.idCuenta}</td>
                  <td>
                    <span className={`badge ${u.rol === 'ADMIN' ? 'badge-admin' : 'badge-votante'}`}>
                      {u.rol}
                    </span>
                  </td>
                  <td className="actions">
                    <button
                      className="btn btn-sm btn-outline"
                      onClick={() => abrirEditar(u)}
                      title="Editar"
                    >
                      Editar
                    </button>
                    <button
                      className="btn btn-sm btn-danger"
                      onClick={() => handleEliminar(u.id)}
                      title="Eliminar"
                    >
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        <div className="pagination">
          <button
            className="btn btn-sm btn-outline"
            onClick={() => setPage((p) => Math.max(p - 1, 0))}
            disabled={page === 0}
          >
            Anterior
          </button>
          <span>Página {page + 1} de {totalPages}</span>
          <button
            className="btn btn-sm btn-outline"
            onClick={() => setPage((p) => Math.min(p + 1, totalPages - 1))}
            disabled={page >= totalPages - 1}
          >
            Siguiente
          </button>
        </div>
      </div>

      {/* Modal crear/editar */}
      {modalOpen && (
        <div className="modal-overlay" onClick={cerrarModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>{modalMode === 'crear' ? 'Crear Usuario' : 'Editar Usuario'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Cédula</label>
                  <input
                    name="cedula"
                    type="tel"
                    maxLength="10"
                    value={form.cedula}
                    onChange={handleChange}
                    required
                  />
                  {formErrors.cedula && (
                    <span className="error-message">{formErrors.cedula}</span>
                  )}
                </div>
                <div className="form-group">
                  <label>Correo</label>
                  <input
                    name="correo"
                    type="email"
                    value={form.correo}
                    onChange={handleChange}
                    required
                  />
                  {formErrors.correo && (
                    <span className="error-message">{formErrors.correo}</span>
                  )}
                </div>
                <div className="form-group">
                  <label>Nickname</label>
                  <input
                    name="nickname"
                    type="text"
                    value={form.nickname}
                    onChange={handleChange}
                    required
                  />
                  {formErrors.nickname && (
                    <span className="error-message">{formErrors.nickname}</span>
                  )}
                </div>
                <div className="form-group">
                  <label>Nombre</label>
                  <input
                    name="nombre"
                    type="text"
                    value={form.nombre}
                    onChange={handleChange}
                    required
                  />
                  {formErrors.nombre && (
                    <span className="error-message">{formErrors.nombre}</span>
                  )}
                </div>
                <div className="form-group">
                  <label>Apellido</label>
                  <input
                    name="apellido"
                    type="text"
                    value={form.apellido}
                    onChange={handleChange}
                    required
                  />
                  {formErrors.apellido && (
                    <span className="error-message">{formErrors.apellido}</span>
                  )}
                </div>
                <div className="form-group">
                  <label>Clave</label>
                  <input
                    name="clave"
                    type="password"
                    value={form.clave}
                    onChange={handleChange}
                    required
                  />
                  {formErrors.clave && (
                    <span className="error-message">{formErrors.clave}</span>
                  )}
                </div>
                <div className="form-group">
                  <label>Rol</label>
                  <select name="rol" value={form.rol} onChange={handleChange}>
                    {ROLES.map((r) => (
                      <option key={r.value} value={r.value}>
                        {r.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="modal-actions">
                <button type="button" className="btn btn-outline" onClick={cerrarModal}>
                  Cancelar
                </button>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  {modalMode === 'crear' ? 'Crear' : 'Guardar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
