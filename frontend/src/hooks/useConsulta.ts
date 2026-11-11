import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Hook para cargar datos de la API con estados de carga y error.
 * Ignora respuestas que llegan tarde (si el usuario cambió el filtro mientras tanto).
 */
export function useConsulta<T>(consulta: () => Promise<T>, dependencias: unknown[], activo = true) {
  const [datos, setDatos] = useState<T | null>(null);
  const [cargando, setCargando] = useState<boolean>(activo);
  const [error, setError] = useState<string | null>(null);
  const solicitudActual = useRef(0);

  const ejecutar = useCallback(async () => {
    const numero = ++solicitudActual.current;
    setCargando(true);
    setError(null);
    try {
      const resultado = await consulta();
      if (numero === solicitudActual.current) setDatos(resultado);
    } catch (e) {
      if (numero === solicitudActual.current) setError(e instanceof Error ? e.message : 'Error desconocido');
    } finally {
      if (numero === solicitudActual.current) setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencias);

  useEffect(() => {
    if (activo) void ejecutar();
  }, [ejecutar, activo]);

  return { datos, cargando, error, recargar: ejecutar, setDatos };
}
