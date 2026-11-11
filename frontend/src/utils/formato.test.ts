import { describe, expect, it } from 'vitest';
import { aFechaLocal, duracion, normalizarPlaca } from './formato';

describe('formato', () => {
  it('normaliza placas a mayúsculas sin espacios externos (RN-01)', () => {
    expect(normalizarPlaca('  abc123 ')).toBe('ABC123');
  });

  it('muestra duraciones legibles', () => {
    expect(duracion(0)).toBe('menos de 1 min');
    expect(duracion(45)).toBe('45 min');
    expect(duracion(120)).toBe('2 h');
    expect(duracion(135)).toBe('2 h 15 min');
    expect(duracion(1500)).toBe('1 d 1 h');
  });

  it('interpreta las fechas del backend como hora local, sin cambiar de zona', () => {
    const d = aFechaLocal('2026-11-08T10:30:15.123456');
    expect(d.getFullYear()).toBe(2026);
    expect(d.getMonth()).toBe(10);
    expect(d.getDate()).toBe(8);
    expect(d.getHours()).toBe(10);
    expect(d.getMinutes()).toBe(30);
  });
});
