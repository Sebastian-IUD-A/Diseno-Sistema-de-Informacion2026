import type { Rol } from '../api/types';

/** Menú lateral. Cada opción indica qué roles la ven (debe coincidir con SecurityConfig del backend). */
export interface OpcionMenu {
  ruta: string;
  etiqueta: string;
  icono: string;
  roles: Rol[];
}

const AMBOS: Rol[] = ['ADMINISTRADOR', 'OPERADOR'];

export const opcionesMenu: OpcionMenu[] = [
  { ruta: '/', etiqueta: 'Inicio', icono: 'inicio', roles: AMBOS },
  { ruta: '/entrada', etiqueta: 'Registrar entrada', icono: 'entrada', roles: ['OPERADOR'] },
  { ruta: '/salida', etiqueta: 'Registrar salida', icono: 'salida', roles: ['OPERADOR'] },
  { ruta: '/ocupacion', etiqueta: 'Ocupación actual', icono: 'ocupacion', roles: AMBOS },
  { ruta: '/historial', etiqueta: 'Historial', icono: 'historial', roles: ['ADMINISTRADOR'] },
  { ruta: '/novedades', etiqueta: 'Novedades', icono: 'novedades', roles: AMBOS },
  { ruta: '/celdas', etiqueta: 'Administrar celdas', icono: 'celdas', roles: ['ADMINISTRADOR'] },
];

export function opcionesPara(rol: Rol): OpcionMenu[] {
  return opcionesMenu.filter((o) => o.roles.includes(rol));
}
