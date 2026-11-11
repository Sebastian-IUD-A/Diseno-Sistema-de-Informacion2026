package co.autoscolombia.parqueadero.security;

import co.autoscolombia.parqueadero.domain.exception.AccesoDenegadoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Obtiene el usuario autenticado a partir del token ya verificado por Spring Security.
 * <p>
 * Regla de la API: el id del operador NUNCA viene en el cuerpo de la petición (el frontend
 * podría mentir); siempre sale de aquí, del token firmado por el servidor (RN-06).
 */
@Component
public class UsuarioActual {

    public Long id() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken jwtAuth) {
            Object uid = jwtAuth.getToken().getClaims().get(TokenService.CLAIM_USUARIO_ID);
            if (uid instanceof Number numero) {
                return numero.longValue();
            }
            if (uid instanceof String texto) {
                return Long.valueOf(texto);
            }
        }
        throw new AccesoDenegadoException("SESION_INVALIDA", "La sesión no es válida. Inicie sesión nuevamente.");
    }
}
