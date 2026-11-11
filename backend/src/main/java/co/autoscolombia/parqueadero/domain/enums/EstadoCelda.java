package co.autoscolombia.parqueadero.domain.enums;

/**
 * Estado de una celda.
 * OCUPADA solo la asigna el sistema al registrar una entrada; el administrador no puede ponerla a mano.
 */
public enum EstadoCelda {
    DISPONIBLE,
    OCUPADA,
    FUERA_SERVICIO
}
