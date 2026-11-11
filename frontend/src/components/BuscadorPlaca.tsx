import { useState, type FormEvent } from 'react';
import { Icono } from './Icono';

/** Formulario de búsqueda por placa reutilizado en Entrada y Salida. */
export function BuscadorPlaca({
  onBuscar,
  buscando,
  valorInicial = '',
  textoBoton = 'Buscar',
}: {
  onBuscar: (placa: string) => void;
  buscando?: boolean;
  valorInicial?: string;
  textoBoton?: string;
}) {
  const [placa, setPlaca] = useState(valorInicial);
  const [error, setError] = useState<string | null>(null);

  function enviar(e: FormEvent) {
    e.preventDefault();
    const limpia = placa.trim().toUpperCase();
    if (!limpia) {
      setError('Ingrese una placa.');
      return;
    }
    if (!/^[A-Z0-9-]{3,10}$/.test(limpia)) {
      setError('La placa solo admite letras, números y guion (3 a 10 caracteres).');
      return;
    }
    setError(null);
    setPlaca(limpia);
    onBuscar(limpia);
  }

  return (
    <form className="buscador" onSubmit={enviar} noValidate role="search">
      <div className="campo campo--en-linea">
        <label htmlFor="placa">Placa del vehículo</label>
        <div className="buscador__fila">
          <input
            id="placa"
            className="input-placa"
            value={placa}
            onChange={(e) => setPlaca(e.target.value.toUpperCase())}
            placeholder="ABC123"
            maxLength={12}
            autoComplete="off"
            aria-invalid={!!error}
            aria-describedby={error ? 'placa-error' : undefined}
          />
          <button type="submit" className="boton boton--primario" disabled={buscando}>
            <Icono nombre="buscar" tamano={18} /> {buscando ? 'Buscando…' : textoBoton}
          </button>
        </div>
        {error && (
          <span id="placa-error" className="campo__error">
            {error}
          </span>
        )}
      </div>
    </form>
  );
}
