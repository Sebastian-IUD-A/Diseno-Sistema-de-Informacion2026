import type { ReactElement } from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import { AuthProvider } from '../auth/AuthContext';
import type { Rol, Usuario } from '../api/types';

/** Ayudas compartidas por las pruebas del frontend. */

export const operador: Usuario = { id: 2, nombre: 'Carlos Operador', correo: 'operador@autoscolombia.co', rol: 'OPERADOR' };
export const admin: Usuario = { id: 1, nombre: 'Laura Administradora', correo: 'admin@autoscolombia.co', rol: 'ADMINISTRADOR' };

/** Simula una sesión iniciada (token + usuario en sessionStorage). */
export function iniciarSesionSimulada(rol: Rol) {
  sessionStorage.setItem('autos-colombia.token', 'token-de-prueba');
  sessionStorage.setItem('autos-colombia.usuario', JSON.stringify(rol === 'OPERADOR' ? operador : admin));
}

export function renderizar(ui: ReactElement, { ruta = '/', patron = '*' }: { ruta?: string; patron?: string } = {}) {
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <AuthProvider>
        <Routes>
          <Route path={patron} element={ui} />
          <Route path="/login" element={<p>Pantalla de login</p>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

export function respuestaJson(cuerpo: unknown, status = 200): Response {
  return new Response(JSON.stringify(cuerpo), { status, headers: { 'Content-Type': 'application/json' } });
}

type Manejador = (url: string, init: RequestInit) => Response | undefined;

/**
 * Reemplaza fetch por un doble que responde según la URL y el método.
 * Devuelve el mock para verificar con qué se llamó.
 */
export function simularFetch(manejadores: Manejador[]) {
  const mock = vi.fn(async (entrada: RequestInfo | URL, init: RequestInit = {}) => {
    const url = typeof entrada === 'string' ? entrada : entrada.toString();
    for (const m of manejadores) {
      const r = m(url, init);
      if (r) return r;
    }
    return respuestaJson({ status: 404, codigo: 'NO_SIMULADO', mensaje: `Sin respuesta simulada para ${url}` }, 404);
  });
  vi.stubGlobal('fetch', mock);
  return mock;
}

export const ruta =
  (metodo: string, patron: RegExp, respuesta: () => Response): Manejador =>
  (url, init) =>
    (init.method ?? 'GET') === metodo && patron.test(url) ? respuesta() : undefined;
