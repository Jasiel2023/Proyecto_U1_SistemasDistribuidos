import { useEffect, useState } from 'react';
import { useResultados } from './useResultados.js';
import { api } from '../../api/client';
import '../../styles.css';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

const ETIQUETAS_CONEXION = {
  inactivo: 'Sin consulta',
  conectando: 'Conectando',
  conectado: 'En vivo',
  reconectando: 'Reconectando',
  error: 'Canal con error'
};

function eleccionInicial() {
  const parametro = new URLSearchParams(window.location.search).get('eleccion');
  if (parametro && UUID.test(parametro)) return parametro;
  const guardada = window.localStorage.getItem('resultados.eleccion');
  return guardada && UUID.test(guardada) ? guardada : '';
}

export default function ResultadosPage({ dependencias }) {
  const [elecciones, setElecciones] = useState([]);
  const [cargandoElecciones, setCargandoElecciones] = useState(true);
  const [borrador, setBorrador] = useState(eleccionInicial);
  const [eleccionId, setEleccionId] = useState(() => {
    const inicial = eleccionInicial();
    return UUID.test(inicial) ? inicial : '';
  });
  const [errorFormulario, setErrorFormulario] = useState(null);
  const { snapshot, cargando, error, conexion } = useResultados(eleccionId, dependencias);

  useEffect(() => {
    let activo = true;
    api.get('/elecciones')
      .then(({ data }) => {
        if (activo) setElecciones(data);
      })
      .catch(() => {
        if (activo) setErrorFormulario('No fue posible cargar las elecciones.');
      })
      .finally(() => {
        if (activo) setCargandoElecciones(false);
      });

    return () => {
      activo = false;
    };
  }, []);

  const consultar = (evento) => {
    evento.preventDefault();
    const valor = borrador;
    if (!UUID.test(valor)) {
      setErrorFormulario('Seleccione una elección válida.');
      return;
    }
    setErrorFormulario(null);
    window.localStorage.setItem('resultados.eleccion', valor);
    const url = new URL(window.location.href);
    url.searchParams.set('eleccion', valor);
    window.history.replaceState(null, '', url);
    setEleccionId(valor);
  };

  return (
    <section className="resultados-app">
      <header className="encabezado">
        <h1>Resultados electorales</h1>
        <p>
          Consulta los votos acumulados y recibe actualizaciones en tiempo real.
        </p>
      </header>

      <div className="panel">
        <form className="consulta" onSubmit={consultar}>
          <label htmlFor="eleccion">Elección</label>
          <div className="fila">
            <select
              id="eleccion"
              value={borrador}
              onChange={(evento) => setBorrador(evento.target.value)}
              disabled={cargandoElecciones}
            >
              <option value="">Seleccione una elección</option>
              {elecciones.map((eleccion) => (
                <option key={eleccion.id} value={eleccion.id}>{eleccion.titulo}</option>
              ))}
            </select>
            <button type="submit">Consultar</button>
          </div>
          {errorFormulario && <p className="error" role="alert">{errorFormulario}</p>}
        </form>

        {!eleccionId && <p className="vacio">Ingrese una elección para consultar sus resultados.</p>}

        {eleccionId && (
          <article className="resultado" aria-live="polite">
            <div className="barra">
              <span className={`conexion conexion-${conexion}`}>
                <span className="indicador-live" aria-hidden="true" />
                {ETIQUETAS_CONEXION[conexion] ?? conexion}
              </span>
              {snapshot && <span className="revision">Revisión {snapshot.revision}</span>}
            </div>

            {cargando && !snapshot && <p className="vacio">Cargando resultados...</p>}
            {error && <p className="error" role="alert">{error}</p>}

            {snapshot && !snapshot.disponible && (
              <p className="vacio">Aún no existen votos contabilizados para esta elección.</p>
            )}

            {snapshot && snapshot.disponible && (
              <>
                <h2>{elecciones.find((eleccion) => String(eleccion.id) === String(snapshot.eleccionId))?.titulo || 'Elección'}</h2>
                <p className="total">
                  <span>Total de votos</span>
                  <strong>{snapshot.totalVotos}</strong>
                </p>
                <p className="actualizado">
                  Última actualización: {new Date(snapshot.ultimaActualizacion).toLocaleString('es-EC')}
                </p>
                <table>
                  <thead>
                    <tr>
                      <th>Candidato</th>
                      <th>Votos</th>
                    </tr>
                  </thead>
                  <tbody>
                    {snapshot.conteos.map((conteo) => (
                      <tr key={conteo.candidatoId}>
                        <td>{nombreCandidato(conteo.candidatoId, elecciones, snapshot.eleccionId)}</td>
                        <td>{conteo.votos}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </>
            )}
          </article>
        )}
      </div>
    </section>
  );
}

function nombreCandidato(candidatoId, elecciones, eleccionId) {
  const eleccion = elecciones.find((item) => String(item.id) === String(eleccionId));
  const candidato = eleccion?.candidatos?.find((item) => (
    String(item.id ?? item.candidatoId) === String(candidatoId)
  ));

  if (!candidato) return 'Candidato no identificado';
  return [candidato.nombre, candidato.apellido].filter(Boolean).join(' ')
    || candidato.nombreCompleto
    || 'Candidato';
}
