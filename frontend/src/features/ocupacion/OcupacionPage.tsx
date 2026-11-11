import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/endpoints';
import { useAuth } from '../../auth/AuthContext';
import { EncabezadoPagina } from '../../components/EncabezadoPagina';
import { Alerta, Cargando, Vacio } from '../../components/Estados';
import { Icono } from '../../components/Icono';
import { useConsulta } from '../../hooks/useConsulta';
import { duracion, etiquetaTipoVehiculo, fechaHora } from '../../utils/formato';

/** Vehículos que están dentro ahora mismo, con filtro por placa. */
export function OcupacionPage() {
  const { tieneRol } = useAuth();
  const [texto, setTexto] = useState('');
  const [filtro, setFiltro] = useState('');

  // Espera 300 ms después de que el usuario deja de escribir antes de consultar.
  useEffect(() => {
    const t = setTimeout(() => setFiltro(texto.trim().toUpperCase()), 300);
    return () => clearTimeout(t);
  }, [texto]);

  const { datos, cargando, error, recargar } = useConsulta(() => api.activos(filtro || undefined), [filtro]);
  const esOperador = tieneRol('OPERADOR');

  return (
    <>
      <EncabezadoPagina
        titulo="Ocupación actual"
        descripcion="Vehículos que se encuentran dentro del parqueadero."
        acciones={
          <button type="button" className="boton boton--secundario" onClick={() => void recargar()}>
            <Icono nombre="recargar" tamano={18} /> Actualizar
          </button>
        }
      />

      <section className="tarjeta">
        <div className="filtros">
          <div className="campo">
            <label htmlFor="filtro-placa">Filtrar por placa</label>
            <input
              id="filtro-placa"
              value={texto}
              onChange={(e) => setTexto(e.target.value.toUpperCase())}
              placeholder="Ej.: ABC"
              maxLength={10}
              autoComplete="off"
            />
          </div>
          <p className="filtros__resumen texto-suave" aria-live="polite">
            {datos ? `${datos.length} vehículo(s) dentro${filtro ? ` que coinciden con "${filtro}"` : ''}` : ''}
          </p>
        </div>

        {error && <Alerta tipo="error">{error}</Alerta>}
        {cargando && !datos && <Cargando />}
        {datos && datos.length === 0 && (
          <Vacio titulo={filtro ? 'Sin coincidencias' : 'El parqueadero está vacío'} texto={filtro ? 'Ningún vehículo dentro coincide con esa placa.' : undefined} />
        )}

        {datos && datos.length > 0 && (
          <div className="tabla-contenedor">
            <table className="tabla">
              <thead>
                <tr>
                  <th scope="col">Placa</th>
                  <th scope="col">Tipo</th>
                  <th scope="col">Cliente</th>
                  <th scope="col">Celda</th>
                  <th scope="col">Entrada</th>
                  <th scope="col">Tiempo dentro</th>
                  <th scope="col">Novedades</th>
                  <th scope="col">
                    <span className="sr-only">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {datos.map((m) => (
                  <tr key={m.id}>
                    <td>
                      <span className="placa">{m.placa}</span>
                    </td>
                    <td>{etiquetaTipoVehiculo[m.tipoVehiculo]}</td>
                    <td>{m.clienteNombre ?? '—'}</td>
                    <td>
                      {m.celdaCodigo} <span className="texto-suave texto-pequeno">{m.zona}</span>
                    </td>
                    <td>{fechaHora(m.fechaHoraEntrada)}</td>
                    <td>{duracion(m.minutosPermanencia)}</td>
                    <td>{m.cantidadNovedades}</td>
                    <td className="tabla__acciones">
                      <Link className="boton boton--pequeno boton--secundario" to={`/novedades?movimiento=${m.id}`}>
                        Novedades
                      </Link>
                      {esOperador && (
                        <Link className="boton boton--pequeno boton--primario" to={`/salida?placa=${m.placa}`}>
                          Salida
                        </Link>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
