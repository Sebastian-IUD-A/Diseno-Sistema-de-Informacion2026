import type { EstadoCelda } from '../api/types';
import { etiquetaEstadoCelda } from '../utils/formato';

type Tono = 'verde' | 'rojo' | 'gris' | 'ambar' | 'azul';

/** Etiqueta de color para estados. El texto siempre acompaña al color (accesibilidad). */
export function Insignia({ tono, children }: { tono: Tono; children: string }) {
  return <span className={`insignia insignia--${tono}`}>{children}</span>;
}

const tonoCelda: Record<EstadoCelda, Tono> = {
  DISPONIBLE: 'verde',
  OCUPADA: 'ambar',
  FUERA_SERVICIO: 'gris',
};

export function InsigniaCelda({ estado }: { estado: EstadoCelda }) {
  return <Insignia tono={tonoCelda[estado]}>{etiquetaEstadoCelda[estado]}</Insignia>;
}
