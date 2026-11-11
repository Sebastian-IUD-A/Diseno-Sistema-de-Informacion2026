import type { ReactNode } from 'react';
import { Icono } from './Icono';

/** Componentes reutilizables para los estados de una pantalla: cargando, vacío y mensajes. */

export function Cargando({ texto = 'Cargando…' }: { texto?: string }) {
  return (
    <div className="estado-cargando" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <span>{texto}</span>
    </div>
  );
}

export function Vacio({ titulo, texto, accion }: { titulo: string; texto?: string; accion?: ReactNode }) {
  return (
    <div className="estado-vacio">
      <Icono nombre="vacio" tamano={36} />
      <p className="estado-vacio__titulo">{titulo}</p>
      {texto && <p className="estado-vacio__texto">{texto}</p>}
      {accion}
    </div>
  );
}

export type TipoAlerta = 'exito' | 'error' | 'aviso' | 'info';

export function Alerta({
  tipo,
  titulo,
  children,
  onCerrar,
}: {
  tipo: TipoAlerta;
  titulo?: string;
  children?: ReactNode;
  onCerrar?: () => void;
}) {
  const icono = tipo === 'exito' ? 'check' : tipo === 'error' ? 'alerta' : tipo === 'aviso' ? 'alerta' : 'info';
  return (
    <div className={`alerta alerta--${tipo}`} role={tipo === 'error' ? 'alert' : 'status'}>
      <Icono nombre={icono} tamano={18} />
      <div className="alerta__cuerpo">
        {titulo && <strong className="alerta__titulo">{titulo}</strong>}
        {children && <div>{children}</div>}
      </div>
      {onCerrar && (
        <button type="button" className="alerta__cerrar" onClick={onCerrar} aria-label="Cerrar mensaje">
          ×
        </button>
      )}
    </div>
  );
}
