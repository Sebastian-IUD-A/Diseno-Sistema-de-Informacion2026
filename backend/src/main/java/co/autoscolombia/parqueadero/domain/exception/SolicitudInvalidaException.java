package co.autoscolombia.parqueadero.domain.exception;

/** Los datos enviados no son válidos (HTTP 400). Ej.: rango de fechas invertido. */
public class SolicitudInvalidaException extends NegocioException {

    public SolicitudInvalidaException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
