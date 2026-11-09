import ResultadosPage from './features/resultados/ResultadosPage.jsx';

export default function App() {
  return (
    <main className="contenedor">
      <header className="encabezado">
        <h1>Resultados electorales</h1>
        <p>Consulta persistente y actualizaciones en tiempo real. Los nombres de elecciones y candidatos no forman parte de este módulo.</p>
      </header>
      <ResultadosPage />
    </main>
  );
}
