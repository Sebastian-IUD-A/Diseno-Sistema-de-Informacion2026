package co.autoscolombia.parqueadero.domain.exception;

/**
 * La solicitud está bien formada pero una regla del negocio la impide (HTTP 422).
 * Ej.: autorización vencida, celda fuera de servicio, tipo de celda incompatible.
 */
public class ReglaNegocioException extends NegocioException {

    public ReglaNegocioException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
