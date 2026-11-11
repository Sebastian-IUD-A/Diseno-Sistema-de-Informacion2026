import { Navigate, Route, Routes } from 'react-router-dom';
import { RutaProtegida } from './auth/RutaProtegida';
import { Plantilla } from './layout/Plantilla';
import { LoginPage } from './features/login/LoginPage';
import { DashboardPage } from './features/dashboard/DashboardPage';
import { EntradaPage } from './features/entrada/EntradaPage';
import { SalidaPage } from './features/salida/SalidaPage';
import { OcupacionPage } from './features/ocupacion/OcupacionPage';
import { HistorialPage } from './features/historial/HistorialPage';
import { NovedadesPage } from './features/novedades/NovedadesPage';
import { CeldasPage } from './features/celdas/CeldasPage';

/** Mapa de rutas de la aplicación. Cada pantalla vive en su propia carpeta dentro de features/. */
export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RutaProtegida>
            <Plantilla />
          </RutaProtegida>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="entrada" element={<RutaProtegida roles={['OPERADOR']}><EntradaPage /></RutaProtegida>} />
        <Route path="salida" element={<RutaProtegida roles={['OPERADOR']}><SalidaPage /></RutaProtegida>} />
        <Route path="ocupacion" element={<OcupacionPage />} />
        <Route path="historial" element={<RutaProtegida roles={['ADMINISTRADOR']}><HistorialPage /></RutaProtegida>} />
        <Route path="novedades" element={<NovedadesPage />} />
        <Route path="celdas" element={<RutaProtegida roles={['ADMINISTRADOR']}><CeldasPage /></RutaProtegida>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
