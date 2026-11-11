/**
 * Tipos TypeScript que reflejan los DTOs que devuelve el backend.
 * Si cambia un DTO en Java, se actualiza aquí y el compilador señala dónde ajustar la UI.
 */

export type Rol = 'ADMINISTRADOR' | 'OPERADOR';
export type TipoVehiculo = 'CARRO' | 'MOTO';
export type EstadoCelda = 'DISPONIBLE' | 'OCUPADA' | 'FUERA_SERVICIO';
export type EstadoRegistro = 'ACTIVO' | 'INACTIVO';
export type TipoNovedad = 'DANO_VEHICULO' | 'OBJETO_OLVIDADO' | 'INCIDENTE_SEGURIDAD' | 'OBSERVACION' | 'OTRO';

export interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  rol: Rol;
}

export interface Sesion {
  token: string;
  tipo: string;
  expiraEn: string;
  usuario: Usuario;
}

export interface Cliente {
  id: number;
  tipoDocumento: string;
  numeroDocumento: string;
  nombreCompleto: string;
  telefono: string | null;
  correo: string | null;
  estado: EstadoRegistro;
}

export interface Motivo {
  codigo: string;
  mensaje: string;
}

export interface Vehiculo {
  id: number;
  placa: string;
  tipo: TipoVehiculo;
  marca: string | null;
  modelo: string | null;
  color: string | null;
  estado: EstadoRegistro;
  autorizadoHasta: string;
  autorizacionVigente: boolean;
  diasParaVencer: number;
  cliente: Cliente | null;
  dentro: boolean;
  movimientoAbiertoId: number | null;
  celdaActual: string | null;
  fechaHoraEntradaActual: string | null;
  puedeIngresar: boolean;
  motivoNoIngreso: Motivo | null;
}

export interface Celda {
  id: number;
  codigo: string;
  zona: string;
  tipoVehiculo: TipoVehiculo;
  estado: EstadoCelda;
  placaActual: string | null;
  movimientoId: number | null;
}

export interface CeldaSolicitud {
  codigo: string;
  zona: string;
  tipoVehiculo: TipoVehiculo;
  /** OCUPADA solo se acepta si la celda ya lo está (el backend no permite asignarla a mano). */
  estado: EstadoCelda;
}

export interface Movimiento {
  id: number;
  placa: string;
  tipoVehiculo: TipoVehiculo;
  descripcionVehiculo: string;
  clienteNombre: string | null;
  celdaId: number;
  celdaCodigo: string;
  zona: string;
  fechaHoraEntrada: string;
  fechaHoraSalida: string | null;
  usuarioEntrada: string;
  usuarioSalida: string | null;
  abierto: boolean;
  minutosPermanencia: number;
  cantidadNovedades: number;
}

export interface Novedad {
  id: number;
  movimientoId: number;
  tipo: TipoNovedad;
  descripcion: string;
  fechaHora: string;
  usuario: string;
}

export interface ResumenTipo {
  tipo: TipoVehiculo;
  disponibles: number;
  ocupadas: number;
  fueraServicio: number;
  total: number;
}

export interface Dashboard {
  vehiculosDentro: number;
  celdasDisponibles: number;
  celdasOcupadas: number;
  celdasFueraServicio: number;
  totalCeldas: number;
  porTipo: ResumenTipo[];
  generadoEn: string;
}

/** Formato de error común del backend (ErrorResponse.java). */
export interface ErrorApi {
  status: number;
  codigo: string;
  mensaje: string;
  ruta?: string;
  errores?: Record<string, string>;
}
