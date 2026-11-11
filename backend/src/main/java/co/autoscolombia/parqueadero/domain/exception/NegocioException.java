package co.autoscolombia.parqueadero.domain.exception;

/**
 * Excepción base del dominio. Lleva un {@code codigo} estable (p. ej. "AUTORIZACION_VENCIDA")
 * que el frontend puede usar sin depender del texto del mensaje.
 * <p>
 * Las subclases indican el tipo de problema; el manejador global
 * ({@code GlobalExceptionHandler}) las traduce a códigos HTTP.
 */
public abstract class NegocioException extends RuntimeException {

    private final String codigo;

    protected NegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
