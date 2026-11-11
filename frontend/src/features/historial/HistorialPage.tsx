import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/endpoints';
import type { Movimiento } from '../../api/types';
import { EncabezadoPagina } from '../../components/EncabezadoPagina';
import { Alerta, Cargando, Vacio } from '../../components/Estados';
import { Insignia } from '../../components/Insignia';
import { duracion, fechaHora } from '../../utils/formato';

/** Historial de permanencias por placa y rango de fechas de entrada. */
export function HistorialPage() {
  const [placa, setPlaca] = useState('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const [errorFiltro, setErrorFiltro] = useState<string | null>(null);

  const [datos, setDatos] = useState<Movimiento[] | null>(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function consultar(e?: FormEvent) {
    e?.preventDefault();
    if (desde && hasta && desde > hasta) {
      setErrorFiltro('La fecha inicial no puede ser posterior a la final.');
      return;
    }
    setErrorFiltro(null);
    setCargando(true);
    setError(null);
    try {
      setDatos(await api.historial({ placa: placa.trim().toUpperCase() || undefined, desde: desde || undefined, hasta: hasta || undefined }));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo consultar el historial.');
    } finally {
      setCargando(false);
    }
  }

  function limpiar() {
    setPlaca('');
    setDesde('');
    setHasta('');
    setDatos(null);
    setError(null);
    setErrorFiltro(null);
  }

  return (
    <>
      <EncabezadoPagina titulo="Historial de movimientos" descripcion="Consulte entradas y salidas por placa y rango de fechas de entrada." />

      <section className="tarjeta">
        <form className="filtros filtros--formulario" onSubmit={consultar} noValidate>
          <div className="campo">
            <label htmlFor="h-placa">Placa (exacta)</label>
            <input id="h-placa" value={placa} onChange={(e) => setPlaca(e.target.value.toUpperCase())} placeholder="Todas" maxLength={10} />
          </div>
          <div className="campo">
            <label htmlFor="h-desde">Desde</label>
            <input id="h-desde" type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
          </div>
          <div className="campo">
            <label htmlFor="h-hasta">Hasta</label>
            <input id="h-hasta" type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} aria-invalid={!!errorFiltro} />
          </div>
          <div className="filtros__botones">
            <button type="submit" className="boton boton--primario" disabled={cargando}>
              {cargando ? 'Consultando…' : 'Consultar'}
            </button>
            <button type="button" className="boton boton--secundario" onClick={limpiar}>
              Limpiar
            </button>
          </div>
        </form>
        {errorFiltro && <Alerta tipo="error">{errorFiltro}</Alerta>}
      </section>

      {error && <Alerta tipo="error">{error}</Alerta>}
      {cargando && <Cargando texto="Consultando historial…" />}
      {!cargando && datos === null && !error && (
        <Vacio titulo="Defina los filtros y pulse Consultar" texto="Puede dejar los filtros vacíos para ver los movimientos más recientes (máx. 500)." />
      )}
      {!cargando && datos && datos.length === 0 && <Vacio titulo="Sin resultados" texto="No hay movimientos con esos filtros." />}

      {!cargando && datos && datos.length > 0 && (
        <section className="tarjeta">
          <div className="tarjeta__cabecera">
            <h2 className="tarjeta__titulo">{datos.length} movimiento(s)</h2>
          </div>
          <div className="tabla-contenedor">
            <table className="tabla">
              <thead>
                <tr>
                  <th scope="col">Placa</th>
                  <th scope="col">Celda</th>
                  <th scope="col">Entrada</th>
                  <th scope="col">Salida</th>
                  <th scope="col">Permanencia</th>
                  <th scope="col">Operadores</th>
                  <th scope="col">Novedades</th>
                </tr>
              </thead>
              <tbody>
                {datos.map((m) => (
                  <tr key={m.id}>
                    <td>
                      <span className="placa">{m.placa}</span>
                    </td>
                    <td>{m.celdaCodigo}</td>
                    <td>{fechaHora(m.fechaHoraEntrada)}</td>
                    <td>{m.abierto ? <Insignia tono="ambar">Dentro</Insignia> : fechaHora(m.fechaHoraSalida)}</td>
                    <td>{duracion(m.minutosPermanencia)}</td>
                    <td className="texto-pequeno">
                      Entrada: {m.usuarioEntrada}
                      <br />
                      Salida: {m.usuarioSalida ?? '—'}
                    </td>
                    <td>
                      <Link className="enlace" to={`/novedades?movimiento=${m.id}`}>
                        {m.cantidadNovedades} · ver
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </>
  );
}
