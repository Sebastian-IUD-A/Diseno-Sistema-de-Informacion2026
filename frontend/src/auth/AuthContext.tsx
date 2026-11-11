import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { almacenToken, registrarOyenteNoAutorizado } from '../api/client';
import { api } from '../api/endpoints';
import type { Rol, Usuario } from '../api/types';

/**
 * Contexto de autenticación: guarda quién inició sesión y expone login/logout a toda la app.
 * El token se guarda en sessionStorage (se borra al cerrar la pestaña), adecuado para
 * equipos compartidos como la caseta del parqueadero.
 */

const CLAVE_USUARIO = 'autos-colombia.usuario';

interface ValorAuth {
  usuario: Usuario | null;
  login: (correo: string, contrasena: string) => Promise<Usuario>;
  logout: (motivo?: string) => void;
  tieneRol: (...roles: Rol[]) => boolean;
  avisoSesion: string | null;
  limpiarAviso: () => void;
}

const AuthContext = createContext<ValorAuth | null>(null);

function leerUsuarioGuardado(): Usuario | null {
  if (!almacenToken.leer()) return null;
  try {
    const crudo = sessionStorage.getItem(CLAVE_USUARIO);
    return crudo ? (JSON.parse(crudo) as Usuario) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(leerUsuarioGuardado);
  const [avisoSesion, setAvisoSesion] = useState<string | null>(null);

  const logout = useCallback((motivo?: string) => {
    if (almacenToken.leer()) {
      api.logout().catch(() => undefined); // informativo; el token se descarta igual
    }
    almacenToken.borrar();
    sessionStorage.removeItem(CLAVE_USUARIO);
    setUsuario(null);
    setAvisoSesion(motivo ?? null);
  }, []);

  const login = useCallback(async (correo: string, contrasena: string) => {
    const sesion = await api.login(correo, contrasena);
    almacenToken.guardar(sesion.token);
    sessionStorage.setItem(CLAVE_USUARIO, JSON.stringify(sesion.usuario));
    setUsuario(sesion.usuario);
    setAvisoSesion(null);
    return sesion.usuario;
  }, []);

  // Si el backend responde 401 en cualquier petición, la sesión expiró: se cierra.
  useEffect(() => {
    registrarOyenteNoAutorizado(() => {
      almacenToken.borrar();
      sessionStorage.removeItem(CLAVE_USUARIO);
      setUsuario(null);
      setAvisoSesion('Su sesión expiró. Inicie sesión nuevamente.');
    });
    return () => registrarOyenteNoAutorizado(null);
  }, []);

  const tieneRol = useCallback((...roles: Rol[]) => !!usuario && roles.includes(usuario.rol), [usuario]);

  const valor = useMemo<ValorAuth>(
    () => ({ usuario, login, logout, tieneRol, avisoSesion, limpiarAviso: () => setAvisoSesion(null) }),
    [usuario, login, logout, tieneRol, avisoSesion],
  );

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth(): ValorAuth {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>');
  return ctx;
}
