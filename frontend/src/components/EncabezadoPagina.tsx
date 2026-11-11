import type { ReactNode } from 'react';

/** Título y descripción de cada pantalla, con acciones opcionales a la derecha. */
export function EncabezadoPagina({ titulo, descripcion, acciones }: { titulo: string; descripcion?: string; acciones?: ReactNode }) {
  return (
    <header className="encabezado-pagina">
      <div>
        <h1>{titulo}</h1>
        {descripcion && <p>{descripcion}</p>}
      </div>
      {acciones && <div className="encabezado-pagina__acciones">{acciones}</div>}
    </header>
  );
}
