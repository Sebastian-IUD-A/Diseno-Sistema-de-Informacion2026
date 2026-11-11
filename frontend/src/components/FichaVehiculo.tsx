import type { Vehiculo } from '../api/types';
import { etiquetaTipoVehiculo, fecha, fechaHora } from '../utils/formato';
import { Insignia } from './Insignia';

/** Tarjeta con los datos del vehículo, su cliente y la vigencia de la mensualidad. */
export function FichaVehiculo({ vehiculo }: { vehiculo: Vehiculo }) {
  const v = vehiculo;
  const vigencia = v.autorizacionVigente
    ? v.diasParaVencer <= 5
      ? { tono: 'ambar' as const, texto: v.diasParaVencer === 0 ? 'Vence hoy' : `Vence en ${v.diasParaVencer} día(s)` }
      : { tono: 'verde' as const, texto: 'Mensualidad vigente' }
    : { tono: 'rojo' as const, texto: 'Mensualidad vencida' };

  return (
    <section className="tarjeta ficha" aria-label={`Vehículo ${v.placa}`}>
      <div className="ficha__cabecera">
        <span className="placa placa--grande">{v.placa}</span>
        <div className="ficha__insignias">
          <Insignia tono="azul">{etiquetaTipoVehiculo[v.tipo]}</Insignia>
          <Insignia tono={v.estado === 'ACTIVO' ? 'verde' : 'rojo'}>{v.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</Insignia>
          <Insignia tono={vigencia.tono}>{vigencia.texto}</Insignia>
          {v.dentro && <Insignia tono="ambar">{`Dentro · ${v.celdaActual ?? ''}`}</Insignia>}
        </div>
      </div>
      <dl className="ficha__datos">
        <div>
          <dt>Vehículo</dt>
          <dd>{[v.marca, v.modelo, v.color].filter(Boolean).join(' · ') || '—'}</dd>
        </div>
        <div>
          <dt>Autorizado hasta</dt>
          <dd>{fecha(v.autorizadoHasta)}</dd>
        </div>
        <div>
          <dt>Cliente</dt>
          <dd>{v.cliente?.nombreCompleto ?? '—'}</dd>
        </div>
        <div>
          <dt>Documento</dt>
          <dd>{v.cliente ? `${v.cliente.tipoDocumento} ${v.cliente.numeroDocumento}` : '—'}</dd>
        </div>
        <div>
          <dt>Teléfono</dt>
          <dd>{v.cliente?.telefono ?? '—'}</dd>
        </div>
        {v.dentro && (
          <div>
            <dt>Ingresó</dt>
            <dd>{fechaHora(v.fechaHoraEntradaActual)}</dd>
          </div>
        )}
      </dl>
    </section>
  );
}
