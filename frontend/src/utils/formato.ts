import type { EstadoCelda, TipoNovedad, TipoVehiculo } from '../api/types';

/** Utilidades de presentación: fechas, duraciones y etiquetas legibles en español. */

const formatoFechaHora = new Intl.DateTimeFormat('es-CO', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: true,
});

const formatoFecha = new Intl.DateTimeFormat('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });

/**
 * El backend envía fechas locales sin zona ("2026-11-08T10:30:00"), ya en hora de Colombia.
 * Se interpretan tal cual (sin conversión de zona) para mostrarlas.
 */
export function aFechaLocal(iso: string): Date {
  const [fecha, hora = '00:00:00'] = iso.split('T');
  const [a, m, d] = fecha.split('-').map(Number);
  const [h, min, s] = hora.split(':').map((v) => Number.parseFloat(v));
  return new Date(a, (m ?? 1) - 1, d ?? 1, h || 0, min || 0, Math.floor(s || 0));
}

export function fechaHora(iso: string | null | undefined): string {
  if (!iso) return '—';
  return formatoFechaHora.format(aFechaLocal(iso));
}

export function fecha(iso: string | null | undefined): string {
  if (!iso) return '—';
  return formatoFecha.format(aFechaLocal(iso));
}

/** 135 → "2 h 15 min"; 1500 → "1 d 1 h". */
export function duracion(minutos: number): string {
  if (!Number.isFinite(minutos) || minutos < 1) return 'menos de 1 min';
  const dias = Math.floor(minutos / 1440);
  const horas = Math.floor((minutos % 1440) / 60);
  const mins = Math.floor(minutos % 60);
  if (dias > 0) return horas > 0 ? `${dias} d ${horas} h` : `${dias} d`;
  if (horas > 0) return mins > 0 ? `${horas} h ${mins} min` : `${horas} h`;
  return `${mins} min`;
}

/** RN-01 en el cliente (solo para mostrar/enviar ordenado; el backend vuelve a normalizar). */
export function normalizarPlaca(placa: string): string {
  return placa.trim().toUpperCase();
}

export const etiquetaTipoVehiculo: Record<TipoVehiculo, string> = {
  CARRO: 'Carro',
  MOTO: 'Moto',
};

export const etiquetaEstadoCelda: Record<EstadoCelda, string> = {
  DISPONIBLE: 'Disponible',
  OCUPADA: 'Ocupada',
  FUERA_SERVICIO: 'Fuera de servicio',
};

export const etiquetaTipoNovedad: Record<TipoNovedad, string> = {
  DANO_VEHICULO: 'Daño en vehículo',
  OBJETO_OLVIDADO: 'Objeto olvidado',
  INCIDENTE_SEGURIDAD: 'Incidente de seguridad',
  OBSERVACION: 'Observación',
  OTRO: 'Otro',
};

export function hoyIso(): string {
  const d = new Date();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}
