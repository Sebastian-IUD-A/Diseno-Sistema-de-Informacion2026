import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import type { Rol } from '../api/types';
import { useAuth } from './AuthContext';
import { Vacio } from '../components/Estados';

/**
 * Protege una pantalla: sin sesión → redirige al login; rol no permitido → mensaje.
 * OJO: esto es solo comodidad visual. La seguridad real está en el backend (SecurityConfig),
 * que rechaza con 403 cualquier llamada no permitida aunque alguien manipule el navegador.
 */
export function RutaProtegida({ roles, children }: { roles?: Rol[]; children: ReactNode }) {
  const { usuario } = useAuth();
  const ubicacion = useLocation();

  if (!usuario) {
    return <Navigate to="/login" replace state={{ desde: ubicacion.pathname }} />;
  }
  if (roles && !roles.includes(usuario.rol)) {
    return (
      <Vacio
        titulo="Sin permisos"
        texto="Su rol no tiene acceso a esta sección. Si cree que es un error, contacte al administrador."
      />
    );
  }
  return <>{children}</>;
}
