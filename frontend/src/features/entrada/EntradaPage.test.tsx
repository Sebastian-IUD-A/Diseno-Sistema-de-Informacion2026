import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import type { Celda, Vehiculo } from '../../api/types';
import { iniciarSesionSimulada, renderizar, respuestaJson, ruta, simularFetch } from '../../test/utilidades';
import { EntradaPage } from './EntradaPage';

const vehiculoBase: Vehiculo = {
  id: 1,
  placa: 'ABC123',
  tipo: 'CARRO',
  marca: 'Mazda',
  modelo: '3',
  color: 'Gris',
  estado: 'ACTIVO',
  autorizadoHasta: '2027-01-01',
  autorizacionVigente: true,
  diasParaVencer: 54,
  cliente: {
    id: 1,
    tipoDocumento: 'CC',
    numeroDocumento: '1020304050',
    nombreCompleto: 'Andrea Gómez Restrepo',
    telefono: '3001234567',
    correo: null,
    estado: 'ACTIVO',
  },
  dentro: false,
  movimientoAbiertoId: null,
  celdaActual: null,
  fechaHoraEntradaActual: null,
  puedeIngresar: true,
  motivoNoIngreso: null,
};

const celdas: Celda[] = [
  { id: 1, codigo: 'A-01', zona: 'Zona A', tipoVehiculo: 'CARRO', estado: 'DISPONIBLE', placaActual: null, movimientoId: null },
  { id: 3, codigo: 'A-03', zona: 'Zona A', tipoVehiculo: 'CARRO', estado: 'DISPONIBLE', placaActual: null, movimientoId: null },
];

describe('EntradaPage', () => {
  beforeEach(() => iniciarSesionSimulada('OPERADOR'));

  it('autorización vencida: muestra el motivo y no ofrece celdas', async () => {
    const vencido: Vehiculo = {
      ...vehiculoBase,
      placa: 'MNO345',
      autorizacionVigente: false,
      diasParaVencer: -10,
      puedeIngresar: false,
      motivoNoIngreso: { codigo: 'AUTORIZACION_VENCIDA', mensaje: 'La mensualidad del vehículo MNO345 venció el 29/10/2026.' },
    };
    const fetchMock = simularFetch([ruta('GET', /\/api\/vehicles\/MNO345$/, () => respuestaJson(vencido))]);
    renderizar(<EntradaPage />);

    await userEvent.type(screen.getByLabelText('Placa del vehículo'), 'mno345');
    await userEvent.click(screen.getByRole('button', { name: /verificar/i }));

    expect(await screen.findByText(/venció el 29\/10\/2026/)).toBeInTheDocument();
    expect(screen.getByText('Mensualidad vencida')).toBeInTheDocument();
    expect(screen.queryByRole('radio')).not.toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/api/cells/available'))).toBe(false);
  });

  it('placa inexistente: muestra el error del backend', async () => {
    simularFetch([
      ruta('GET', /\/api\/vehicles\//, () =>
        respuestaJson({ status: 404, codigo: 'VEHICULO_NO_ENCONTRADO', mensaje: 'No existe un vehículo registrado con la placa ZZZ999.' }, 404),
      ),
    ]);
    renderizar(<EntradaPage />);

    await userEvent.type(screen.getByLabelText('Placa del vehículo'), 'ZZZ999');
    await userEvent.click(screen.getByRole('button', { name: /verificar/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No existe un vehículo registrado con la placa ZZZ999.');
  });

  it('entrada correcta: elige celda, confirma y envía solo placa y celda (sin usuario)', async () => {
    const fetchMock = simularFetch([
      ruta('GET', /\/api\/vehicles\/ABC123$/, () => respuestaJson(vehiculoBase)),
      ruta('GET', /\/api\/cells\/available\?type=CARRO$/, () => respuestaJson(celdas)),
      ruta('POST', /\/api\/movements\/entries$/, () =>
        respuestaJson(
          {
            id: 77,
            placa: 'ABC123',
            tipoVehiculo: 'CARRO',
            descripcionVehiculo: 'Mazda · 3 · Gris',
            clienteNombre: 'Andrea Gómez Restrepo',
            celdaId: 3,
            celdaCodigo: 'A-03',
            zona: 'Zona A',
            fechaHoraEntrada: '2026-11-08T10:00:00',
            fechaHoraSalida: null,
            usuarioEntrada: 'Carlos Operador',
            usuarioSalida: null,
            abierto: true,
            minutosPermanencia: 0,
            cantidadNovedades: 0,
          },
          201,
        ),
      ),
    ]);
    renderizar(<EntradaPage />);

    await userEvent.type(screen.getByLabelText('Placa del vehículo'), 'abc123');
    await userEvent.click(screen.getByRole('button', { name: /verificar/i }));

    const botonRegistrar = await screen.findByRole('button', { name: 'Registrar entrada' });
    expect(botonRegistrar).toBeDisabled(); // sin celda elegida no se puede continuar

    await userEvent.click(screen.getByRole('radio', { name: 'A-03' }));
    await userEvent.click(botonRegistrar);

    const dialogo = await screen.findByRole('dialog', { name: 'Confirmar entrada' });
    expect(dialogo).toHaveTextContent('ABC123');
    expect(dialogo).toHaveTextContent('A-03');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Sí, registrar entrada' }));

    expect(await screen.findByText('Entrada registrada')).toBeInTheDocument();
    const post = fetchMock.mock.calls.find(([, init]) => (init as RequestInit)?.method === 'POST');
    expect(JSON.parse((post![1] as RequestInit).body as string)).toEqual({ placa: 'ABC123', celdaId: 3 });
  });

  it('si la celda fue ocupada por otro operador (409) muestra el error y recarga las celdas', async () => {
    let consultasCeldas = 0;
    simularFetch([
      ruta('GET', /\/api\/vehicles\/ABC123$/, () => respuestaJson(vehiculoBase)),
      ruta('GET', /\/api\/cells\/available/, () => {
        consultasCeldas++;
        return respuestaJson(consultasCeldas === 1 ? celdas : celdas.slice(1));
      }),
      ruta('POST', /\/api\/movements\/entries$/, () =>
        respuestaJson({ status: 409, codigo: 'CELDA_OCUPADA', mensaje: 'La celda A-01 ya está ocupada. Seleccione otra celda.' }, 409),
      ),
    ]);
    renderizar(<EntradaPage />);

    await userEvent.type(screen.getByLabelText('Placa del vehículo'), 'ABC123');
    await userEvent.click(screen.getByRole('button', { name: /verificar/i }));
    await userEvent.click(await screen.findByRole('radio', { name: 'A-01' }));
    await userEvent.click(screen.getByRole('button', { name: 'Registrar entrada' }));
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Sí, registrar entrada' }));

    expect(await screen.findByText('La celda A-01 ya está ocupada. Seleccione otra celda.')).toBeInTheDocument();
    expect(await screen.findByRole('radio', { name: 'A-03' })).toBeInTheDocument();
    expect(screen.queryByRole('radio', { name: 'A-01' })).not.toBeInTheDocument();
  });
});
