import { Link } from 'react-router-dom';
import { api } from '../../api/endpoints';
import { useAuth } from '../../auth/AuthContext';
import { EncabezadoPagina } from '../../components/EncabezadoPagina';
import { Alerta, Cargando, Vacio } from '../../components/Estados';
import { Icono } from '../../components/Icono';
import { useConsulta } from '../../hooks/useConsulta';
import { duracion, etiquetaTipoVehiculo, fechaHora } from '../../utils/formato';

export function DashboardPage() {
  const { tieneRol } = useAuth();
  const resumen = useConsulta(() => api.dashboard(), []);
  const activos = useConsulta(() => api.activos(), []);

  const recargar = () => {
    void resumen.recargar();
    void activos.recargar();
  };

  return (
    <>
      <EncabezadoPagina
        titulo="Resumen del parqueadero"
        descripcion={resumen.datos ? `Actualizado: ${fechaHora(resumen.datos.generadoEn)}` : 'Estado actual de la operación'}
        acciones={
          <button type="button" className="boton boton--secundario" onClick={recargar}>
            <Icono nombre="recargar" tamano={18} /> Actualizar
          </button>
        }
      />

      {resumen.error && <Alerta tipo="error">{resumen.error}</Alerta>}
      {resumen.cargando && !resumen.datos && <Cargando texto="Cargando resumen…" />}

      {resumen.datos && (
        <>
          <section className="cuadricula-indicadores" aria-label="Indicadores">
            <Indicador etiqueta="Vehículos dentro" valor={resumen.datos.vehiculosDentro} tono="azul" />
            <Indicador etiqueta="Celdas disponibles" valor={resumen.datos.celdasDisponibles} tono="verde" />
            <Indicador etiqueta="Celdas ocupadas" valor={resumen.datos.celdasOcupadas} tono="ambar" />
            <Indicador etiqueta="Fuera de servicio" valor={resumen.datos.celdasFueraServicio} tono="gris" />
          </section>

          <div className="dos-columnas">
            <section className="tarjeta">
              <h2 className="tarjeta__titulo">Ocupación por tipo</h2>
              {resumen.datos.porTipo.map((t) => {
                const util = t.total - t.fueraServicio;
                const porcentaje = util > 0 ? Math.round((t.ocupadas / util) * 100) : 0;
                return (
                  <div key={t.tipo} className="barra-ocupacion">
                    <div className="barra-ocupacion__fila">
                      <span>
                        <strong>{etiquetaTipoVehiculo[t.tipo]}</strong> · {t.ocupadas} de {util} en uso
                      </span>
                      <span>{porcentaje}%</span>
                    </div>
                    <div
                      className="barra-ocupacion__pista"
                      role="progressbar"
                      aria-label={`Ocupación ${etiquetaTipoVehiculo[t.tipo]}`}
                      aria-valuenow={porcentaje}
                      aria-valuemin={0}
                      aria-valuemax={100}
                    >
                      <div className="barra-ocupacion__relleno" style={{ width: `${porcentaje}%` }} />
                    </div>
                    <span className="texto-suave texto-pequeno">
                      {t.disponibles} disponibles · {t.fueraServicio} fuera de servicio
                    </span>
                  </div>
                );
              })}
              {tieneRol('OPERADOR') && (
                <div className="acciones-rapidas">
                  <Link to="/entrada" className="boton boton--primario">
                    <Icono nombre="entrada" tamano={18} /> Registrar entrada
                  </Link>
                  <Link to="/salida" className="boton boton--secundario">
                    <Icono nombre="salida" tamano={18} /> Registrar salida
                  </Link>
                </div>
              )}
            </section>

            <section className="tarjeta">
              <div className="tarjeta__cabecera">
                <h2 className="tarjeta__titulo">Últimos ingresos</h2>
                <Link to="/ocupacion" className="enlace">
                  Ver todo
                </Link>
              </div>
              {activos.cargando && !activos.datos && <Cargando />}
              {activos.datos && activos.datos.length === 0 && <Vacio titulo="No hay vehículos dentro" />}
              {activos.datos && activos.datos.length > 0 && (
                <ul className="lista-simple">
                  {activos.datos.slice(0, 6).map((m) => (
                    <li key={m.id}>
                      <span className="placa">{m.placa}</span>
                      <span className="lista-simple__detalle">
                        Celda {m.celdaCodigo} · {fechaHora(m.fechaHoraEntrada)}
                      </span>
                      <span className="texto-suave texto-pequeno">{duracion(m.minutosPermanencia)}</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        </>
      )}
    </>
  );
}

function Indicador({ etiqueta, valor, tono }: { etiqueta: string; valor: number; tono: string }) {
  return (
    <div className={`indicador indicador--${tono}`}>
      <span className="indicador__valor">{valor}</span>
      <span className="indicador__etiqueta">{etiqueta}</span>
    </div>
  );
}
