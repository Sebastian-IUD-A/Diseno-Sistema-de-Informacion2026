package co.autoscolombia.parqueadero.presentation.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Formato único de error de la API. Ejemplo:
 * <pre>
 * { "timestamp": "...", "status": 409, "codigo": "CELDA_OCUPADA",
 *   "mensaje": "La celda A-01 ya está ocupada.", "ruta": "/api/movements/entries" }
 * </pre>
 * {@code errores} solo aparece en errores de validación (campo → mensaje).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(Instant timestamp, int status, String codigo, String mensaje, String ruta,
                            Map<String, String> errores) {

    public static ErrorResponse de(HttpStatus estado, String codigo, String mensaje, String ruta) {
        return new ErrorResponse(Instant.now(), estado.value(), codigo, mensaje, ruta, null);
    }

    public static ErrorResponse validacion(String mensaje, String ruta, Map<String, String> errores) {
        return new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), "VALIDACION", mensaje, ruta, errores);
    }
}
