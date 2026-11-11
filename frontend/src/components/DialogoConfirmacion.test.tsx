import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { DialogoConfirmacion } from './DialogoConfirmacion';

describe('DialogoConfirmacion', () => {
  it('es un diálogo accesible con foco inicial en Cancelar y se cierra con Escape', async () => {
    const onCancelar = vi.fn();
    render(
      <DialogoConfirmacion abierto titulo="Confirmar salida" onConfirmar={vi.fn()} onCancelar={onCancelar}>
        <p>¿Seguro?</p>
      </DialogoConfirmacion>,
    );

    expect(screen.getByRole('dialog', { name: 'Confirmar salida' })).toHaveAttribute('aria-modal', 'true');
    expect(screen.getByRole('button', { name: 'Cancelar' })).toHaveFocus();
    await userEvent.keyboard('{Escape}');
    expect(onCancelar).toHaveBeenCalled();
  });

  it('deshabilita los botones mientras procesa', () => {
    render(<DialogoConfirmacion abierto procesando titulo="X" onConfirmar={vi.fn()} onCancelar={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Procesando…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled();
  });
});
