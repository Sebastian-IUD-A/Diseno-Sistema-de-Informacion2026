import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../../api/client';
import { api } from '../../api/endpoints';
import type { Celda, Movimiento, Vehiculo } from '../../api/types';
import { BuscadorPlaca } from '../../components/BuscadorPlaca';
import { DialogoConfirmacion } from '../../components/DialogoConfirmacion';
import { EncabezadoPagina } from '../../components/EncabezadoPagina';
import { Alerta, Cargando, Vacio } from '../../components/Estados';
import { FichaVehiculo } from '../../components/FichaVehiculo';
import { etiquetaTipoVehiculo, fechaHora } from '../../utils/formato';

/**
 * Flujo de entrada:
 * 1) buscar placa → 2) validar autorización y que no esté dentro → 3) elegir celda compatible
 * → 4) confirmar → 5) guardar. Todas las validaciones se repiten en el backend.
 */
export function EntradaPage() {
  const [vehiculo, setVehiculo] = useState<Vehiculo | null>(null);
  const [buscando, setBuscando] = useState(false);
  const [errorBusqueda, setErrorBusqueda] = useState<string | null>(null);

  const [celdas, setCeldas] = useState<Celda[] | null>(null);
  const [cargandoCeldas, setCargandoCeldas] = useState(false);
  const [celdaId, setCeldaId] = useState<number | null>(null);

  const [confirmando, setConfirmando] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [errorGuardar, setErrorGuardar] = useState<string | null>(null);
  const [registrado, setRegistrado] = useState<Movimiento | null>(null);

  async function cargarCeldas(v: Vehiculo) {
    setCargandoCeldas(true);
    try {
      setCeldas(await api.celdasDisponibles(v.tipo));
    } catch (e) {
      setErrorBusqueda(e instanceof Error ? e.message : 'No se pudieron cargar las celdas.');
    } finally {
      setCargandoCeldas(false);
    }
  }

  async function buscar(placa: string) {
    setBuscando(true);
    setErrorBusqueda(null);
    setErrorGuardar(null);
    setRegistrado(null);
    setVehiculo(null);
    setCeldas(null);
    setCeldaId(null);
    try {
      const v = await api.vehiculo(placa);
      setVehiculo(v);
      if (v.puedeIngresar) await cargarCeldas(v);
    } catch (e) {
      setErrorBusqueda(e instanceof Error ? e.message : 'No se pudo consultar la placa.');
    } finally {
      setBuscando(false);
    }
  }

  async function confirmar() {
    if (!vehiculo || celdaId == null) return;
    setGuardando(true);
    setErrorGuardar(null);
    try {
      const movimiento = await api.registrarEntrada(vehiculo.placa, celdaId);
      setRegistrado(movimiento);
      setConfirmando(false);
      setVehiculo(null);
      setCeldas(null);
      setCeldaId(null);
    } catch (e) {
      setConfirmando(false);
      setErrorGuardar(e instanceof Error ? e.message : 'No se pudo registrar la entrada.');
      // Si otra persona ocupó la celda al mismo tiempo, se recarga la lista para elegir otra.
      if (e instanceof ApiError && (e.status === 409 || e.status === 422) && vehiculo) {
        setCeldaId(null);
        await cargarCeldas(vehiculo);
      }
    } finally {
      setGuardando(false);
    }
  }

  const celdaElegida = celdas?.find((c) => c.id === celdaId) ?? null;
  const zonas = agruparPorZona(celdas ?? []);

  return (
    <>
      <EncabezadoPagina titulo="Registrar entrada" descripcion="Busque la placa, verifique la mensualidad y asigne una celda." />

      {registrado && (
        <Alerta tipo="exito" titulo="Entrada registrada" onCerrar={() => setRegistrado(null)}>
          {registrado.placa} ingresó a la celda <strong>{registrado.celdaCodigo}</strong> el {fechaHora(registrado.fechaHoraEntrada)}.
        </Alerta>
      )}

      <section className="tarjeta">
        <BuscadorPlaca onBuscar={buscar} buscando={buscando} textoBoton="Verificar" />
      </section>

      {errorBusqueda && <Alerta tipo="error">{errorBusqueda}</Alerta>}

      {vehiculo && (
        <>
          <FichaVehiculo vehiculo={vehiculo} />

          {!vehiculo.puedeIngresar && vehiculo.motivoNoIngreso && (
            <Alerta tipo={vehiculo.dentro ? 'aviso' : 'error'} titulo="No puede ingresar">
              {vehiculo.motivoNoIngreso.mensaje}{' '}
              {vehiculo.dentro && (
                <Link className="enlace" to={`/salida?placa=${vehiculo.placa}`}>
                  Ir a registrar salida
                </Link>
              )}
            </Alerta>
          )}

          {vehiculo.puedeIngresar && (
            <section className="tarjeta">
              <div className="tarjeta__cabecera">
                <h2 className="tarjeta__titulo">Celdas disponibles para {etiquetaTipoVehiculo[vehiculo.tipo].toLowerCase()}</h2>
                {celdas && <span className="texto-suave">{celdas.length} disponibles</span>}
              </div>

              {errorGuardar && <Alerta tipo="error">{errorGuardar}</Alerta>}
              {cargandoCeldas && <Cargando texto="Buscando celdas…" />}
              {celdas && celdas.length === 0 && (
                <Vacio titulo="No hay celdas disponibles" texto="Todas las celdas compatibles están ocupadas o fuera de servicio." />
              )}

              {celdas && celdas.length > 0 && (
                <fieldset className="selector-celdas">
                  <legend className="sr-only">Seleccione una celda</legend>
                  {Object.entries(zonas).map(([zona, lista]) => (
                    <div key={zona} className="selector-celdas__zona">
                      <h3>{zona}</h3>
                      <div className="selector-celdas__grilla">
                        {lista.map((c) => (
                          <label key={c.id} className={`celda-opcion ${celdaId === c.id ? 'celda-opcion--elegida' : ''}`}>
                            <input
                              type="radio"
                              name="celda"
                              value={c.id}
                              checked={celdaId === c.id}
                              onChange={() => setCeldaId(c.id)}
                            />
                            <span>{c.codigo}</span>
                          </label>
                        ))}
                      </div>
                    </div>
                  ))}
                </fieldset>
              )}

              <div className="acciones-formulario">
                <button
                  type="button"
                  className="boton boton--primario"
                  disabled={celdaId == null || guardando}
                  onClick={() => setConfirmando(true)}
                >
                  Registrar entrada
                </button>
              </div>
            </section>
          )}
        </>
      )}

      <DialogoConfirmacion
        abierto={confirmando}
        titulo="Confirmar entrada"
        textoConfirmar="Sí, registrar entrada"
        procesando={guardando}
        onConfirmar={confirmar}
        onCancelar={() => setConfirmando(false)}
      >
        <p>
          ¿Registrar la entrada del vehículo <strong>{vehiculo?.placa}</strong> en la celda{' '}
          <strong>{celdaElegida?.codigo}</strong> ({celdaElegida?.zona})?
        </p>
        <p className="texto-suave texto-pequeno">Se guardará la hora actual y su usuario como operador de entrada.</p>
      </DialogoConfirmacion>
    </>
  );
}

function agruparPorZona(celdas: Celda[]): Record<string, Celda[]> {
  return celdas.reduce<Record<string, Celda[]>>((acc, c) => {
    (acc[c.zona] ??= []).push(c);
    return acc;
  }, {});
}
