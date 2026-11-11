package co.autoscolombia.parqueadero.domain.exception;

/** El recurso pedido no existe (HTTP 404). Ej.: placa no registrada. */
public class RecursoNoEncontradoException extends NegocioException {

    public RecursoNoEncontradoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
