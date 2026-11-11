/** Íconos SVG en línea (sin dependencias externas). Decorativos: aria-hidden. */

const trazos: Record<string, string> = {
  inicio: 'M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z',
  entrada: 'M10 17l5-5-5-5M15 12H3M14 3h5a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-5',
  salida: 'M14 17l5-5-5-5M19 12H7M10 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5',
  ocupacion: 'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z',
  historial: 'M12 7v5l3 2M3.05 11a9 9 0 1 1 .5 4M3 4v5h5',
  novedades: 'M12 9v4M12 17h.01M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z',
  celdas: 'M3 5h18M3 12h18M3 19h18M8 5v14M16 5v14',
  salir: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
  menu: 'M4 6h16M4 12h16M4 18h16',
  buscar: 'M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16zM21 21l-4.35-4.35',
  check: 'M20 6L9 17l-5-5',
  alerta: 'M12 8v5M12 16.5h.01M12 3l10 18H2z',
  info: 'M12 16v-5M12 8h.01M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z',
  vacio: 'M3 7l9-4 9 4-9 4-9-4zM3 7v10l9 4 9-4V7',
  recargar: 'M21 12a9 9 0 1 1-3-6.7L21 8M21 3v5h-5',
  carro: 'M5 17h14M6 17v2M18 17v2M3 13l2-6h14l2 6v4H3zM7 13h.01M17 13h.01',
  moto: 'M5 18a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM19 18a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM8 15h6l3-6h-4M10 9h3',
  mas: 'M12 5v14M5 12h14',
  editar: 'M4 20h4L19 9l-4-4L4 16zM14 6l4 4',
};

export function Icono({ nombre, tamano = 20 }: { nombre: keyof typeof trazos | string; tamano?: number }) {
  return (
    <svg
      width={tamano}
      height={tamano}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d={trazos[nombre] ?? trazos.info} />
    </svg>
  );
}
