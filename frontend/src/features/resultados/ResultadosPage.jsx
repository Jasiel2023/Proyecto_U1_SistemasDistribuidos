import { useState } from 'react';
import { useResultados } from './useResultados.js';
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
  const [borrador, setBorrador] = useState(eleccionInicial);
  const [eleccionId, setEleccionId] = useState(() => {
    const inicial = eleccionInicial();
    return UUID.test(inicial) ? inicial : '';
  });
  const [errorFormulario, setErrorFormulario] = useState(null);
  const { snapshot, cargando, error, conexion } = useResultados(eleccionId, dependencias);

  const consultar = (evento) => {
    evento.preventDefault();
    const valor = borrador.trim();
    if (!UUID.test(valor)) {
      setErrorFormulario('Ingrese un UUID válido de elección.');
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
          Consulta persistente y actualizaciones en tiempo real. Los nombres de elecciones y
          candidatos no forman parte de este módulo.
        </p>
      </header>

      <div className="panel">
        <form className="consulta" onSubmit={consultar}>
          <label htmlFor="eleccion">Identificador de elección (UUID)</label>
          <div className="fila">
            <input
              id="eleccion"
              value={borrador}
              onChange={(evento) => setBorrador(evento.target.value)}
              placeholder="00000000-0000-4000-8000-000000000000"
              autoComplete="off"
              spellCheck="false"
            />
            <button type="submit">Consultar</button>
          </div>
          {errorFormulario && <p className="error" role="alert">{errorFormulario}</p>}
          <p className="ayuda">
            El catálogo de elecciones no pertenece a este módulo; se muestra el UUID recibido.
          </p>
        </form>

        {!eleccionId && <p className="vacio">Ingrese una elección para consultar sus resultados.</p>}

        {eleccionId && (
          <article className="resultado" aria-live="polite">
            <div className="barra">
              <span className={`conexion conexion-${conexion}`}>
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
                      <th>Candidato (UUID)</th>
                      <th>Votos</th>
                    </tr>
                  </thead>
                  <tbody>
                    {snapshot.conteos.map((conteo) => (
                      <tr key={conteo.candidatoId}>
                        <td className="mono">{conteo.candidatoId}</td>
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
