package co.autoscolombia.parqueadero.domain.exception;

/** El usuario autenticado no puede realizar la operación (HTTP 403). Ej.: usuario desactivado. */
public class AccesoDenegadoException extends NegocioException {

    public AccesoDenegadoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
