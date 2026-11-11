import { peticion } from './client';
import type {
  Celda,
  CeldaSolicitud,
  Dashboard,
  Movimiento,
  Novedad,
  Sesion,
  TipoNovedad,
  TipoVehiculo,
  Usuario,
  Vehiculo,
} from './types';

/**
 * Un solo lugar con todas las llamadas a la API REST.
 * Las pantallas importan estas funciones y nunca escriben URLs a mano.
 * Ninguna petición envía el id del usuario: el backend lo toma del token.
 */
export const api = {
  login: (correo: string, contrasena: string) =>
    peticion<Sesion>('/api/auth/login', { method: 'POST', body: { correo, contrasena } }),
  perfil: () => peticion<Usuario>('/api/auth/me'),
  logout: () => peticion<void>('/api/auth/logout', { method: 'POST' }),

  dashboard: () => peticion<Dashboard>('/api/dashboard/summary'),

  vehiculo: (placa: string) => peticion<Vehiculo>(`/api/vehicles/${encodeURIComponent(placa.trim())}`),

  celdas: () => peticion<Celda[]>('/api/cells'),
  celdasDisponibles: (tipo: TipoVehiculo) => peticion<Celda[]>('/api/cells/available', { query: { type: tipo } }),
  crearCelda: (datos: CeldaSolicitud) => peticion<Celda>('/api/cells', { method: 'POST', body: datos }),
  actualizarCelda: (id: number, datos: CeldaSolicitud) =>
    peticion<Celda>(`/api/cells/${id}`, { method: 'PUT', body: datos }),

  registrarEntrada: (placa: string, celdaId: number) =>
    peticion<Movimiento>('/api/movements/entries', { method: 'POST', body: { placa, celdaId } }),
  registrarSalida: (movimientoId: number) =>
    peticion<Movimiento>(`/api/movements/${movimientoId}/exit`, { method: 'POST' }),
  activos: (placa?: string) => peticion<Movimiento[]>('/api/movements/active', { query: { plate: placa } }),
  movimiento: (id: number) => peticion<Movimiento>(`/api/movements/${id}`),
  historial: (filtros: { placa?: string; desde?: string; hasta?: string }) =>
    peticion<Movimiento[]>('/api/movements', {
      query: { plate: filtros.placa, from: filtros.desde, to: filtros.hasta },
    }),

  novedades: (movimientoId: number) => peticion<Novedad[]>(`/api/movements/${movimientoId}/incidents`),
  registrarNovedad: (movimientoId: number, tipo: TipoNovedad, descripcion: string) =>
    peticion<Novedad>(`/api/movements/${movimientoId}/incidents`, { method: 'POST', body: { tipo, descripcion } }),
};
