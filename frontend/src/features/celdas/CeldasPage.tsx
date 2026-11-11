import { useMemo, useState, type FormEvent } from 'react';
import { api } from '../../api/endpoints';
import type { Celda, CeldaSolicitud, EstadoCelda, TipoVehiculo } from '../../api/types';
import { DialogoConfirmacion } from '../../components/DialogoConfirmacion';
import { EncabezadoPagina } from '../../components/EncabezadoPagina';
import { Alerta, Cargando, Vacio } from '../../components/Estados';
import { Icono } from '../../components/Icono';
import { InsigniaCelda } from '../../components/Insignia';
import { useConsulta } from '../../hooks/useConsulta';
import { etiquetaEstadoCelda, etiquetaTipoVehiculo } from '../../utils/formato';

type EstadoEditable = Exclude<EstadoCelda, 'OCUPADA'>;
const VACIA: CeldaSolicitud = { codigo: '', zona: '', tipoVehiculo: 'CARRO', estado: 'DISPONIBLE' };

/**
 * Administración de celdas (solo ADMINISTRADOR).
 * RN-09: una celda ocupada no se puede poner fuera de servicio; la UI lo impide y el backend lo vuelve a validar.
 */
export function CeldasPage() {
  const { datos, cargando, error, recargar } = useConsulta(() => api.celdas(), []);
  const [filtroEstado, setFiltroEstado] = useState<EstadoCelda | ''>('');
  const [filtroTipo, setFiltroTipo] = useState<TipoVehiculo | ''>('');
  const [editando, setEditando] = useState<Celda | 'nueva' | null>(null);
  const [cambioEstado, setCambioEstado] = useState<{ celda: Celda; nuevo: EstadoEditable } | null>(null);
  const [procesando, setProcesando] = useState(false);
  const [mensaje, setMensaje] = useState<{ tipo: 'exito' | 'error'; texto: string } | null>(null);

  const filtradas = useMemo(
    () => (datos ?? []).filter((c) => (!filtroEstado || c.estado === filtroEstado) && (!filtroTipo || c.tipoVehiculo === filtroTipo)),
    [datos, filtroEstado, filtroTipo],
  );

  async function aplicarCambioEstado() {
    if (!cambioEstado) return;
    const { celda, nuevo } = cambioEstado;
    setProcesando(true);
    try {
      await api.actualizarCelda(celda.id, { codigo: celda.codigo, zona: celda.zona, tipoVehiculo: celda.tipoVehiculo, estado: nuevo });
      setMensaje({ tipo: 'exito', texto: `La celda ${celda.codigo} quedó ${etiquetaEstadoCelda[nuevo].toLowerCase()}.` });
      void recargar();
    } catch (e) {
      setMensaje({ tipo: 'error', texto: e instanceof Error ? e.message : 'No se pudo actualizar la celda.' });
      void recargar();
    } finally {
      setProcesando(false);
      setCambioEstado(null);
    }
  }

  return (
    <>
      <EncabezadoPagina
        titulo="Administrar celdas"
        descripcion="Cree celdas, edite sus datos y gestione las que están fuera de servicio."
        acciones={
          <button type="button" className="boton boton--primario" onClick={() => setEditando('nueva')}>
            <Icono nombre="mas" tamano={18} /> Nueva celda
          </button>
        }
      />

      {mensaje && (
        <Alerta tipo={mensaje.tipo} onCerrar={() => setMensaje(null)}>
          {mensaje.texto}
        </Alerta>
      )}

      {editando && (
        <FormularioCelda
          celda={editando === 'nueva' ? null : editando}
          onCancelar={() => setEditando(null)}
          onGuardado={(texto) => {
            setEditando(null);
            setMensaje({ tipo: 'exito', texto });
            void recargar();
          }}
        />
      )}

      <section className="tarjeta">
        <div className="filtros">
          <div className="campo">
            <label htmlFor="f-estado">Estado</label>
            <select id="f-estado" value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value as EstadoCelda | '')}>
              <option value="">Todos</option>
              {(Object.keys(etiquetaEstadoCelda) as EstadoCelda[]).map((e) => (
                <option key={e} value={e}>
                  {etiquetaEstadoCelda[e]}
                </option>
              ))}
            </select>
          </div>
          <div className="campo">
            <label htmlFor="f-tipo">Tipo</label>
            <select id="f-tipo" value={filtroTipo} onChange={(e) => setFiltroTipo(e.target.value as TipoVehiculo | '')}>
              <option value="">Todos</option>
              <option value="CARRO">Carro</option>
              <option value="MOTO">Moto</option>
            </select>
          </div>
          <p className="filtros__resumen texto-suave">{datos ? `${filtradas.length} de ${datos.length} celdas` : ''}</p>
        </div>

        {error && <Alerta tipo="error">{error}</Alerta>}
        {cargando && !datos && <Cargando />}
        {datos && filtradas.length === 0 && <Vacio titulo="No hay celdas con esos filtros" />}
        {filtradas.length > 0 && (
          <div className="tabla-contenedor">
            <table className="tabla">
              <thead>
                <tr>
                  <th scope="col">Código</th>
                  <th scope="col">Zona</th>
                  <th scope="col">Tipo</th>
                  <th scope="col">Estado</th>
                  <th scope="col">Vehículo</th>
                  <th scope="col">
                    <span className="sr-only">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {filtradas.map((c) => (
                  <tr key={c.id}>
                    <td>
                      <strong>{c.codigo}</strong>
                    </td>
                    <td>{c.zona}</td>
                    <td>{etiquetaTipoVehiculo[c.tipoVehiculo]}</td>
                    <td>
                      <InsigniaCelda estado={c.estado} />
                    </td>
                    <td>{c.placaActual ? <span className="placa">{c.placaActual}</span> : '—'}</td>
                    <td className="tabla__acciones">
                      <button type="button" className="boton boton--pequeno boton--secundario" onClick={() => setEditando(c)}>
                        <Icono nombre="editar" tamano={16} /> Editar
                      </button>
                      {c.estado === 'DISPONIBLE' && (
                        <button
                          type="button"
                          className="boton boton--pequeno boton--peligro-suave"
                          onClick={() => setCambioEstado({ celda: c, nuevo: 'FUERA_SERVICIO' })}
                        >
                          Fuera de servicio
                        </button>
                      )}
                      {c.estado === 'FUERA_SERVICIO' && (
                        <button
                          type="button"
                          className="boton boton--pequeno boton--secundario"
                          onClick={() => setCambioEstado({ celda: c, nuevo: 'DISPONIBLE' })}
                        >
                          Habilitar
                        </button>
                      )}
                      {c.estado === 'OCUPADA' && (
                        <span className="texto-suave texto-pequeno" title="Registre primero la salida del vehículo">
                          Ocupada: no modificable
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <DialogoConfirmacion
        abierto={!!cambioEstado}
        titulo={cambioEstado?.nuevo === 'FUERA_SERVICIO' ? 'Poner fuera de servicio' : 'Habilitar celda'}
        textoConfirmar={cambioEstado?.nuevo === 'FUERA_SERVICIO' ? 'Sí, poner fuera de servicio' : 'Sí, habilitar'}
        peligro={cambioEstado?.nuevo === 'FUERA_SERVICIO'}
        procesando={procesando}
        onConfirmar={aplicarCambioEstado}
        onCancelar={() => setCambioEstado(null)}
      >
        <p>
          {cambioEstado?.nuevo === 'FUERA_SERVICIO'
            ? `La celda ${cambioEstado?.celda.codigo} dejará de ofrecerse para nuevas entradas.`
            : `La celda ${cambioEstado?.celda.codigo} volverá a estar disponible para entradas.`}
        </p>
      </DialogoConfirmacion>
    </>
  );
}

function FormularioCelda({
  celda,
  onCancelar,
  onGuardado,
}: {
  celda: Celda | null;
  onCancelar: () => void;
  onGuardado: (mensaje: string) => void;
}) {
  const ocupada = celda?.estado === 'OCUPADA';
  const [valores, setValores] = useState<CeldaSolicitud>(
    celda
      ? { codigo: celda.codigo, zona: celda.zona, tipoVehiculo: celda.tipoVehiculo, estado: celda.estado === 'FUERA_SERVICIO' ? 'FUERA_SERVICIO' : 'DISPONIBLE' }
      : VACIA,
  );
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  function validar(): boolean {
    const e: Record<string, string> = {};
    if (!valores.codigo.trim()) e.codigo = 'El código es obligatorio.';
    else if (!/^[A-Za-z0-9-]{1,20}$/.test(valores.codigo.trim())) e.codigo = 'Solo letras, números y guion (máx. 20).';
    if (!valores.zona.trim()) e.zona = 'La zona es obligatoria.';
    setErrores(e);
    return Object.keys(e).length === 0;
  }

  async function enviar(ev: FormEvent) {
    ev.preventDefault();
    setErrorGeneral(null);
    if (!validar()) return;
    setGuardando(true);
    // Si la celda está ocupada se envía el mismo estado que tiene (OCUPADA no es editable).
    const cuerpo = { ...valores, codigo: valores.codigo.trim().toUpperCase(), zona: valores.zona.trim() };
    try {
      if (celda) {
        const enviado: CeldaSolicitud = ocupada ? { ...cuerpo, estado: 'OCUPADA' } : cuerpo;
        const r = await api.actualizarCelda(celda.id, enviado);
        onGuardado(`Celda ${r.codigo} actualizada.`);
      } else {
        const r = await api.crearCelda(cuerpo);
        onGuardado(`Celda ${r.codigo} creada.`);
      }
    } catch (e) {
      setErrorGeneral(e instanceof Error ? e.message : 'No se pudo guardar la celda.');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <section className="tarjeta tarjeta--resaltada">
      <h2 className="tarjeta__titulo">{celda ? `Editar celda ${celda.codigo}` : 'Nueva celda'}</h2>
      {ocupada && <Alerta tipo="info">La celda está ocupada: puede cambiar código y zona, pero no su tipo ni su estado.</Alerta>}
      {errorGeneral && <Alerta tipo="error">{errorGeneral}</Alerta>}
      <form className="formulario-grilla" onSubmit={enviar} noValidate>
        <div className="campo">
          <label htmlFor="c-codigo">Código</label>
          <input
            id="c-codigo"
            value={valores.codigo}
            maxLength={20}
            onChange={(e) => setValores({ ...valores, codigo: e.target.value.toUpperCase() })}
            aria-invalid={!!errores.codigo}
            aria-describedby={errores.codigo ? 'c-codigo-error' : undefined}
          />
          {errores.codigo && <span id="c-codigo-error" className="campo__error">{errores.codigo}</span>}
        </div>
        <div className="campo">
          <label htmlFor="c-zona">Zona</label>
          <input
            id="c-zona"
            value={valores.zona}
            maxLength={40}
            onChange={(e) => setValores({ ...valores, zona: e.target.value })}
            aria-invalid={!!errores.zona}
            aria-describedby={errores.zona ? 'c-zona-error' : undefined}
          />
          {errores.zona && <span id="c-zona-error" className="campo__error">{errores.zona}</span>}
        </div>
        <div className="campo">
          <label htmlFor="c-tipo">Tipo de vehículo</label>
          <select
            id="c-tipo"
            value={valores.tipoVehiculo}
            disabled={ocupada}
            onChange={(e) => setValores({ ...valores, tipoVehiculo: e.target.value as TipoVehiculo })}
          >
            <option value="CARRO">Carro</option>
            <option value="MOTO">Moto</option>
          </select>
        </div>
        <div className="campo">
          <label htmlFor="c-estado">Estado</label>
          <select
            id="c-estado"
            value={ocupada ? 'OCUPADA' : valores.estado}
            disabled={ocupada}
            onChange={(e) => setValores({ ...valores, estado: e.target.value as EstadoEditable })}
          >
            {ocupada && <option value="OCUPADA">Ocupada</option>}
            <option value="DISPONIBLE">Disponible</option>
            <option value="FUERA_SERVICIO">Fuera de servicio</option>
          </select>
        </div>
        <div className="acciones-formulario formulario-grilla__completo">
          <button type="button" className="boton boton--secundario" onClick={onCancelar} disabled={guardando}>
            Cancelar
          </button>
          <button type="submit" className="boton boton--primario" disabled={guardando}>
            {guardando ? 'Guardando…' : 'Guardar'}
          </button>
        </div>
      </form>
    </section>
  );
}
