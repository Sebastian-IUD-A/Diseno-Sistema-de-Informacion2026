package co.autoscolombia.parqueadero.domain.exception;

/**
 * Login fallido (HTTP 401). El mensaje es deliberadamente genérico: no revela si el correo
 * existe o si lo incorrecto fue la contraseña, para no ayudar a un atacante.
 */
public class CredencialesInvalidasException extends NegocioException {

    public CredencialesInvalidasException() {
        super("CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos.");
    }
}
