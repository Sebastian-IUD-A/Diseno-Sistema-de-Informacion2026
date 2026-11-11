package co.autoscolombia.parqueadero.security;

import co.autoscolombia.parqueadero.domain.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Emite el token JWT que el frontend envía luego en la cabecera "Authorization: Bearer ...". */
@Service
public class TokenService {

    public static final String CLAIM_USUARIO_ID = "uid";
    public static final String CLAIM_ROLES = "roles";

    public record TokenEmitido(String valor, Instant expiraEn) {
    }

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final long minutosValidez;

    public TokenService(JwtEncoder jwtEncoder, Clock clock,
                        @Value("${app.jwt.expiracion-minutos:480}") long minutosValidez) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.minutosValidez = minutosValidez;
    }

    public TokenEmitido emitir(Usuario usuario) {
        Instant ahora = clock.instant();
        Instant expira = ahora.plus(minutosValidez, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("autos-colombia")
                .issuedAt(ahora)
                .expiresAt(expira)
                .subject(usuario.getCorreo())
                .claim(CLAIM_USUARIO_ID, usuario.getId())
                .claim("nombre", usuario.getNombre())
                .claim(CLAIM_ROLES, List.of(usuario.getRol().name()))
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new TokenEmitido(token, expira);
    }
}
