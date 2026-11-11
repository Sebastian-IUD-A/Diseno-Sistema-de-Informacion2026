package co.autoscolombia.parqueadero.security;

import co.autoscolombia.parqueadero.presentation.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Responde en JSON (mismo formato que el resto de errores) cuando:
 * <ul>
 *   <li>no hay token o es inválido/expirado → 401</li>
 *   <li>hay token pero el rol no tiene permiso → 403</li>
 * </ul>
 */
@Component
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ManejadorErroresSeguridad(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escribir(response, request, HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                "Debe iniciar sesión o su sesión expiró.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escribir(response, request, HttpStatus.FORBIDDEN, "SIN_PERMISOS",
                "Su rol no tiene permiso para realizar esta operación.");
    }

    private void escribir(HttpServletResponse response, HttpServletRequest request, HttpStatus estado,
                          String codigo, String mensaje) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.de(estado, codigo, mensaje, request.getRequestURI()));
    }
}
