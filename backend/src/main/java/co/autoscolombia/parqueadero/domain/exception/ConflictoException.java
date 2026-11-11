package co.autoscolombia.parqueadero.domain.exception;

/**
 * La operación choca con el estado actual de los datos (HTTP 409).
 * Ej.: el vehículo ya está dentro, la celda acaba de ser ocupada por otra solicitud.
 */
public class ConflictoException extends NegocioException {

    public ConflictoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
