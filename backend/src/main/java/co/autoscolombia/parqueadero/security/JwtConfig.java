package co.autoscolombia.parqueadero.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Configuración criptográfica:
 * <ul>
 *   <li>JWT firmado con HMAC-SHA256 (HS256). El secreto llega por la variable de entorno JWT_SECRET.</li>
 *   <li>BCrypt para las contraseñas.</li>
 * </ul>
 * Un JWT es un texto firmado que dice "soy el usuario X con rol Y, válido hasta tal hora".
 * El servidor no guarda sesiones: en cada petición verifica la firma del token.
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
    private static final int BYTES_MINIMOS = 32; // 256 bits, exigido por HS256

    @Bean
    public SecretKey jwtSecretKey(@Value("${app.jwt.secret:}") String secreto) {
        byte[] bytes;
        if (secreto == null || secreto.isBlank()) {
            // Sin secreto configurado generamos uno aleatorio: funciona, pero los tokens dejan de
            // ser válidos cada vez que se reinicia el backend. En producción SIEMPRE configure JWT_SECRET.
            bytes = new byte[BYTES_MINIMOS];
            new SecureRandom().nextBytes(bytes);
            log.warn("JWT_SECRET no está configurado: se generó un secreto temporal. "
                    + "Las sesiones se invalidarán al reiniciar el backend.");
        } else {
            bytes = secreto.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < BYTES_MINIMOS) {
                throw new IllegalStateException("JWT_SECRET debe tener al menos " + BYTES_MINIMOS + " caracteres.");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
