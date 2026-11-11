import { describe, expect, it, vi } from 'vitest';
import { ApiError, peticion, registrarOyenteNoAutorizado } from './client';
import { respuestaJson, simularFetch } from '../test/utilidades';

describe('cliente HTTP', () => {
  it('envía el token en la cabecera Authorization', async () => {
    sessionStorage.setItem('autos-colombia.token', 'abc');
    const fetchMock = simularFetch([() => respuestaJson({ ok: true })]);

    await peticion('/api/dashboard/summary');

    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer abc');
  });

  it('convierte el error del backend en ApiError con código y mensaje', async () => {
    simularFetch([() => respuestaJson({ status: 409, codigo: 'CELDA_OCUPADA', mensaje: 'La celda A-01 ya está ocupada.' }, 409)]);

    const error = (await peticion('/api/movements/entries', { method: 'POST', body: {} }).catch((e) => e)) as ApiError;

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(409);
    expect(error.codigo).toBe('CELDA_OCUPADA');
    expect(error.message).toBe('La celda A-01 ya está ocupada.');
  });

  it('avisa a la sesión cuando el token ya no es válido (401)', async () => {
    sessionStorage.setItem('autos-colombia.token', 'vencido');
    const oyente = vi.fn();
    registrarOyenteNoAutorizado(oyente);
    simularFetch([() => respuestaJson({ status: 401, codigo: 'NO_AUTENTICADO', mensaje: 'Sesión expirada' }, 401)]);

    await peticion('/api/movements/active').catch(() => undefined);

    expect(oyente).toHaveBeenCalledOnce();
    registrarOyenteNoAutorizado(null);
  });

  it('omite parámetros vacíos en la URL', async () => {
    const fetchMock = simularFetch([() => respuestaJson([])]);
    await peticion('/api/movements', { query: { plate: 'ABC123', from: '', to: undefined } });
    expect(fetchMock.mock.calls[0][0]).toBe('/api/movements?plate=ABC123');
  });

  it('informa cuando no hay conexión con el servidor', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Promise.reject(new TypeError('Failed to fetch'))));
    const error = (await peticion('/api/auth/me').catch((e) => e)) as ApiError;
    expect(error.codigo).toBe('SIN_CONEXION');
  });
});
