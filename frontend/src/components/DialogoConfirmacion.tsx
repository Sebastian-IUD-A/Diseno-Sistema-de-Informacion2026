import { useEffect, useId, useRef, type ReactNode } from 'react';

/**
 * Ventana de confirmación para acciones críticas (registrar entrada/salida, poner fuera de servicio).
 * Accesible: role="dialog", aria-modal, título enlazado, Escape para cancelar y foco inicial en "Cancelar".
 */
export function DialogoConfirmacion({
  abierto,
  titulo,
  children,
  textoConfirmar = 'Confirmar',
  peligro = false,
  procesando = false,
  onConfirmar,
  onCancelar,
}: {
  abierto: boolean;
  titulo: string;
  children?: ReactNode;
  textoConfirmar?: string;
  peligro?: boolean;
  procesando?: boolean;
  onConfirmar: () => void;
  onCancelar: () => void;
}) {
  const idTitulo = useId();
  const botonCancelar = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!abierto) return;
    botonCancelar.current?.focus();
    const alPresionar = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !procesando) onCancelar();
    };
    document.addEventListener('keydown', alPresionar);
    return () => document.removeEventListener('keydown', alPresionar);
  }, [abierto, procesando, onCancelar]);

  if (!abierto) return null;

  return (
    <div className="dialogo-fondo" onMouseDown={(e) => e.target === e.currentTarget && !procesando && onCancelar()}>
      <div className="dialogo" role="dialog" aria-modal="true" aria-labelledby={idTitulo}>
        <h2 id={idTitulo} className="dialogo__titulo">
          {titulo}
        </h2>
        <div className="dialogo__cuerpo">{children}</div>
        <div className="dialogo__acciones">
          <button ref={botonCancelar} type="button" className="boton boton--secundario" onClick={onCancelar} disabled={procesando}>
            Cancelar
          </button>
          <button
            type="button"
            className={`boton ${peligro ? 'boton--peligro' : 'boton--primario'}`}
            onClick={onConfirmar}
            disabled={procesando}
          >
            {procesando ? 'Procesando…' : textoConfirmar}
          </button>
        </div>
      </div>
    </div>
  );
}
