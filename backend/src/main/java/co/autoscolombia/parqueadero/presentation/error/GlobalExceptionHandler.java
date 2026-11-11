package co.autoscolombia.parqueadero.presentation.error;

import co.autoscolombia.parqueadero.domain.exception.AccesoDenegadoException;
import co.autoscolombia.parqueadero.domain.exception.ConflictoException;
import co.autoscolombia.parqueadero.domain.exception.CredencialesInvalidasException;
import co.autoscolombia.parqueadero.domain.exception.NegocioException;
import co.autoscolombia.parqueadero.domain.exception.RecursoNoEncontradoException;
import co.autoscolombia.parqueadero.domain.exception.ReglaNegocioException;
import co.autoscolombia.parqueadero.domain.exception.SolicitudInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo centralizado de excepciones: convierte cualquier error en una respuesta JSON
 * {@link ErrorResponse} con el código HTTP adecuado. Así los controladores quedan limpios
 * (no tienen try/catch) y todos los errores se ven iguales para el frontend.
 *
 * <pre>
 *  400 Bad Request          datos inválidos (validación, JSON mal formado, fechas)
 *  401 Unauthorized         credenciales inválidas / sin token
 *  403 Forbidden            rol sin permiso / usuario inactivo
 *  404 Not Found            placa, celda o movimiento inexistente
 *  409 Conflict             vehículo ya dentro, celda ocupada, carrera concurrente
 *  422 Unprocessable Entity regla de negocio: autorización vencida, celda fuera de servicio...
 *  500 Internal Error       error inesperado (se registra en el log)
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorResponse> negocio(NegocioException ex, HttpServletRequest request) {
        HttpStatus estado = estadoPara(ex);
        return ResponseEntity.status(estado)
                .body(ErrorResponse.de(estado, ex.getCodigo(), ex.getMessage(), request.getRequestURI()));
    }

    private static HttpStatus estadoPara(NegocioException ex) {
        if (ex instanceof RecursoNoEncontradoException) return HttpStatus.NOT_FOUND;
        if (ex instanceof ConflictoException) return HttpStatus.CONFLICT;
        if (ex instanceof ReglaNegocioException) return HttpStatus.UNPROCESSABLE_ENTITY;
        if (ex instanceof SolicitudInvalidaException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof AccesoDenegadoException) return HttpStatus.FORBIDDEN;
        if (ex instanceof CredencialesInvalidasException) return HttpStatus.UNAUTHORIZED;
        return HttpStatus.BAD_REQUEST;
    }

    /** Errores de Bean Validation (@NotBlank, @Size...) en el cuerpo de la petición. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));
        String mensaje = errores.isEmpty() ? "Datos inválidos." : errores.values().iterator().next();
        return ResponseEntity.badRequest().body(ErrorResponse.validacion(mensaje, request.getRequestURI(), errores));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(HttpStatus.BAD_REQUEST, "SOLICITUD_MAL_FORMADA",
                "El cuerpo de la solicitud no es válido. Revise los valores enviados.", request.getRequestURI()));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> parametroInvalido(Exception ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO",
                "Uno de los parámetros no es válido o falta.", request.getRequestURI()));
    }

    /**
     * La base de datos rechazó la operación por una restricción (p. ej. el índice único de
     * "movimiento abierto por celda"). Ocurre si dos solicitudes concurrentes llegan a competir.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Restricción de base de datos violada en {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.de(HttpStatus.CONFLICT, "CONFLICTO_DATOS",
                "La operación entra en conflicto con otra registrada al mismo tiempo. Actualice la pantalla e intente de nuevo.",
                request.getRequestURI()));
    }

    /** Bloqueos, interbloqueos (deadlocks) o versiones optimistas desactualizadas. */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ErrorResponse> concurrencia(ConcurrencyFailureException ex, HttpServletRequest request) {
        log.warn("Conflicto de concurrencia en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.de(HttpStatus.CONFLICT, "CONFLICTO_CONCURRENCIA",
                "Otro usuario modificó estos datos al mismo tiempo. Actualice la pantalla e intente de nuevo.",
                request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.de(HttpStatus.FORBIDDEN, "SIN_PERMISOS",
                "Su rol no tiene permiso para realizar esta operación.", request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.de(HttpStatus.NOT_FOUND, "RUTA_NO_ENCONTRADA",
                "La ruta solicitada no existe.", request.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ErrorResponse.de(HttpStatus.METHOD_NOT_ALLOWED,
                "METODO_NO_PERMITIDO", "Método HTTP no permitido para esta ruta.", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest request) {
        // Muchas excepciones de Spring MVC (415, 406, ...) ya traen su código HTTP: lo respetamos.
        if (ex instanceof org.springframework.web.ErrorResponse conEstado) {
            HttpStatus estado = HttpStatus.resolve(conEstado.getStatusCode().value());
            if (estado != null && estado.is4xxClientError()) {
                return ResponseEntity.status(estado).body(ErrorResponse.de(estado, "SOLICITUD_INVALIDA",
                        "La solicitud no es válida.", request.getRequestURI()));
            }
        }
        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.de(HttpStatus.INTERNAL_SERVER_ERROR,
                "ERROR_INTERNO", "Ocurrió un error inesperado. Intente de nuevo.", request.getRequestURI()));
    }
}
