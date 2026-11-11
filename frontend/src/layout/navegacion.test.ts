import { describe, expect, it } from 'vitest';
import { opcionesPara } from './navegacion';

describe('menú por rol', () => {
  it('el OPERADOR ve entrada y salida, pero no historial ni celdas', () => {
    const rutas = opcionesPara('OPERADOR').map((o) => o.ruta);
    expect(rutas).toEqual(expect.arrayContaining(['/', '/entrada', '/salida', '/ocupacion', '/novedades']));
    expect(rutas).not.toContain('/historial');
    expect(rutas).not.toContain('/celdas');
  });

  it('el ADMINISTRADOR ve historial y celdas, pero no registra entradas ni salidas', () => {
    const rutas = opcionesPara('ADMINISTRADOR').map((o) => o.ruta);
    expect(rutas).toEqual(expect.arrayContaining(['/', '/ocupacion', '/historial', '/novedades', '/celdas']));
    expect(rutas).not.toContain('/entrada');
    expect(rutas).not.toContain('/salida');
  });
});
