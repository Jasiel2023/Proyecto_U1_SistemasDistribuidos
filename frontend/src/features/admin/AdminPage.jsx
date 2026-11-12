import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import './AdminPage.css'

const emptyElection = {
  titulo: '',
  fecha: '',
  horaInicio: '08:00',
  horaFin: '17:00',
  estaActiva: false,
}

const emptyCandidate = { nombre: '', apellido: '', partidoPolitico: '' }

function AdminPage() {
  const [elecciones, setElecciones] = useState([])
  const [form, setForm] = useState(emptyElection)
  const [candidateForms, setCandidateForms] = useState({})
  const [editingId, setEditingId] = useState(null)
  const [status, setStatus] = useState({ type: 'idle', message: '' })

  useEffect(() => {
    cargarElecciones()
  }, [])

  async function cargarElecciones() {
    try {
      const response = await api.get('/admin/elecciones')
      setElecciones(response.data)
      setStatus({ type: 'idle', message: '' })
    } catch {
      setStatus({ type: 'error', message: 'No fue posible cargar las elecciones.' })
    }
  }

  function handleFormChange(event) {
    const { name, value, type, checked } = event.target
    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
  }

  function startEditing(eleccion) {
    setEditingId(eleccion.id)
    setForm({
      titulo: eleccion.titulo,
      fecha: eleccion.fecha,
      horaInicio: eleccion.horaInicio.slice(0, 5),
      horaFin: eleccion.horaFin.slice(0, 5),
      estaActiva: eleccion.estaActiva,
    })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function resetForm() {
    setEditingId(null)
    setForm(emptyElection)
  }

  async function saveElection(event) {
    event.preventDefault()
    setStatus({ type: 'loading', message: 'Guardando elección...' })
    try {
      const request = { ...form, estaActiva: Boolean(form.estaActiva) }
      if (editingId) {
        await api.put(`/admin/elecciones/${editingId}`, request)
      } else {
        await api.post('/admin/elecciones', request)
      }
      resetForm()
      await cargarElecciones()
      setStatus({ type: 'success', message: 'Elección guardada correctamente.' })
    } catch (error) {
      setStatus({ type: 'error', message: error.response?.data?.message || 'No fue posible guardar la elección.' })
    }
  }

  function updateCandidateForm(eleccionId, event) {
    const { name, value } = event.target
    setCandidateForms((current) => ({
      ...current,
      [eleccionId]: { ...(current[eleccionId] || emptyCandidate), [name]: value },
    }))
  }

  async function addCandidate(eleccionId, event) {
    event.preventDefault()
    const candidate = candidateForms[eleccionId] || emptyCandidate
    try {
      await api.post(`/admin/elecciones/${eleccionId}/candidatos`, candidate)
      setCandidateForms((current) => ({ ...current, [eleccionId]: emptyCandidate }))
      await cargarElecciones()
      setStatus({ type: 'success', message: 'Candidato agregado correctamente.' })
    } catch (error) {
      setStatus({ type: 'error', message: error.response?.data?.message || 'No fue posible agregar el candidato.' })
    }
  }

  async function removeCandidate(eleccionId, candidatoId) {
    if (!window.confirm('¿Eliminar este candidato?')) return
    await api.delete(`/admin/elecciones/${eleccionId}/candidatos/${candidatoId}`)
    await cargarElecciones()
  }

  async function removeElection(id) {
    if (!window.confirm('¿Eliminar esta elección y sus candidatos?')) return
    try {
      await api.delete(`/admin/elecciones/${id}`)
      await cargarElecciones()
      setStatus({ type: 'success', message: 'Elección eliminada.' })
    } catch (error) {
      setStatus({ type: 'error', message: error.response?.data?.message || 'No fue posible eliminarla.' })
    }
  }

  return (
    <main className="admin-page">
      <header className="admin-header">
        <div>
          <p className="admin-eyebrow">Panel administrativo</p>
          <h1>Gestiona tus elecciones</h1>
          <p>Configura fechas, horarios, apertura y candidatos desde un solo lugar.</p>
        </div>
        <a href="/votacion">Ver boleta</a>
      </header>

      <section className="admin-editor">
        <div className="admin-section-title">
          <span>01</span>
          <div>
            <p className="admin-eyebrow">Configuración</p>
            <h2>{editingId ? 'Editar elección' : 'Nueva elección'}</h2>
          </div>
        </div>
        <form className="admin-election-form" onSubmit={saveElection}>
          <label>Título<input name="titulo" onChange={handleFormChange} required value={form.titulo} /></label>
          <label>Fecha<input name="fecha" onChange={handleFormChange} required type="date" value={form.fecha} /></label>
          <label>Hora de inicio<input name="horaInicio" onChange={handleFormChange} required type="time" value={form.horaInicio} /></label>
          <label>Hora de fin<input name="horaFin" onChange={handleFormChange} required type="time" value={form.horaFin} /></label>
          <label className="active-toggle"><input checked={form.estaActiva} name="estaActiva" onChange={handleFormChange} type="checkbox" /><span>Elección abierta</span></label>
          <div className="admin-actions">
            <button type="submit">{editingId ? 'Guardar cambios' : 'Crear elección'}</button>
            {editingId && <button className="secondary-button" onClick={resetForm} type="button">Cancelar</button>}
          </div>
        </form>
        {status.message && <p className={`admin-status ${status.type}`}>{status.message}</p>}
      </section>

      <section className="election-list">
        <div className="list-heading"><div><p className="admin-eyebrow">Contenido</p><h2>Elecciones registradas</h2></div><span>{elecciones.length} total</span></div>
        {elecciones.map((eleccion) => {
          const candidate = candidateForms[eleccion.id] || emptyCandidate
          return (
            <article className="election-card" key={eleccion.id}>
              <div className="election-card-head">
                <div><span className={`state-pill ${eleccion.estaActiva ? 'open' : ''}`}>{eleccion.estaActiva ? 'Abierta' : 'Cerrada'}</span><h3>{eleccion.titulo}</h3><p>{formatDate(eleccion.fecha)} · {eleccion.horaInicio.slice(0, 5)} - {eleccion.horaFin.slice(0, 5)}</p></div>
                <div className="card-actions"><button onClick={() => startEditing(eleccion)} type="button">Editar</button><button className="danger-button" onClick={() => removeElection(eleccion.id)} type="button">Eliminar</button></div>
              </div>
              <div className="candidate-admin-list">
                {eleccion.candidatos.map((candidato) => <div className="candidate-admin-row" key={candidato.id}><span><strong>{candidato.nombre} {candidato.apellido}</strong><small>{candidato.partidoPolitico}</small></span><button className="remove-button" onClick={() => removeCandidate(eleccion.id, candidato.id)} type="button">Quitar</button></div>)}
              </div>
              <form className="candidate-form" onSubmit={(event) => addCandidate(eleccion.id, event)}>
                <input name="nombre" onChange={(event) => updateCandidateForm(eleccion.id, event)} placeholder="Nombre" required value={candidate.nombre} />
                <input name="apellido" onChange={(event) => updateCandidateForm(eleccion.id, event)} placeholder="Apellido" required value={candidate.apellido} />
                <input name="partidoPolitico" onChange={(event) => updateCandidateForm(eleccion.id, event)} placeholder="Partido político" required value={candidate.partidoPolitico} />
                <button type="submit">Agregar candidato</button>
              </form>
            </article>
          )
        })}
      </section>
    </main>
  )
}

function formatDate(value) {
  const [year, month, day] = value.split('-')
  return `${day}/${month}/${year}`
}

export default AdminPage
