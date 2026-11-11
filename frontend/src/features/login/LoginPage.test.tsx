import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { LoginPage } from './LoginPage';
import { operador, renderizar, respuestaJson, ruta, simularFetch } from '../../test/utilidades';

describe('LoginPage', () => {
  it('tiene campos con etiqueta accesible', () => {
    renderizar(<LoginPage />, { ruta: '/login', patron: '/login' });
    expect(screen.getByLabelText('Correo electrónico')).toBeInTheDocument();
    expect(screen.getByLabelText('Contraseña')).toBeInTheDocument();
  });

  it('valida campos vacíos sin llamar al servidor', async () => {
    const fetchMock = simularFetch([]);
    renderizar(<LoginPage />, { ruta: '/login', patron: '/login' });

    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Ingrese su correo y contraseña.');
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('muestra el mensaje del backend cuando las credenciales son inválidas', async () => {
    simularFetch([
      ruta('POST', /\/api\/auth\/login$/, () =>
        respuestaJson({ status: 401, codigo: 'CREDENCIALES_INVALIDAS', mensaje: 'Correo o contraseña incorrectos.' }, 401),
      ),
    ]);
    renderizar(<LoginPage />, { ruta: '/login', patron: '/login' });

    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'operador@autoscolombia.co');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'mala');
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos.');
    expect(sessionStorage.getItem('autos-colombia.token')).toBeNull();
  });

  it('guarda el token al iniciar sesión correctamente', async () => {
    simularFetch([
      ruta('POST', /\/api\/auth\/login$/, () =>
        respuestaJson({ token: 'jwt-123', tipo: 'Bearer', expiraEn: '2030-01-01T00:00:00Z', usuario: operador }),
      ),
    ]);
    renderizar(<LoginPage />, { ruta: '/login', patron: '/login' });

    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'operador@autoscolombia.co');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'Operador123*');
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }));

    await waitFor(() => expect(sessionStorage.getItem('autos-colombia.token')).toBe('jwt-123'));
    expect(screen.queryByLabelText('Contraseña')).not.toBeInTheDocument(); // ya salió del login
  });
});
