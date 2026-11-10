import VotacionPage from './features/votacion/VotacionPage'
import AdminPage from './features/admin/AdminPage'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate replace to="/votacion" />} />
        <Route path="/votacion" element={<VotacionPage />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="*" element={<Navigate replace to="/votacion" />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
