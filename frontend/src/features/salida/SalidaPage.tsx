import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../../api/endpoints';
import type { Movimiento } from '../../api/types';
import { BuscadorPlaca } from '../../components/BuscadorPlaca';
import { DialogoConfirmacion } from '../../components/DialogoConfirmacion';
import { EncabezadoPagina } from '../../components/EncabezadoPagina';
import { Alerta, Cargando, Vacio } from '../../components/Estados';
import { useConsulta } from '../../hooks/useConsulta';
import { duracion, etiquetaTipoVehiculo, fechaHora } from '../../utils/formato';

/**
 * Flujo de salida: localizar el movimiento abierto, revisar datos, confirmar.
 * El backend cierra el movimiento y libera la celda en una sola transacción (RN-05).
 */
export function SalidaPage() {
  const [parametros] = useSearchParams();
  const placaInicial = parametros.get('placa') ?? '';

  const activos = useConsulta(() => api.activos(), []);
  const [seleccionado, setSeleccionado] = useState<Movimiento | null>(null);
  const [buscando, setBuscando] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);
  const [confirmando, setConfirmando] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cerrado, setCerrado] = useState<Movimiento | null>(null);

  async function buscar(placa: string) {
    setBuscando(true);
    setAviso(null);
    setError(null);
    setCerrado(null);
    setSeleccionado(null);
    try {
      const encontrados = await api.activos(placa);
      const exacto = encontrados.find((m) => m.placa === placa);
      if (exacto) setSeleccionado(exacto);
      else if (encontrados.length === 1) setSeleccionado(encontrados[0]);
      else if (encontrados.length === 0) setAviso(`No hay ningún vehículo dentro con la placa ${placa}.`);
      else setAviso(`Hay ${encontrados.length} vehículos dentro cuya placa contiene "${placa}". Selecciónelo en la tabla.`);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo buscar el vehículo.');
    } finally {
      setBuscando(false);
    }
  }

  useEffect(() => {
    if (placaInicial) void buscar(placaInicial.trim().toUpperCase());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [placaInicial]);

  async function confirmar() {
    if (!seleccionado) return;
    setGuardando(true);
    setError(null);
    try {
      const resultado = await api.registrarSalida(seleccionado.id);
      setCerrado(resultado);
      setSeleccionado(null);
      setConfirmando(false);
      void activos.recargar();
    } catch (e) {
      setConfirmando(false);
      setError(e instanceof Error ? e.message : 'No se pudo registrar la salida.');
      void activos.recargar();
    } finally {
      setGuardando(false);
    }
  }

  return (
    <>
      <EncabezadoPagina titulo="Registrar salida" descripcion="Localice el vehículo, verifique los datos y confirme la salida." />

      {cerrado && (
        <Alerta tipo="exito" titulo="Salida registrada" onCerrar={() => setCerrado(null)}>
          {cerrado.placa} salió de la celda <strong>{cerrado.celdaCodigo}</strong> a las {fechaHora(cerrado.fechaHoraSalida)}.
          Permanencia: {duracion(cerrado.minutosPermanencia)}. La celda quedó disponible.
        </Alerta>
      )}

      <section className="tarjeta">
        <BuscadorPlaca key={placaInicial} onBuscar={buscar} buscando={buscando} valorInicial={placaInicial} />
      </section>

      {aviso && <Alerta tipo="aviso">{aviso}</Alerta>}
      {error && <Alerta tipo="error">{error}</Alerta>}

      {seleccionado && (
        <section className="tarjeta ficha" aria-label="Movimiento seleccionado">
          <div className="ficha__cabecera">
            <span className="placa placa--grande">{seleccionado.placa}</span>
            <span className="texto-suave">{etiquetaTipoVehiculo[seleccionado.tipoVehiculo]} · {seleccionado.descripcionVehiculo}</span>
          </div>
          <dl className="ficha__datos">
            <div>
              <dt>Cliente</dt>
              <dd>{seleccionado.clienteNombre ?? '—'}</dd>
            </div>
            <div>
              <dt>Celda</dt>
              <dd>
                {seleccionado.celdaCodigo} · {seleccionado.zona}
              </dd>
            </div>
            <div>
              <dt>Entrada</dt>
              <dd>{fechaHora(seleccionado.fechaHoraEntrada)}</dd>
            </div>
            <div>
              <dt>Registró la entrada</dt>
              <dd>{seleccionado.usuarioEntrada}</dd>
            </div>
            <div>
              <dt>Tiempo dentro</dt>
              <dd>{duracion(seleccionado.minutosPermanencia)}</dd>
            </div>
            <div>
              <dt>Novedades</dt>
              <dd>{seleccionado.cantidadNovedades}</dd>
            </div>
          </dl>
          <div className="acciones-formulario">
            <button type="button" className="boton boton--secundario" onClick={() => setSeleccionado(null)}>
              Cancelar
            </button>
            <button type="button" className="boton boton--primario" onClick={() => setConfirmando(true)}>
              Registrar salida
            </button>
          </div>
        </section>
      )}

      <section className="tarjeta">
        <div className="tarjeta__cabecera">
          <h2 className="tarjeta__titulo">Vehículos dentro</h2>
          <span className="texto-suave">{activos.datos ? `${activos.datos.length} en total` : ''}</span>
        </div>
        {activos.error && <Alerta tipo="error">{activos.error}</Alerta>}
        {activos.cargando && !activos.datos && <Cargando />}
        {activos.datos && activos.datos.length === 0 && <Vacio titulo="No hay vehículos dentro" />}
        {activos.datos && activos.datos.length > 0 && (
          <div className="tabla-contenedor">
            <table className="tabla">
              <thead>
                <tr>
                  <th scope="col">Placa</th>
                  <th scope="col">Celda</th>
                  <th scope="col">Entrada</th>
                  <th scope="col">Tiempo</th>
                  <th scope="col">
                    <span className="sr-only">Acción</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {activos.datos.map((m) => (
                  <tr key={m.id} className={seleccionado?.id === m.id ? 'tabla__fila--activa' : undefined}>
                    <td>
                      <span className="placa">{m.placa}</span>
                    </td>
                    <td>{m.celdaCodigo}</td>
                    <td>{fechaHora(m.fechaHoraEntrada)}</td>
                    <td>{duracion(m.minutosPermanencia)}</td>
                    <td className="tabla__acciones">
                      <button
                        type="button"
                        className="boton boton--pequeno boton--secundario"
                        onClick={() => {
                          setSeleccionado(m);
                          setAviso(null);
                          setCerrado(null);
                          window.scrollTo?.({ top: 0, behavior: 'smooth' });
                        }}
                      >
                        Seleccionar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <DialogoConfirmacion
        abierto={confirmando}
        titulo="Confirmar salida"
        textoConfirmar="Sí, registrar salida"
        procesando={guardando}
        onConfirmar={confirmar}
        onCancelar={() => setConfirmando(false)}
      >
        <p>
          ¿Registrar la salida de <strong>{seleccionado?.placa}</strong> y liberar la celda <strong>{seleccionado?.celdaCodigo}</strong>?
        </p>
        <p className="texto-suave texto-pequeno">Esta acción cierra la permanencia y no se puede deshacer.</p>
      </DialogoConfirmacion>
    </>
  );
}
