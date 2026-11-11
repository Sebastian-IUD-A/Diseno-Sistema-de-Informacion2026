import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import type { Movimiento } from '../../api/types';
import { iniciarSesionSimulada, renderizar, respuestaJson, ruta, simularFetch } from '../../test/utilidades';
import { SalidaPage } from './SalidaPage';

const abierto: Movimiento = {
  id: 4,
  placa: 'JKL012',
  tipoVehiculo: 'CARRO',
  descripcionVehiculo: 'Chevrolet · Onix · Rojo',
  clienteNombre: 'Logística Andina S.A.S.',
  celdaId: 2,
  celdaCodigo: 'A-02',
  zona: 'Zona A - Cubierta',
  fechaHoraEntrada: '2026-11-08T08:00:00',
  fechaHoraSalida: null,
  usuarioEntrada: 'Carlos Operador',
  usuarioSalida: null,
  abierto: true,
  minutosPermanencia: 120,
  cantidadNovedades: 1,
};

describe('SalidaPage', () => {
  beforeEach(() => iniciarSesionSimulada('OPERADOR'));

  it('busca la placa, confirma y registra la salida', async () => {
    let salidaRegistrada = false;
    const fetchMock = simularFetch([
      ruta('GET', /\/api\/movements\/active/, () => respuestaJson(salidaRegistrada ? [] : [abierto])),
      ruta('POST', /\/api\/movements\/4\/exit$/, () => {
        salidaRegistrada = true;
        return respuestaJson({ ...abierto, abierto: false, fechaHoraSalida: '2026-11-08T10:00:00', usuarioSalida: 'Carlos Operador' });
      }),
    ]);
    renderizar(<SalidaPage />, { ruta: '/salida?placa=jkl012' });

    const ficha = await screen.findByRole('region', { name: 'Movimiento seleccionado' });
    expect(ficha).toHaveTextContent('A-02');
    expect(ficha).toHaveTextContent('2 h');

    await userEvent.click(within(ficha).getByRole('button', { name: 'Registrar salida' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Confirmar salida' });
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Sí, registrar salida' }));

    expect(await screen.findByText('Salida registrada')).toBeInTheDocument();
    expect(screen.getByText(/La celda quedó disponible/)).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([url, init]) => String(url).endsWith('/api/movements/4/exit') && (init as RequestInit).method === 'POST')).toBe(true);
  });

  it('cancelar en el diálogo no registra nada', async () => {
    const fetchMock = simularFetch([ruta('GET', /\/api\/movements\/active/, () => respuestaJson([abierto]))]);
    renderizar(<SalidaPage />, { ruta: '/salida?placa=JKL012' });

    const ficha = await screen.findByRole('region', { name: 'Movimiento seleccionado' });
    await userEvent.click(within(ficha).getByRole('button', { name: 'Registrar salida' }));
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancelar' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([, init]) => (init as RequestInit)?.method === 'POST')).toBe(false);
  });

  it('avisa cuando la placa no está dentro', async () => {
    simularFetch([ruta('GET', /\/api\/movements\/active/, () => respuestaJson([]))]);
    renderizar(<SalidaPage />, { ruta: '/salida?placa=XYZ999' });
    expect(await screen.findByText('No hay ningún vehículo dentro con la placa XYZ999.')).toBeInTheDocument();
  });
});
