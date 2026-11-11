import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../../api/endpoints';
import type { TipoNovedad } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import { EncabezadoPagina } from '../../components/EncabezadoPagina';
import { Alerta, Cargando, Vacio } from '../../components/Estados';
import { Insignia } from '../../components/Insignia';
import { useConsulta } from '../../hooks/useConsulta';
import { duracion, etiquetaTipoNovedad, fechaHora } from '../../utils/formato';

const TIPOS = Object.keys(etiquetaTipoNovedad) as TipoNovedad[];
const MAX = 500;

/** Novedades de un movimiento (RN-07). Se elige el movimiento y se consultan/registran sus novedades. */
export function NovedadesPage() {
  const [parametros, setParametros] = useSearchParams();
  const movimientoId = Number(parametros.get('movimiento')) || null;

  return (
    <>
      <EncabezadoPagina
        titulo="Novedades"
        descripcion="Daños, objetos olvidados u observaciones asociadas a una permanencia."
        acciones={
          movimientoId ? (
            <button type="button" className="boton boton--secundario" onClick={() => setParametros({})}>
              Elegir otro movimiento
            </button>
          ) : undefined
        }
      />
      {movimientoId ? <DetalleNovedades movimientoId={movimientoId} /> : <SelectorMovimiento onElegir={(id) => setParametros({ movimiento: String(id) })} />}
    </>
  );
}

function SelectorMovimiento({ onElegir }: { onElegir: (id: number) => void }) {
  const { tieneRol } = useAuth();
  const activos = useConsulta(() => api.activos(), []);
  const [id, setId] = useState('');

  function buscarPorId(e: FormEvent) {
    e.preventDefault();
    const numero = Number(id);
    if (Number.isInteger(numero) && numero > 0) onElegir(numero);
  }

  return (
    <>
      <section className="tarjeta">
        <h2 className="tarjeta__titulo">Vehículos dentro</h2>
        <p className="texto-suave">Seleccione la permanencia a la que pertenece la novedad.</p>
        {activos.error && <Alerta tipo="error">{activos.error}</Alerta>}
        {activos.cargando && !activos.datos && <Cargando />}
        {activos.datos && activos.datos.length === 0 && <Vacio titulo="No hay vehículos dentro" />}
        {activos.datos && activos.datos.length > 0 && (
          <ul className="lista-seleccion">
            {activos.datos.map((m) => (
              <li key={m.id}>
                <button type="button" className="lista-seleccion__item" onClick={() => onElegir(m.id)}>
                  <span className="placa">{m.placa}</span>
                  <span>Celda {m.celdaCodigo}</span>
                  <span className="texto-suave texto-pequeno">desde {fechaHora(m.fechaHoraEntrada)}</span>
                  <span className="texto-suave texto-pequeno">{m.cantidadNovedades} novedad(es)</span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="tarjeta">
        <form className="filtros" onSubmit={buscarPorId}>
          <div className="campo">
            <label htmlFor="mov-id">Número de movimiento</label>
            <input id="mov-id" inputMode="numeric" value={id} onChange={(e) => setId(e.target.value.replace(/\D/g, ''))} placeholder="Ej.: 12" />
          </div>
          <div className="filtros__botones">
            <button type="submit" className="boton boton--secundario" disabled={!id}>
              Abrir
            </button>
          </div>
        </form>
        {tieneRol('ADMINISTRADOR') && (
          <p className="texto-suave texto-pequeno">
            Para movimientos ya cerrados use el <Link className="enlace" to="/historial">Historial</Link> y el enlace de novedades de cada fila.
          </p>
        )}
      </section>
    </>
  );
}

function DetalleNovedades({ movimientoId }: { movimientoId: number }) {
  const { tieneRol } = useAuth();
  const puedeRegistrar = tieneRol('OPERADOR');
  const movimiento = useConsulta(() => api.movimiento(movimientoId), [movimientoId]);
  const novedades = useConsulta(() => api.novedades(movimientoId), [movimientoId]);

  const [tipo, setTipo] = useState<TipoNovedad>('OBSERVACION');
  const [descripcion, setDescripcion] = useState('');
  const [errorCampo, setErrorCampo] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [exito, setExito] = useState<string | null>(null);

  async function guardar(e: FormEvent) {
    e.preventDefault();
    setExito(null);
    setError(null);
    if (!descripcion.trim()) {
      setErrorCampo('La descripción es obligatoria.');
      return;
    }
    setErrorCampo(null);
    setGuardando(true);
    try {
      await api.registrarNovedad(movimientoId, tipo, descripcion.trim());
      setDescripcion('');
      setExito('Novedad registrada.');
      void novedades.recargar();
      void movimiento.recargar();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo registrar la novedad.');
    } finally {
      setGuardando(false);
    }
  }

  if (movimiento.cargando && !movimiento.datos) return <Cargando />;
  if (movimiento.error) return <Alerta tipo="error">{movimiento.error}</Alerta>;
  const m = movimiento.datos;
  if (!m) return null;

  return (
    <div className="dos-columnas dos-columnas--ancha-izq">
      <section className="tarjeta">
        <div className="tarjeta__cabecera">
          <h2 className="tarjeta__titulo">
            Movimiento #{m.id} · <span className="placa">{m.placa}</span>
          </h2>
          {m.abierto ? <Insignia tono="ambar">Dentro</Insignia> : <Insignia tono="gris">Cerrado</Insignia>}
        </div>
        <p className="texto-suave">
          Celda {m.celdaCodigo} · Entrada {fechaHora(m.fechaHoraEntrada)}
          {m.fechaHoraSalida ? ` · Salida ${fechaHora(m.fechaHoraSalida)}` : ''} · {duracion(m.minutosPermanencia)}
        </p>

        <h3 className="subtitulo">Novedades registradas</h3>
        {novedades.error && <Alerta tipo="error">{novedades.error}</Alerta>}
        {novedades.cargando && !novedades.datos && <Cargando />}
        {novedades.datos && novedades.datos.length === 0 && <Vacio titulo="Sin novedades" texto="Este movimiento no tiene novedades." />}
        {novedades.datos && novedades.datos.length > 0 && (
          <ol className="linea-tiempo">
            {novedades.datos.map((n) => (
              <li key={n.id} className="linea-tiempo__item">
                <div className="linea-tiempo__cabecera">
                  <Insignia tono={n.tipo === 'DANO_VEHICULO' || n.tipo === 'INCIDENTE_SEGURIDAD' ? 'rojo' : 'azul'}>
                    {etiquetaTipoNovedad[n.tipo]}
                  </Insignia>
                  <span className="texto-suave texto-pequeno">
                    {fechaHora(n.fechaHora)} · {n.usuario}
                  </span>
                </div>
                <p>{n.descripcion}</p>
              </li>
            ))}
          </ol>
        )}
      </section>

      <section className="tarjeta">
        <h2 className="tarjeta__titulo">Registrar novedad</h2>
        {!puedeRegistrar ? (
          <p className="texto-suave">Las novedades las registran los operadores. Como administrador puede consultarlas.</p>
        ) : (
          <form className="formulario-columna" onSubmit={guardar} noValidate>
            {exito && <Alerta tipo="exito" onCerrar={() => setExito(null)}>{exito}</Alerta>}
            {error && <Alerta tipo="error">{error}</Alerta>}
            <div className="campo">
              <label htmlFor="n-tipo">Tipo</label>
              <select id="n-tipo" value={tipo} onChange={(e) => setTipo(e.target.value as TipoNovedad)}>
                {TIPOS.map((t) => (
                  <option key={t} value={t}>
                    {etiquetaTipoNovedad[t]}
                  </option>
                ))}
              </select>
            </div>
            <div className="campo">
              <label htmlFor="n-descripcion">Descripción</label>
              <textarea
                id="n-descripcion"
                rows={4}
                maxLength={MAX}
                value={descripcion}
                onChange={(e) => setDescripcion(e.target.value)}
                aria-invalid={!!errorCampo}
                aria-describedby="n-ayuda"
              />
              <span id="n-ayuda" className={errorCampo ? 'campo__error' : 'campo__ayuda'}>
                {errorCampo ?? `${descripcion.length}/${MAX} caracteres`}
              </span>
            </div>
            <button type="submit" className="boton boton--primario" disabled={guardando}>
              {guardando ? 'Guardando…' : 'Registrar novedad'}
            </button>
          </form>
        )}
      </section>
    </div>
  );
}
